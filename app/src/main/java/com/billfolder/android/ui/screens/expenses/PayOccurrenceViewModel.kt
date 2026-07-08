package com.billfolder.android.ui.screens.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.CheckingAccountResponse
import com.billfolder.android.data.dto.PayOccurrenceRequest
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
 * ViewModel do PayOccurrenceSheet — "dar baixa" numa ocorrência semanal de
 * uma despesa provisionada. Variante enxuta do PayExpenseViewModel: mesma
 * ideia de pré-preencher data (=hoje) e valor (=occurrenceAmount), mas chama
 * expensesRepository.payOccurrence em vez de markPaid.
 *
 * Diferente do markPaid, aqui o backend NÃO quita a despesa inteira — só
 * incrementa occurrencesPaid/paidToDate. paidFromAccountId continua opcional.
 */
data class PayOccurrenceFormState(
    val expenseId: String = "",
    val paidDate: String = LocalDate.now().toString(),
    val amount: String = "",
    val selectedAccountId: String? = null,

    val accounts: List<CheckingAccountResponse> = emptyList(),
    val isLoadingReferences: Boolean = true,

    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedSuccessfully: Boolean = false,
)

@HiltViewModel
class PayOccurrenceViewModel @Inject constructor(
    private val referenceDataRepository: ReferenceDataRepository,
    private val expensesRepository: ExpensesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PayOccurrenceFormState())
    val state: StateFlow<PayOccurrenceFormState> = _state.asStateFlow()

    /**
     * Reseta o form. Chamado pelo sheet toda vez que abre (o hiltViewModel()
     * é compartilhado no scope da tela pai) — sem isso, savedSuccessfully
     * ficaria stuck em true da submissão anterior.
     */
    fun resetForm() {
        _state.value = PayOccurrenceFormState()
    }

    /**
     * Inicializa pra uma ocorrência específica. O caller passa o id da
     * despesa e o occurrenceAmount (valor de uma baixa) pra pré-preencher.
     */
    fun initializeFor(expenseId: String, occurrenceAmount: Double) {
        _state.update {
            it.copy(
                expenseId = expenseId,
                amount = formatAmount(occurrenceAmount),
            )
        }
        loadAccounts()
    }

    fun onPaidDateChange(iso: String) = _state.update { it.copy(paidDate = iso) }
    fun onAmountChange(value: String) = _state.update { it.copy(amount = value) }
    fun onAccountChange(id: String) = _state.update { it.copy(selectedAccountId = id) }

    fun submit(amountInvalidMessage: String) {
        val current = _state.value
        val amountValue = parseAmount(current.amount) ?: 0.0
        if (amountValue <= 0) {
            _state.update { it.copy(errorMessage = amountInvalidMessage) }
            return
        }

        _state.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                expensesRepository.payOccurrence(
                    id = current.expenseId,
                    request = PayOccurrenceRequest(
                        paidDate = current.paidDate,
                        amount = amountValue,
                        paidFromAccountId = current.selectedAccountId,
                    ),
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
