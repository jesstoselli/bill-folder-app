package com.billfolder.android.ui.screens.expenses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.CycleResponse
import com.billfolder.android.data.dto.ExpenseResponse
import com.billfolder.android.data.repository.CyclesRepository
import com.billfolder.android.data.repository.ExpensesRepository
import com.billfolder.android.ui.util.CycleDirection
import com.billfolder.android.ui.util.observeDrawerRefresh
import com.billfolder.android.ui.util.resolveAdjacentCycle
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
 * Estados da tela "despesas". Padrão idêntico ao DailyExpensesViewModel,
 * com 3 status visuais derivados do `ExpenseResponse.status`:
 *  - "overdue": atrasada (computed: status=pending E dueDate<hoje)
 *  - "pending": pendente (futura ou hoje)
 *  - "paid":    paga
 *
 * O backend já entrega o status computado correto — UI só agrupa.
 *
 * Estados de ação por row:
 *  - pendingDelete: row em swipe-left aguardando confirmação no AlertDialog
 *  - editing: row em swipe-right com sheet de edit aberta
 *  - deletingId: id da row sendo deletada (entre confirm e resposta)
 *
 * Importante: o "pagar despesa" continua sendo um fluxo separado (tap
 * na row pending/overdue → PayExpenseSheet). Swipe-right é pra editar
 * a despesa em si — campos como label, dueDate, expectedAmount.
 */
sealed interface ExpensesUiState {
    data object Loading : ExpensesUiState
    data object NoCycle : ExpensesUiState
    data class Content(
        val cycle: CycleResponse,
        val expenses: List<ExpenseResponse>,
        val pendingDelete: ExpenseResponse? = null,
        val editing: ExpenseResponse? = null,
        val deletingId: String? = null,
        val cycles: List<CycleResponse> = emptyList(),
        val isSwitchingCycle: Boolean = false,
        val isRefreshing: Boolean = false,
    ) : ExpensesUiState
    data class Error(val message: String) : ExpensesUiState
}

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cyclesRepository: CyclesRepository,
    private val expensesRepository: ExpensesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ExpensesUiState>(ExpensesUiState.Loading)
    val state: StateFlow<ExpensesUiState> = _state.asStateFlow()

    init {
        load()
        observeDrawerRefresh(savedStateHandle) { pullRefresh() }
    }

    fun refresh() {
        if (_state.value is ExpensesUiState.Loading) return
        _state.value = ExpensesUiState.Loading
        load()
    }

    /** Pull-to-refresh: refetch sem apagar a tela. */
    fun pullRefresh() {
        val current = _state.value
        if (current !is ExpensesUiState.Content) {
            refresh()
            return
        }
        _state.update { (it as? ExpensesUiState.Content)?.copy(isRefreshing = true) ?: it }
        viewModelScope.launch {
            try {
                val cycle = cyclesRepository.getCurrent()
                val expenses = expensesRepository.list(from = cycle.startDate, to = cycle.endDate)
                val cycles = runCatching { cyclesRepository.list() }.getOrDefault(emptyList())
                _state.update {
                    (it as? ExpensesUiState.Content)?.copy(
                        cycle = cycle,
                        expenses = expenses.sortedBy { it.dueDate },
                        cycles = cycles,
                        isRefreshing = false,
                    ) ?: it
                }
            } catch (e: Exception) {
                _state.update { (it as? ExpensesUiState.Content)?.copy(isRefreshing = false) ?: it }
            }
        }
    }

    // ------------------------------------------------------------------------
    // Delete flow (swipe-left)
    // ------------------------------------------------------------------------

    fun requestDelete(item: ExpenseResponse) {
        _state.update { current ->
            if (current is ExpensesUiState.Content) {
                current.copy(pendingDelete = item)
            } else {
                current
            }
        }
    }

    fun cancelDelete() {
        _state.update { current ->
            if (current is ExpensesUiState.Content) {
                current.copy(pendingDelete = null)
            } else {
                current
            }
        }
    }

    fun confirmDelete() {
        val current = _state.value
        if (current !is ExpensesUiState.Content) return
        val item = current.pendingDelete ?: return

        _state.update {
            (it as? ExpensesUiState.Content)
                ?.copy(pendingDelete = null, deletingId = item.id)
                ?: it
        }

        viewModelScope.launch {
            try {
                expensesRepository.delete(item.id)
                _state.update { s ->
                    if (s is ExpensesUiState.Content) {
                        s.copy(
                            expenses = s.expenses.filterNot { it.id == item.id },
                            deletingId = null,
                        )
                    } else {
                        s
                    }
                }
            } catch (e: HttpException) {
                _state.value = ExpensesUiState.Error("Erro ao deletar (HTTP ${e.code()})")
            } catch (e: IOException) {
                _state.value = ExpensesUiState.Error("Sem conexão. Tenta de novo.")
            } catch (e: Exception) {
                _state.value = ExpensesUiState.Error(e.message ?: "Algo deu errado.")
            }
        }
    }

    // ------------------------------------------------------------------------
    // Edit flow (swipe-right) — toggles do flag; PATCH é feito pela sheet
    // ------------------------------------------------------------------------

    fun requestEdit(item: ExpenseResponse) {
        _state.update { current ->
            if (current is ExpensesUiState.Content) {
                current.copy(editing = item)
            } else {
                current
            }
        }
    }

    fun cancelEdit() {
        _state.update { current ->
            if (current is ExpensesUiState.Content) {
                current.copy(editing = null)
            } else {
                current
            }
        }
    }

    // ------------------------------------------------------------------------
    // Cycle navigation (setinhas do CycleNavigator)
    // ------------------------------------------------------------------------

    fun goToPreviousCycle() = navigate(CycleDirection.PREVIOUS)
    fun goToNextCycle()     = navigate(CycleDirection.NEXT)

    private fun navigate(direction: CycleDirection) {
        val current = _state.value as? ExpensesUiState.Content ?: return
        if (current.isSwitchingCycle) return
        val target = resolveAdjacentCycle(current.cycles, current.cycle.id, direction)
            ?: return

        _state.update {
            (it as? ExpensesUiState.Content)?.copy(isSwitchingCycle = true) ?: it
        }

        viewModelScope.launch {
            try {
                val expenses = expensesRepository.list(
                    from = target.startDate,
                    to = target.endDate,
                )
                _state.update { s ->
                    (s as? ExpensesUiState.Content)?.copy(
                        cycle = target,
                        expenses = expenses.sortedBy { it.dueDate },
                        isSwitchingCycle = false,
                        pendingDelete = null,
                        editing = null,
                    ) ?: s
                }
            } catch (e: Exception) {
                _state.update { s ->
                    (s as? ExpensesUiState.Content)?.copy(isSwitchingCycle = false) ?: s
                }
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = try {
                val cycle = cyclesRepository.getCurrent()
                val expenses = expensesRepository.list(
                    from = cycle.startDate,
                    to = cycle.endDate,
                )
                val cycles = runCatching { cyclesRepository.list() }.getOrDefault(emptyList())
                ExpensesUiState.Content(
                    cycle = cycle,
                    // Ordenação: overdue/pending por dueDate ascendente (o que
                    // vence antes vai primeiro); paid por dueDate descendente
                    // (mais recentes primeiro). Mantemos lista plana e separa
                    // por seção na UI.
                    expenses = expenses.sortedBy { it.dueDate },
                    cycles = cycles,
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
