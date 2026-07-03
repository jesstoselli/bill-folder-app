package com.billfolder.android.ui.screens.dailyexpenses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.CycleResponse
import com.billfolder.android.data.dto.DailyExpenseResponse
import com.billfolder.android.data.repository.CyclesRepository
import com.billfolder.android.data.repository.DailyExpensesRepository
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
 * Estados da tela "despesas avulsas".
 *
 * - Loading: spinner enquanto resolvemos ciclo + lista
 * - NoCycle: usuário não tem ciclo aberto. UI mostra empty state com CTA
 *   pra criar um ciclo (futuro)
 * - Content: carregamento ok; lista pode estar vazia (no app a tela mostra
 *   um empty state distinto de "sem ciclo")
 * - Error: falha de rede/servidor; UI exibe retry
 *
 * Estados de ação por row:
 *  - pendingDelete: row em swipe-left aguardando confirmação no AlertDialog
 *  - editing: row em swipe-right com a sheet de edit aberta
 *  - deletingId: id da row sendo deletada (entre confirm e resposta do
 *    backend) — placeholder pra futura UI de "deletando..." se quisermos
 */
sealed interface DailyExpensesUiState {
    data object Loading : DailyExpensesUiState
    data object NoCycle : DailyExpensesUiState
    data class Content(
        val cycle: CycleResponse,
        val expenses: List<DailyExpenseResponse>,
        val pendingDelete: DailyExpenseResponse? = null,
        val editing: DailyExpenseResponse? = null,
        val deletingId: String? = null,
        /** Lista completa de ciclos, pra resolver prev/next client-side. */
        val cycles: List<CycleResponse> = emptyList(),
        /** true durante refetch do ciclo prev/next — bloqueia tap-spam. */
        val isSwitchingCycle: Boolean = false,
        /** true durante pull-to-refresh — mantém dados visíveis. */
        val isRefreshing: Boolean = false,
    ) : DailyExpensesUiState
    data class Error(val message: String) : DailyExpensesUiState
}

