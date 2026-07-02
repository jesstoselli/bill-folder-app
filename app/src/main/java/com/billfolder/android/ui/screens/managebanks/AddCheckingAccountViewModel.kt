package com.billfolder.android.ui.screens.managebanks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.CheckingAccountResponse
import com.billfolder.android.data.dto.CreateCheckingAccountRequest
import com.billfolder.android.data.dto.UpdateCheckingAccountRequest
import com.billfolder.android.data.repository.CheckingAccountsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/**
 * Form de "nova/editar conta corrente".
 *
 * Modos:
 *  - Create (editingId == null): POST.
 *  - Edit (editingId != null): PATCH parcial.
 *
 * isPrimary: se marcada true, backend automaticamente desmarca as outras
 * primaries do user. Não precisamos coordenar client-side.
 *
 * Validações alinhadas com backend:
 *  - bankName: obrigatório, max 100
 *  - branch: obrigatório, max 20
 *  - accountNumber: obrigatório, max 30
 *  - initialBalance: >= 0
 */
data class AddCheckingAccountFormState(
    val bankName: String = "",
    val branch: String = "",
    val accountNumber: String = "",
    val initialBalance: String = "",
    val isPrimary: Boolean = false,

    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedSuccessfully: Boolean = false,

    val editingId: String? = null,
)

@HiltViewModel
class AddCheckingAccountViewModel @Inject constructor(
    private val checkingAccountsRepository: CheckingAccountsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AddCheckingAccountFormState())
    val state: StateFlow<AddCheckingAccountFormState> = _state.asStateFlow()

    /**
     * Reseta o form. Chamado pelo sheet ao abrir E ao fechar
     * (LaunchedEffect + DisposableEffect) — evita state stuck entre
     * aberturas do sheet compartilhado no scope da tela pai.
     */
    fun resetForm() {
        _state.value = AddCheckingAccountFormState()
    }

    fun onBankNameChange(value: String) = _state.update { it.copy(bankName = value) }
    fun onBranchChange(value: String) = _state.update { it.copy(branch = value) }
    fun onAccountNumberChange(value: String) = _state.update { it.copy(accountNumber = value) }
    fun onInitialBalanceChange(value: String) = _state.update { it.copy(initialBalance = value) }
    fun onIsPrimaryChange(value: Boolean) = _state.update { it.copy(isPrimary = value) }

    fun prefill(account: CheckingAccountResponse) {
        if (_state.value.editingId == account.id) return
        _state.update {
            it.copy(
                editingId = account.id,
                bankName = account.bankName,
                branch = account.branch.orEmpty(),
                accountNumber = account.accountNumber.orEmpty(),
                initialBalance = account.initialBalance.toBrlInputString(),
                isPrimary = account.isPrimary,
                errorMessage = null,
            )
        }
    }

    fun submit(
        bankNameEmptyMessage: String,
        branchEmptyMessage: String,
        accountNumberEmptyMessage: String,
        initialBalanceInvalidMessage: String,
    ) {
        val current = _state.value
        val balance = parseAmount(current.initialBalance)

        val validationError = when {
            current.bankName.isBlank()      -> bankNameEmptyMessage
            current.branch.isBlank()        -> branchEmptyMessage
            current.accountNumber.isBlank() -> accountNumberEmptyMessage
            balance == null || balance < 0  -> initialBalanceInvalidMessage
            else                             -> null
        }
        if (validationError != null) {
            _state.update { it.copy(errorMessage = validationError) }
            return
        }

        _state.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                if (current.editingId != null) {
                    val request = UpdateCheckingAccountRequest(
                        bankName = current.bankName.trim(),
                        branch = current.branch.trim(),
                        accountNumber = current.accountNumber.trim(),
                        initialBalance = balance,
                        isPrimary = current.isPrimary,
                    )
                    checkingAccountsRepository.updateAccount(current.editingId, request)
                } else {
                    val request = CreateCheckingAccountRequest(
                        bankName = current.bankName.trim(),
                        branch = current.branch.trim(),
                        accountNumber = current.accountNumber.trim(),
                        initialBalance = balance!!,
                        isPrimary = current.isPrimary,
                    )
                    checkingAccountsRepository.createAccount(request)
                }
                _state.update { it.copy(isSaving = false, savedSuccessfully = true) }
            } catch (e: HttpException) {
                _state.update { it.copy(isSaving = false, errorMessage = "Erro do servidor (HTTP ${e.code()}).") }
            } catch (e: IOException) {
                _state.update { it.copy(isSaving = false, errorMessage = "Sem conexão. Tenta de novo.") }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, errorMessage = e.message ?: "Algo deu errado.") }
            }
        }
    }

    private fun parseAmount(input: String): Double? =
        input.replace(',', '.').toDoubleOrNull()

    private fun Double.toBrlInputString(): String =
        "%.2f".format(this).replace('.', ',')
}
