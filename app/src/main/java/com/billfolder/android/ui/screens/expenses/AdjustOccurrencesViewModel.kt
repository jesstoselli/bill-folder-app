package com.billfolder.android.ui.screens.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.ExpenseResponse
import com.billfolder.android.data.repository.ExpensesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/** Mesmo teto do backend: um ciclo tem até 5 semanas, com folga pra sessões extras. */
internal const val MAX_OCCURRENCES = 10

/**
 * Sessões de uma despesa provisionada NESTE ciclo (férias da terapeuta, sessão
 * extra). Só muda este ciclo; o total recalcula = valor por sessão × sessões.
 */
data class AdjustOccurrencesFormState(
    val expenseId: String = "",
    val perSession: Double = 0.0,
    val originalCount: Int = 0,
    val paidCount: Int = 0,
    val count: Int = 0,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedSuccessfully: Boolean = false,
) {
    /** Abaixo das já pagas o backend recusa; zero sessões é "excluir só esta". */
    val minCount: Int get() = maxOf(1, paidCount)
    val canDecrement: Boolean get() = count > minCount
    val canIncrement: Boolean get() = count < MAX_OCCURRENCES
    val newTotal: Double get() = perSession * count
    val originalTotal: Double get() = perSession * originalCount
    val hasChanged: Boolean get() = count != originalCount
}

@HiltViewModel
class AdjustOccurrencesViewModel @Inject constructor(
    private val expensesRepository: ExpensesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AdjustOccurrencesFormState())
    val state: StateFlow<AdjustOccurrencesFormState> = _state.asStateFlow()

    fun initializeFor(expense: ExpenseResponse) {
        val total = expense.occurrencesTotal ?: 0
        _state.value = AdjustOccurrencesFormState(
            expenseId = expense.id,
            perSession = expense.occurrenceAmount ?: 0.0,
            originalCount = total,
            paidCount = expense.occurrencesPaid,
            count = total,
        )
    }

    fun decrement() = _state.update { if (it.canDecrement) it.copy(count = it.count - 1) else it }

    fun increment() = _state.update { if (it.canIncrement) it.copy(count = it.count + 1) else it }

    fun submit() {
        val current = _state.value
        if (!current.hasChanged || current.isSaving) return

        _state.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                expensesRepository.updateOccurrences(current.expenseId, current.count)
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
}
