package com.billfolder.android.data.repository

import com.billfolder.android.data.api.BillFolderApi
import com.billfolder.android.data.auth.TokenStorage
import com.billfolder.android.data.dto.ApiErrorBody
import com.billfolder.android.data.dto.AuthResponse
import com.billfolder.android.data.dto.ForgotPasswordRequest
import com.billfolder.android.data.dto.LoginRequest
import com.billfolder.android.data.dto.LogoutRequest
import com.billfolder.android.data.dto.ResetPasswordRequest
import com.billfolder.android.data.dto.SignupRequest
import com.billfolder.android.data.dto.UserDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resultado simples pra propagar sucesso/falha pro ViewModel sem expor
 * exceções HTTP. Se precisar de mais granularidade depois (ex: distinguir
 * erro de rede de 4xx), evolui pra um sealed.
 */
sealed interface AuthResult {
    data class Success(val user: UserDto) : AuthResult
    data class Failure(val code: String, val message: String) : AuthResult
}

/**
 * Resultado das operações auth sem retorno de token (forgot/reset).
 * Success é sem payload — a UI reage baseado só no discriminador.
 */
sealed interface AuthActionResult {
    data object Success : AuthActionResult
    data class Failure(val code: String, val message: String) : AuthActionResult
}

@Singleton
class AuthRepository @Inject constructor(
    private val api: BillFolderApi,
    private val tokenStorage: TokenStorage,
    private val json: Json,
) {

    /** Stream de "está logado?" — true se há access_token no DataStore. */
    val isLoggedIn: Flow<Boolean> =
        tokenStorage.accessTokenFlow.map { !it.isNullOrBlank() }

    suspend fun signup(email: String, password: String, displayName: String): AuthResult =
        runAuth { api.signup(SignupRequest(email, password, displayName)) }

    suspend fun login(email: String, password: String): AuthResult =
        runAuth { api.login(LoginRequest(email, password)) }

    suspend fun logout() {
        val refresh = tokenStorage.getRefreshToken()
        if (!refresh.isNullOrBlank()) {
            // Best-effort: se der erro, mesmo assim limpa local.
            runCatching { api.logout(LogoutRequest(refresh)) }
        }
        tokenStorage.clear()
    }

    /**
     * Dispara envio do código de reset por email. Backend sempre 200
     * mesmo se email não existir — mantemos essa semântica pra cima da
     * cadeia (VM sempre trata como sucesso pra não vazar existência do
     * email). Só retorna Failure em erro de rede ou HTTP inesperado.
     *
     * O devCode do body é ignorado aqui — só é usado em dev sem provider
     * de email configurado, e mesmo assim não expomos ao user via UI.
     */
    suspend fun forgotPassword(email: String): AuthActionResult {
        val response = try {
            api.forgotPassword(ForgotPasswordRequest(email = email.trim()))
        } catch (e: Exception) {
            return AuthActionResult.Failure("network_error", e.message ?: "Erro de rede.")
        }

        return if (response.isSuccessful) {
            AuthActionResult.Success
        } else {
            val raw = response.errorBody()?.string().orEmpty()
            val parsed = runCatching { json.decodeFromString<ApiErrorBody>(raw) }.getOrNull()
            AuthActionResult.Failure(
                code    = parsed?.error ?: "http_${response.code()}",
                message = parsed?.message ?: "Algo deu errado. (HTTP ${response.code()})",
            )
        }
    }

    /**
     * Conclui reset. Em sucesso (204), backend já revogou os refresh
     * tokens ativos — se o user tinha sessão aberta em outro device,
     * cai no próximo refresh. Local tokens (se houver) também limpamos
     * pra forçar re-login com a senha nova.
     */
    suspend fun resetPassword(
        email: String,
        code: String,
        newPassword: String,
    ): AuthActionResult {
        val response = try {
            api.resetPassword(
                ResetPasswordRequest(
                    email = email.trim(),
                    code = code.trim(),
                    newPassword = newPassword,
                ),
            )
        } catch (e: Exception) {
            return AuthActionResult.Failure("network_error", e.message ?: "Erro de rede.")
        }

        return if (response.isSuccessful) {
            // Sanity: limpa tokens locais mesmo que já estivessem inválidos.
            tokenStorage.clear()
            AuthActionResult.Success
        } else {
            val raw = response.errorBody()?.string().orEmpty()
            val parsed = runCatching { json.decodeFromString<ApiErrorBody>(raw) }.getOrNull()
            AuthActionResult.Failure(
                code    = parsed?.error ?: "http_${response.code()}",
                message = parsed?.message ?: "Algo deu errado. (HTTP ${response.code()})",
            )
        }
    }

    private suspend fun runAuth(call: suspend () -> Response<AuthResponse>): AuthResult {
        val response = try {
            call()
        } catch (e: Exception) {
            return AuthResult.Failure("network_error", e.message ?: "Erro de rede.")
        }

        return if (response.isSuccessful) {
            val body = response.body()
                ?: return AuthResult.Failure("empty_body", "Resposta vazia do servidor.")
            tokenStorage.saveTokens(body.accessToken, body.refreshToken)
            AuthResult.Success(body.user)
        } else {
            val raw = response.errorBody()?.string().orEmpty()
            val parsed = runCatching { json.decodeFromString<ApiErrorBody>(raw) }.getOrNull()
            AuthResult.Failure(
                code    = parsed?.error ?: "http_${response.code()}",
                message = parsed?.message ?: defaultMessageFor(response.code()),
            )
        }
    }

    private fun defaultMessageFor(httpCode: Int): String = when (httpCode) {
        401 -> "E-mail ou senha incorretos."
        409 -> "Esse e-mail já está cadastrado."
        in 500..599 -> "Servidor indisponível, tenta de novo em instantes."
        else -> "Algo deu errado. (HTTP $httpCode)"
    }
}
