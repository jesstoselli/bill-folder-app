package com.billfolder.android.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.repository.AuthActionResult
import com.billfolder.android.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * VM da tela "esqueci minha senha".
 *
 * Fluxo:
 *  Idle → Submitting → Sent (sucesso, navega pra ResetPassword) ou Error.
 *
 * Semântica anti-enumeration: qualquer 200 do backend (email existe ou
 * não) resulta em Sent. Só surface como erro se der problema de rede
 * ou HTTP inesperado (5xx). O caller (screen) navega pra
 * ResetPasswordScreen passando o email como argumento, independente de
 * ter chegado email de verdade — se o email não existir, o reset vai
 * falhar com "código inválido", mesma mensagem genérica.
 */
sealed interface ForgotPasswordUiState {
    data object Idle : ForgotPasswordUiState
    data object Submitting : ForgotPasswordUiState

    /** Backend aceitou o request. Screen deve navegar pra ResetPasswordScreen. */
    data object Sent : ForgotPasswordUiState

    data class Error(val message: String) : ForgotPasswordUiState
}

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ForgotPasswordUiState>(ForgotPasswordUiState.Idle)
    val state: StateFlow<ForgotPasswordUiState> = _state.asStateFlow()

    fun submit(email: String) {
        if (!validate(email)) return
        _state.value = ForgotPasswordUiState.Submitting
        viewModelScope.launch {
            _state.value = when (val result = authRepository.forgotPassword(email)) {
                AuthActionResult.Success        -> ForgotPasswordUiState.Sent
                is AuthActionResult.Failure     -> ForgotPasswordUiState.Error(result.message)
            }
        }
    }

    /** Reseta erro quando o user começa a digitar de novo. */
    fun consumeError() {
        _state.update {
            if (it is ForgotPasswordUiState.Error) ForgotPasswordUiState.Idle else it
        }
    }

    private fun validate(email: String): Boolean {
        val errorMsg = when {
            email.isBlank()                    -> "Informe seu e-mail."
            !EMAIL_REGEX.matches(email.trim()) -> "E-mail em formato inválido."
            else                                -> null
        }
        return if (errorMsg != null) {
            _state.value = ForgotPasswordUiState.Error(errorMsg)
            false
        } else {
            true
        }
    }

    private companion object {
        // Regex pragmático — mesmo do AuthViewModel. Server valida o resto.
        val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
