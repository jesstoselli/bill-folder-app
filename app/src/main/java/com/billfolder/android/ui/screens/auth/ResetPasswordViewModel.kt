package com.billfolder.android.ui.screens.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.repository.AuthActionResult
import com.billfolder.android.data.repository.AuthRepository
import com.billfolder.android.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * VM da tela "redefinir senha". Recebe o email da ForgotPasswordScreen
 * via SavedStateHandle (arg de navegação) e não permite editar — o user
 * chegou aqui do fluxo em cadeia.
 *
 * Fluxo:
 *  Idle → Submitting → Success (screen volta pra Login) ou Error.
 *
 * Validações locais (feedback rápido, backend valida de novo):
 *  - código: exatamente 6 dígitos
 *  - senha: ≥8 caracteres (mesma regra do signup)
 *  - confirmação: precisa bater com senha
 */
sealed interface ResetPasswordUiState {
    data object Idle : ResetPasswordUiState
    data object Submitting : ResetPasswordUiState
    data object Success : ResetPasswordUiState
    data class Error(val message: String) : ResetPasswordUiState
}

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
) : ViewModel() {

    /** Email vindo do NavHost (arg da URL). Se não vier, cai pra string vazia
     *  — screen mostra o campo readonly e o submit vai falhar em validação. */
    val email: String = savedStateHandle[Routes.RESET_PASSWORD_ARG_EMAIL] ?: ""

    private val _state = MutableStateFlow<ResetPasswordUiState>(ResetPasswordUiState.Idle)
    val state: StateFlow<ResetPasswordUiState> = _state.asStateFlow()

    fun submit(code: String, newPassword: String, confirmPassword: String) {
        if (!validate(code, newPassword, confirmPassword)) return
        _state.value = ResetPasswordUiState.Submitting
        viewModelScope.launch {
            _state.value = when (val result = authRepository.resetPassword(email, code, newPassword)) {
                AuthActionResult.Success    -> ResetPasswordUiState.Success
                is AuthActionResult.Failure -> ResetPasswordUiState.Error(result.message)
            }
        }
    }

    fun consumeError() {
        _state.update {
            if (it is ResetPasswordUiState.Error) ResetPasswordUiState.Idle else it
        }
    }

    private fun validate(code: String, newPassword: String, confirmPassword: String): Boolean {
        val errorMsg = when {
            email.isBlank()                       -> "E-mail perdido no caminho. Volta pra tela anterior."
            !CODE_REGEX.matches(code.trim())      -> "Código deve ter 6 dígitos numéricos."
            newPassword.length < MIN_PASSWORD     -> "Nova senha precisa ter ao menos 8 caracteres."
            newPassword != confirmPassword        -> "As senhas não conferem."
            else                                   -> null
        }
        return if (errorMsg != null) {
            _state.value = ResetPasswordUiState.Error(errorMsg)
            false
        } else {
            true
        }
    }

    private companion object {
        val CODE_REGEX = Regex("""^\d{6}$""")
        const val MIN_PASSWORD = 8
    }
}
