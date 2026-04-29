package com.billfolder.android.ui.screens.dailyexpenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.CategoryDto
import com.billfolder.android.data.dto.CheckingAccountResponse
import com.billfolder.android.data.dto.CreateDailyExpenseRequest
import com.billfolder.android.data.dto.DailyExpenseResponse
import com.billfolder.android.data.dto.UpdateDailyExpenseRequest
import com.billfolder.android.data.repository.DailyExpensesRepository
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
 * Estado do formulário "nova/editar despesa avulsa".
 *
 * O mesmo VM serve pros dois modos (create e edit) — o sheet diferencia
 * só pelo `existing` que recebe; quando não-null, chama `prefill()` no
 * VM e o submit vira PATCH em vez de POST.
 *
 * - Campos editáveis pela UI (date, label, amount, etc).
 * - References (categories, accounts) carregadas no init e expostas
 *   pra UI montar dropdowns.
 * - `submission` controla loading + erros do POST/PATCH.
 * - `savedSuccessfully` vira true quando o backend confirma; UI usa
 *   pra fechar o sheet e disparar refresh da lista.
 * - `editingId`: quando não-null, submit faz PATCH desse id em vez de POST.
 */
data class AddDailyExpenseFormState(
    // Editáveis
    val date: String = LocalDate.now().toString(),
    val label: String = "",
    val amount: String = "",
    val selectedCategoryId: String? = null,
    val selectedAccountId: String? = null,
    val notes: String = "",

    // References (loaded async)
    val categories: List<CategoryDto> = emptyList(),
    val accounts: List<CheckingAccountResponse> = emptyList(),
    val isLoadingReferences: Boolean = true,

    // Submit state
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedSuccessfully: Boolean = false,

    // Edit mode
    val editingId: String? = null,
)

@HiltViewModel
class AddDailyExpenseViewModel @Inject constructor(
    private val referenceDataRepository: ReferenceDataRepository,
    private val dailyExpensesRepository: DailyExpensesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AddDailyExpenseFormState())
    val state: StateFlow<AddDailyExpenseFormState> = _state.asStateFlow()

    init {
        loadReferences()
    }

    // ---- Field updaters (UI chama no onValueChange dos inputs) ----

    fun onDateChange(iso: String) = _state.update { it.copy(date = iso) }
    fun onLabelChange(value: String) = _state.update { it.copy(label = value) }
    fun onAmountChange(value: String) = _state.update { it.copy(amount = value) }
    fun onCategoryChange(id: String) = _state.update { it.copy(selectedCategoryId = id) }
    fun onAccountChange(id: String) = _state.update { it.copy(selectedAccountId = id) }
    fun onNotesChange(value: String) = _state.update { it.copy(notes = value) }

    /**
     * Preenche o form com os dados de uma despesa existente — modo edit.
     * O sheet chama uma vez no LaunchedEffect(existing). Idempotente:
     * se o `editingId` já bate, não faz nada (evita resetar campos que
     * o user já modificou se a sheet recompor sem motivo).
     */
    fun prefill(item: DailyExpenseResponse) {
        if (_state.value.editingId == item.id) return
        _state.update {
            it.copy(
                editingId = item.id,
                date = item.date,
                label = item.label,
                amount = item.amount.toBrlInputString(),
                selectedCategoryId = item.categoryId,
                selectedAccountId = item.accountId,
                notes = item.notes.orEmpty(),
                errorMessage = null,
            )
        }
    }

    // ---- Submit ----

    fun submit(
        labelEmptyMessage: String,
        amountInvalidMessage: String,
        categoryEmptyMessage: String,
        accountEmptyMessage: String,
    ) {
        val current = _state.value

        val validationError = when {
            current.label.isBlank()                   -> labelEmptyMessage
            (parseAmount(current.amount) ?: 0.0) <= 0 -> amountInvalidMessage
            current.selectedCategoryId.isNullOrBlank() -> categoryEmptyMessage
            current.selectedAccountId.isNullOrBlank()  -> accountEmptyMessage
            else                                       -> null
        }
        if (validationError != null) {
            _state.update { it.copy(errorMessage = validationError) }
            return
        }

        _state.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                if (current.editingId != null) {
                    // PATCH parcial. Mandamos todos os campos que o user
                    // pode editar — backend trata null em cada um. Notes
                    // fica string vazia se o user limpou (convenção
                    // documentada no backend).
                    val request = UpdateDailyExpenseRequest(
                        date = current.date,
                        label = current.label.trim(),
                        amount = parseAmount(current.amount),
                        categoryId = current.selectedCategoryId,
                        accountId = current.selectedAccountId,
                        notes = current.notes.trim(),
                    )
                    dailyExpensesRepository.update(current.editingId, request)
                } else {
                    val request = CreateDailyExpenseRequest(
                        date = current.date,
                        label = current.label.trim(),
                        amount = parseAmount(current.amount)!!,
                        categoryId = current.selectedCategoryId!!,
                        accountId = current.selectedAccountId!!,
                        notes = current.notes.takeIf { it.isNotBlank() }?.trim(),
                    )
                    dailyExpensesRepository.create(request)
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

    // ---- Internals ----

    private fun loadReferences() {
        viewModelScope.launch {
            try {
                val categories = referenceDataRepository.getCategories()
                val accounts = referenceDataRepository.getCheckingAccounts()
                _state.update {
                    it.copy(
                        categories = categories,
                        accounts = accounts,
                        // Pré-seleciona a conta primária se houver — UX nice-to-have.
                        // Em modo edit (editingId != null), respeita o que veio do
                        // prefill (já tem selectedAccountId).
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
                        errorMessage = "Falha ao carregar opções (${e.message ?: "erro"}).",
                    )
                }
            }
        }
    }

    /**
     * Aceita "1234.56" ou "1234,56" (vírgula como separador decimal típico
     * em pt-BR). Retorna null se inválido.
     */
    private fun parseAmount(input: String): Double? =
        input.replace(',', '.').toDoubleOrNull()

    /**
     * Formata o amount pra string de input do MoneyField. Usa vírgula
     * como decimal pra ficar consistente com o que o user digita em pt-BR.
     */
    private fun Double.toBrlInputString(): String =
        "%.2f".format(this).replace('.', ',')
}
