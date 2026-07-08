package com.billfolder.android.ui.screens.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.CategoryDto
import com.billfolder.android.data.dto.CreateExpenseRecurrenceRequest
import com.billfolder.android.data.repository.ExpenseRecurrencesRepository
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
 * Estado do formulário "nova recorrência semanal". Cria um template que o
 * backend usa pra gerar uma despesa provisionada a cada ciclo (ex:
 * fisioterapia toda quarta). Mesma estrutura do AddExpenseViewModel
 * (form state, resetForm, submit, savedSuccessfully), mas o payload é um
 * CreateExpenseRecurrenceRequest com frequency="weekly".
 *
 * weekday: 0=domingo .. 6=sábado (contrato do backend). null enquanto o
 * user não escolhe.
 */
data class AddWeeklyRecurrenceFormState(
    val label: String = "",
    val amount: String = "",
    val selectedCategoryId: String? = null,
    val weekday: Int? = null,
    val startDate: String = LocalDate.now().toString(),

    val categories: List<CategoryDto> = emptyList(),
    val isLoadingReferences: Boolean = true,

    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedSuccessfully: Boolean = false,
)

@HiltViewModel
class AddWeeklyRecurrenceViewModel @Inject constructor(
    private val referenceDataRepository: ReferenceDataRepository,
    private val expenseRecurrencesRepository: ExpenseRecurrencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AddWeeklyRecurrenceFormState())
    val state: StateFlow<AddWeeklyRecurrenceFormState> = _state.asStateFlow()

    init {
        loadCategories()
    }

    /**
     * Reseta o form pros valores iniciais. Preserva categories/
     * isLoadingReferences porque o init só roda uma vez — resetar zeraria
     * o dropdown. Mesmo racional do AddExpenseViewModel.
     */
    fun resetForm() {
        val current = _state.value
        _state.value = AddWeeklyRecurrenceFormState(
            categories = current.categories,
            isLoadingReferences = current.isLoadingReferences,
        )
    }

    fun onLabelChange(value: String) = _state.update { it.copy(label = value) }
    fun onAmountChange(value: String) = _state.update { it.copy(amount = value) }
    fun onCategoryChange(id: String) = _state.update { it.copy(selectedCategoryId = id) }
    fun onWeekdayChange(weekday: Int) = _state.update { it.copy(weekday = weekday) }
    fun onStartDateChange(iso: String) = _state.update { it.copy(startDate = iso) }

    fun submit(
        labelEmptyMessage: String,
        amountInvalidMessage: String,
        categoryEmptyMessage: String,
        weekdayEmptyMessage: String,
    ) {
        val current = _state.value
        val validationError = when {
            current.label.isBlank()                    -> labelEmptyMessage
            (parseAmount(current.amount) ?: 0.0) <= 0  -> amountInvalidMessage
            current.selectedCategoryId.isNullOrBlank() -> categoryEmptyMessage
            current.weekday == null                    -> weekdayEmptyMessage
            else                                        -> null
        }
        if (validationError != null) {
            _state.update { it.copy(errorMessage = validationError) }
            return
        }

        _state.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val request = CreateExpenseRecurrenceRequest(
                    defaultLabel = current.label.trim(),
                    defaultAmount = parseAmount(current.amount)!!,
                    defaultCategoryId = current.selectedCategoryId!!,
                    frequency = "weekly",
                    dueDay = null,
                    weekday = current.weekday!!,
                    startDate = current.startDate,
                )
                expenseRecurrencesRepository.create(request)
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
}