@HiltViewModel
class DailyExpensesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cyclesRepository: CyclesRepository,
    private val dailyExpensesRepository: DailyExpensesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<DailyExpensesUiState>(DailyExpensesUiState.Loading)
    val state: StateFlow<DailyExpensesUiState> = _state.asStateFlow()

    init {
        load()
        observeDrawerRefresh(savedStateHandle) { pullRefresh() }
    }

    fun refresh() {
        if (_state.value is DailyExpensesUiState.Loading) return
        _state.value = DailyExpensesUiState.Loading
        load()
    }

    /** Pull-to-refresh: refetch sem apagar a tela. */
    fun pullRefresh() {
        val current = _state.value
        if (current !is DailyExpensesUiState.Content) {
            refresh()
            return
        }
        _state.update { (it as? DailyExpensesUiState.Content)?.copy(isRefreshing = true) ?: it }
        viewModelScope.launch {
            try {
                val cycle = cyclesRepository.getCurrent()
                val expenses = dailyExpensesRepository.list(
                    from = cycle.startDate,
                    to = cycle.endDate,
                )
                val cycles = runCatching { cyclesRepository.list() }.getOrDefault(emptyList())
                _state.update {
                    (it as? DailyExpensesUiState.Content)?.copy(
                        cycle = cycle,
                        expenses = expenses.sortedByDescending { it.date },
                        cycles = cycles,
                        isRefreshing = false,
                    ) ?: it
                }
            } catch (e: Exception) {
                _state.update { (it as? DailyExpensesUiState.Content)?.copy(isRefreshing = false) ?: it }
            }
        }
    }

    // ------------------------------------------------------------------------
    // Delete flow (swipe-left): request → AlertDialog → confirm/cancel
    // ------------------------------------------------------------------------

    /** Swipe-left completou — pede confirmação antes de bater no DELETE. */
    fun requestDelete(item: DailyExpenseResponse) {
        _state.update { current ->
            if (current is DailyExpensesUiState.Content) {
                current.copy(pendingDelete = item)
            } else {
                current
            }
        }
    }

    fun cancelDelete() {
        _state.update { current ->
            if (current is DailyExpensesUiState.Content) {
                current.copy(pendingDelete = null)
            } else {
                current
            }
        }
    }

    /**
     * User confirmou no dialog — bate DELETE. Em sucesso, remove
     * otimisticamente da lista local; o próximo refresh confirma com
     * o backend mesmo assim.
     */
    fun confirmDelete() {
        val current = _state.value
        if (current !is DailyExpensesUiState.Content) return
        val item = current.pendingDelete ?: return

        _state.update {
            (it as? DailyExpensesUiState.Content)
                ?.copy(pendingDelete = null, deletingId = item.id)
                ?: it
        }

        viewModelScope.launch {
            try {
                dailyExpensesRepository.delete(item.id)
                _state.update { s ->
                    if (s is DailyExpensesUiState.Content) {
                        s.copy(
                            expenses = s.expenses.filterNot { it.id == item.id },
                            deletingId = null,
                        )
                    } else {
                        s
                    }
                }
            } catch (e: HttpException) {
                _state.value = DailyExpensesUiState.Error("Erro ao deletar (HTTP ${e.code()})")
            } catch (e: IOException) {
                _state.value = DailyExpensesUiState.Error("Sem conexão. Tenta de novo.")
            } catch (e: Exception) {
                _state.value = DailyExpensesUiState.Error(e.message ?: "Algo deu errado.")
            }
        }
    }

    // ------------------------------------------------------------------------
    // Edit flow (swipe-right): request → sheet abre prefilled → save/cancel
    // ------------------------------------------------------------------------

    /**
     * Swipe-right completou — abre AddDailyExpenseSheet em modo edit.
     * O PATCH em si é feito pela sheet (que tem seu próprio VM); aqui
     * só guardamos qual item está sendo editado pra propagar pro
     * `existing` da sheet e pro `isPending` do SwipeToActionRow.
     */
    fun requestEdit(item: DailyExpenseResponse) {
        _state.update { current ->
            if (current is DailyExpensesUiState.Content) {
                current.copy(editing = item)
            } else {
                current
            }
        }
    }

    fun cancelEdit() {
        _state.update { current ->
            if (current is DailyExpensesUiState.Content) {
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
        val current = _state.value as? DailyExpensesUiState.Content ?: return
        if (current.isSwitchingCycle) return
        val target = resolveAdjacentCycle(current.cycles, current.cycle.id, direction)
            ?: return

        _state.update {
            (it as? DailyExpensesUiState.Content)?.copy(isSwitchingCycle = true) ?: it
        }

        viewModelScope.launch {
            try {
                val expenses = dailyExpensesRepository.list(
                    from = target.startDate,
                    to = target.endDate,
                )
                _state.update { s ->
                    (s as? DailyExpensesUiState.Content)?.copy(
                        cycle = target,
                        expenses = expenses.sortedByDescending { it.date },
                        isSwitchingCycle = false,
                        pendingDelete = null,
                        editing = null,
                    ) ?: s
                }
            } catch (e: Exception) {
                _state.update { s ->
                    (s as? DailyExpensesUiState.Content)?.copy(isSwitchingCycle = false) ?: s
                }
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = try {
                val cycle = cyclesRepository.getCurrent()
                val expenses = dailyExpensesRepository.list(
                    from = cycle.startDate,
                    to = cycle.endDate,
                )
                // Lista de ciclos best-effort — falha aqui não derruba a tela,
                // só deixa setinhas do CycleNavigator inoperantes.
                val cycles = runCatching { cyclesRepository.list() }.getOrDefault(emptyList())
                // Backend já ordena, mas garantimos: mais recentes primeiro.
                DailyExpensesUiState.Content(
                    cycle = cycle,
                    expenses = expenses.sortedByDescending { it.date },
                    cycles = cycles,
                )
            } catch (e: HttpException) {
                if (e.code() == HTTP_NOT_FOUND) {
                    DailyExpensesUiState.NoCycle
                } else {
                    DailyExpensesUiState.Error("Erro ao carregar (HTTP ${e.code()})")
                }
            } catch (e: IOException) {
                DailyExpensesUiState.Error("Sem conexão. Verifique sua internet.")
            } catch (e: Exception) {
                DailyExpensesUiState.Error(e.message ?: "Algo deu errado.")
            }
        }
    }

    private companion object {
        const val HTTP_NOT_FOUND = 404
    }
}
