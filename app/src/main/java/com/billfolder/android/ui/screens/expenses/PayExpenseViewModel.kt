package com.billfolder.android.ui.screens.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.CheckingAccountResponse
import com.billfolder.android.data.repository.ExpensesRepository
import com.billfolder.android.data.repository.ReferenceDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject

/**
 * ViewModel do PayExpenseSheet. Pré-preenche `paidDate=hoje` e
 * `actualAmount=expectedAmount` (mesma lógica do auto-fill que o backend
 * faz se vierem null) — caller pode editar antes de confirmar.
 *
 * Se `paidFromAccountId` ficar null, backend não trava — apenas registra
 * a despesa como paga sem associar a uma conta. Se vier preenchido,
 * armazena qual conta saiu.
 */
data class PayExpenseFormState(
    val expenseId: String = "",
    val paidDate: String = LocalDate.now().toString(),
    val actualAmount: String = "",
    val selectedAccountId: String? = null,

    val accounts: List<CheckingAccountResponse> = emptyList(),
    val isLoadingReferences: Boolean = true,

    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedSuccessfully: Boolean = false,
)

@HiltViewModel
class PayExpenseViewModel @Inject constructor(
    private val referenceDataRepository: ReferenceDataRepository,
    private val expensesRepository: ExpensesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PayExpenseFormState())
    val state: StateFlow<PayExpenseFormState> = _state.asStateFlow()

    /**
     * Inicializa o formulário pra uma despesa específica. Caller chama
     * isso assim que o sheet aparece passando expense.id e expectedAmount
     * do response — não precisamos GET-ar de novo.
     */
    fun initializeFor(expenseId: String, expectedAmount: Double) {
        _state.update {
            it.copy(
                expenseId = expenseId,
                actualAmount = formatAmount(expectedAmount),
            )
        }
        loadAccounts()
    }

    fun onPaidDateChange(iso: String) = _state.update { it.copy(paidDate = iso) }
    fun onActualAmountChange(value: String) = _state.update { it.copy(actualAmount = value) }
    fun onAccountChange(id: String) = _state.update { it.copy(selectedAccountId = id) }

    fun submit(amountInvalidMessage: String) {
        val current = _state.value
        val amountValue = parseAmount(current.actualAmount) ?: 0.0
        if (amountValue <= 0) {
            _state.update { it.copy(errorMessage = amountInvalidMessage) }
            return
        }

        _state.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                expensesRepository.markPaid(
                    id = current.expenseId,
                    paidDate = current.paidDate,
                    actualAmount = amountValue,
                    paidFromAccountId = current.selectedAccountId,
                )
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

    private fun loadAccounts() {
        viewModelScope.launch {
            try {
                val accounts = referenceDataRepository.getCheckingAccounts()
                _state.update {
                    it.copy(
                        accounts = accounts,
                        // Pré-seleciona a conta primária — UX nice-to-have
                        selectedAccountId = it.selectedAccountId
                            ?: accounts.firstOrNull { acc -> acc.isPrimary }?.id
                            ?: accounts.firstOrNull()?.id,
                        isLoadingReferences = false,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoadingReferences = false,
                        errorMessage = "Falha ao carregar contas.",
                    )
                }
            }
        }
    }

    private fun parseAmount(input: String): Double? =
        input.replace(',', '.').toDoubleOrNull()

    /** "1234.56" → "1234.56" (não formata BRL aqui — UI espera valor cru). */
    private fun formatAmount(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}
