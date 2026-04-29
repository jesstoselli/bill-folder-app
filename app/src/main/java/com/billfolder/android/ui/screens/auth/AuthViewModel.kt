package com.billfolder.android.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.UserDto
import com.billfolder.android.data.repository.AuthRepository
import com.billfolder.android.data.repository.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado das telas de auth (Login + Signup compartilham o ViewModel).
 *
 * Idle      — formulário pronto pra digitar.
 * Submitting — chamada em voo; UI desabilita botão e mostra spinner.
 * Success   — credenciais salvas; UI deve navegar pra Home.
 * Error     — mostra mensagem inline; usuário pode corrigir e tentar de novo.
 */
sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Submitting : AuthUiState
    data class Success(val user: UserDto) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun login(email: String, password: String) {
        if (!validate(email = email, password = password)) return
        _state.value = AuthUiState.Submitting
        viewModelScope.launch {
            handle(authRepository.login(email.trim(), password))
        }
    }

    fun signup(email: String, password: String, displayName: String) {
        if (!validate(email = email, password = password, name = displayName)) return
        _state.value = AuthUiState.Submitting
        viewModelScope.launch {
            handle(authRepository.signup(email.trim(), password, displayName.trim()))
        }
    }

    /** Reseta erro quando o usuário começa a digitar de novo. */
    fun consumeError() {
        if (_state.value is AuthUiState.Error) {
            _state.value = AuthUiState.Idle
        }
    }

    private fun handle(result: AuthResult) {
        _state.value = when (result) {
            is AuthResult.Success -> AuthUiState.Success(result.user)
            is AuthResult.Failure -> AuthUiState.Error(result.message)
        }
    }

    /**
     * Validação client-side mínima — server-side é a fonte de verdade
     * (FluentValidation no .NET roda regras finais). Aqui só evitamos
     * round-trip óbvio: campos vazios, formato de email, senha curta.
     */
    private fun validate(email: String, password: String, name: String? = null): Boolean {
        val errorMsg = when {
            email.isBlank() -> "Informe seu e-mail."
            !EMAIL_REGEX.matches(email.trim()) -> "E-mail em formato inválido."
            password.length < 8 -> "Senha precisa ter ao menos 8 caracteres."
            name != null && name.isBlank() -> "Informe seu nome."
            else -> null
        }
        return if (errorMsg != null) {
            _state.value = AuthUiState.Error(errorMsg)
            false
        } else {
            true
        }
    }

    private companion object {
        // Regex pragmático — não tenta cobrir RFC 5322. Server valida o resto.
        val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
