package com.billfolder.android.data.auth

import com.billfolder.android.data.dto.AuthResponse
import com.billfolder.android.data.dto.RefreshTokenRequest
import com.billfolder.android.di.RawOkHttpClient
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Authenticator do OkHttp: chamado quando o servidor responde 401.
 * Tenta usar o refresh_token pra renovar os tokens. Se conseguir, repete
 * a request original com o novo access_token. Se falhar, devolve null
 * (que faz o OkHttp propagar o 401 pro chamador, e a UI vai pra Login).
 *
 * Usa OkHttpClient *cru* (sem o Authenticator dele mesmo) pra chamar
 * /auth/refresh — se entrasse no Authenticator de novo, viraria um loop.
 *
 * Mutex serializa refresh: se 5 chamadas levarem 401 ao mesmo tempo,
 * só uma chama refresh; as outras esperam e usam o token novo.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStorage: TokenStorage,
    /**
     * Lazy provider do OkHttpClient cru pra evitar ciclo do Hilt
     * (NetworkModule depende daqui pra construir o client autenticado).
     */
    @RawOkHttpClient
    private val rawHttpClientProvider: Provider<OkHttpClient>,
    private val baseUrlProvider: BaseUrlProvider,
    private val json: Json,
) : okhttp3.Authenticator {

    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Se a request original já não tinha token, não tem o que renovar.
        val originalAuth = response.request.header("Authorization") ?: return null
        val staleToken = originalAuth.removePrefix("Bearer ").trim()

        return runBlocking {
            mutex.withLock {
                val current = tokenStorage.getAccessToken()
                // Outra coroutine pode ter renovado enquanto a gente esperava.
                if (!current.isNullOrBlank() && current != staleToken) {
                    return@withLock response.request.newBuilder()
                        .header("Authorization", "Bearer $current")
                        .build()
                }

                val refreshToken = tokenStorage.getRefreshToken() ?: return@withLock null
                val newTokens = refresh(refreshToken) ?: run {
                    tokenStorage.clear()
                    return@withLock null
                }

                tokenStorage.saveTokens(newTokens.accessToken, newTokens.refreshToken)

                response.request.newBuilder()
                    .header("Authorization", "Bearer ${newTokens.accessToken}")
                    .build()
            }
        }
    }

    /** Faz POST /auth/refresh fora do client autenticado. */
    private fun refresh(refreshToken: String): AuthResponse? {
        val body = json
            .encodeToString(
                RefreshTokenRequest.serializer(),
                RefreshTokenRequest(refreshToken),
            )
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("${baseUrlProvider.baseUrl}auth/refresh")
            .post(body)
            .build()

        return try {
            rawHttpClientProvider.get().newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    null
                } else {
                    val raw = resp.body?.string() ?: return null
                    json.decodeFromString(AuthResponse.serializer(), raw)
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Indireção pro BuildConfig.API_BASE_URL — facilita injetar valor
 * diferente em testes. NetworkModule provê a implementação concreta.
 */
interface BaseUrlProvider {
    val baseUrl: String
}
