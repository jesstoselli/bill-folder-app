package com.billfolder.android.ui.screens.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.CategoryDto
import com.billfolder.android.data.dto.CreateExpenseRequest
import com.billfolder.android.data.dto.ExpenseResponse
import com.billfolder.android.data.dto.UpdateExpenseRequest
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
 * Estado do formulário "nova/editar despesa".
 *
 * Modos:
 *  - Create (editingId == null): POST.
 *  - Edit (editingId != null): PATCH com UpdateExpenseRequest mandando
 *    apenas os campos editáveis pelo user (não mexe em status/paid*).
 *
 * O fluxo de pagamento é separado (PayExpenseSheet/PayExpenseViewModel)
 * — não tocamos aqui pra não acoplar.
 */
data class AddExpenseFormState(
    val dueDate: String = LocalDate.now().toString(),
    val label: String = "",
    val amount: String = "",
    val selectedCategoryId: String? = null,
    val notes: String = "",

    val categories: List<CategoryDto> = emptyList(),
    val isLoadingReferences: Boolean = true,

    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedSuccessfully: Boolean = false,

    val editingId: String? = null,
)

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val referenceDataRepository: ReferenceDataRepository,
    private val expensesRepository: ExpensesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AddExpenseFormState())
    val state: StateFlow<AddExpenseFormState> = _state.asStateFlow()

    init {
        loadCategories()
    }

    fun onDueDateChange(iso: String) = _state.update { it.copy(dueDate = iso) }
    fun onLabelChange(value: String) = _state.update { it.copy(label = value) }
    fun onAmountChange(value: String) = _state.update { it.copy(amount = value) }
    fun onCategoryChange(id: String) = _state.update { it.copy(selectedCategoryId = id) }
    fun onNotesChange(value: String) = _state.update { it.copy(notes = value) }

    /**
     * Preenche o form com uma despesa existente — modo edit. Idempotente
     * (checa editingId pra não resetar campos que o user já modificou
     * num recompose acidental do sheet).
     */
    fun prefill(item: ExpenseResponse) {
        if (_state.value.editingId == item.id) return
        _state.update {
            it.copy(
                editingId = item.id,
                dueDate = item.dueDate,
                label = item.label,
                amount = item.expectedAmount.toBrlInputString(),
                selectedCategoryId = item.categoryId,
                notes = item.notes.orEmpty(),
                errorMessage = null,
            )
        }
    }

    fun submit(
        labelEmptyMessage: String,
        amountInvalidMessage: String,
        categoryEmptyMessage: String,
    ) {
        val current = _state.value
        val validationError = when {
            current.label.isBlank()                    -> labelEmptyMessage
            (parseAmount(current.amount) ?: 0.0) <= 0  -> amountInvalidMessage
            current.selectedCategoryId.isNullOrBlank() -> categoryEmptyMessage
            else                                        -> null
        }
        if (validationError != null) {
            _state.update { it.copy(errorMessage = validationError) }
            return
        }

        _state.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                if (current.editingId != null) {
                    // PATCH — só os campos editáveis pelo user. Não mandamos
                    // status/paid* aqui pra não interferir no pay flow do
                    // PayExpenseSheet (que tem seu próprio caminho).
                    val request = UpdateExpenseRequest(
                        dueDate = current.dueDate,
                        label = current.label.trim(),
                        expectedAmount = parseAmount(current.amount),
                        categoryId = current.selectedCategoryId,
                        notes = current.notes.trim(),
                    )
                    expensesRepository.update(current.editingId, request)
                } else {
                    val request = CreateExpenseRequest(
                        dueDate = current.dueDate,
                        label = current.label.trim(),
                        expectedAmount = parseAmount(current.amount)!!,
                        categoryId = current.selectedCategoryId!!,
                        notes = current.notes.takeIf { it.isNotBlank() }?.trim(),
                    )
                    expensesRepository.create(request)
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

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val cats = referenceDataRepository.getCategories()
                _state.update { it.copy(categories = cats, isLoadingReferences = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoadingReferences = false,
                        errorMessage = "Falha ao carregar categorias.",
                    )
                }
            }
        }
    }

    private fun parseAmount(input: String): Double? =
        input.replace(',', '.').toDoubleOrNull()

    /** "1234.5" → "1234,50" pra preencher o MoneyField em modo edit. */
    private fun Double.toBrlInputString(): String =
        "%.2f".format(this).replace('.', ',')
}
