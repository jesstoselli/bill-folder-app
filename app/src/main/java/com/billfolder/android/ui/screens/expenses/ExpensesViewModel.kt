package com.billfolder.android.ui.screens.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.CycleResponse
import com.billfolder.android.data.dto.ExpenseResponse
import com.billfolder.android.data.repository.CyclesRepository
import com.billfolder.android.data.repository.ExpensesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/**
 * Estados da tela "despesas". Padrão idêntico ao DailyExpensesViewModel,
 * com 3 status visuais derivados do `ExpenseResponse.status`:
 *  - "overdue": atrasada (computed: status=pending E dueDate<hoje)
 *  - "pending": pendente (futura ou hoje)
 *  - "paid":    paga
 *
 * O backend já entrega o status computado correto — UI só agrupa.
 */
sealed interface ExpensesUiState {
    data object Loading : ExpensesUiState
    data object NoCycle : ExpensesUiState
    data class Content(
        val cycle: CycleResponse,
        val expenses: List<ExpenseResponse>,
    ) : ExpensesUiState
    data class Error(val message: String) : ExpensesUiState
}

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val cyclesRepository: CyclesRepository,
    private val expensesRepository: ExpensesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ExpensesUiState>(ExpensesUiState.Loading)
    val state: StateFlow<ExpensesUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun refresh() {
        if (_state.value is ExpensesUiState.Loading) return
        _state.value = ExpensesUiState.Loading
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = try {
                val cycle = cyclesRepository.getCurrent()
                val expenses = expensesRepository.list(
                    from = cycle.startDate,
                    to = cycle.endDate,
                )
                ExpensesUiState.Content(
                    cycle = cycle,
                    // Ordenação: overdue/pending por dueDate ascendente (o que
                    // vence antes vai primeiro); paid por dueDate descendente
                    // (mais recentes primeiro). Mantemos lista plana e separa
                    // por seção na UI.
                    expenses = expenses.sortedBy { it.dueDate },
                )
            } catch (e: HttpException) {
                if (e.code() == HTTP_NOT_FOUND) {
                    ExpensesUiState.NoCycle
                } else {
                    ExpensesUiState.Error("Erro ao carregar (HTTP ${e.code()})")
                }
            } catch (e: IOException) {
                ExpensesUiState.Error("Sem conexão. Verifique sua internet.")
            } catch (e: Exception) {
                ExpensesUiState.Error(e.message ?: "Algo deu errado.")
            }
        }
    }

    private companion object {
        const val HTTP_NOT_FOUND = 404
    }
}
