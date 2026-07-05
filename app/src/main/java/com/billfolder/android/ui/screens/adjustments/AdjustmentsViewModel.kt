package com.billfolder.android.ui.screens.adjustments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.CycleAdjustmentResponse
import com.billfolder.android.data.dto.CycleAdjustmentTypes
import com.billfolder.android.data.dto.CycleResponse
import com.billfolder.android.data.repository.CycleAdjustmentsRepository
import com.billfolder.android.data.repository.CyclesRepository
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
 * Tela "ajustes do ciclo" — entradas/saídas avulsas do mês.
 *
 * Cases práticos: venda de item usado, saque da poupança pra usar no
 * ciclo, presente eventual, estorno inesperado, dívida esquecida.
 *
 * Filtragem: window pelo ciclo BillFolder atual via `from/to` no backend.
 *
 * Fluxo:
 *  - Loading enquanto resolve ciclo + lista
 *  - NoCycle: user não tem ciclo (não é comum aqui — deveria ser gate
 *    global, mas duplicamos pra consistência com outras telas)
 *  - Content: ok, pode estar vazia (empty state distinto)
 *  - Error: falha rede/servidor, mostra retry
 */
sealed interface AdjustmentsUiState {
    data object Loading : AdjustmentsUiState
    data object NoCycle : AdjustmentsUiState
    data class Content(
        val cycle: CycleResponse,
        val adjustments: List<CycleAdjustmentResponse>,
        val pendingDelete: CycleAdjustmentResponse? = null,
        val editing: CycleAdjustmentResponse? = null,
        val deletingId: String? = null,
        val cycles: List<CycleResponse> = emptyList(),
        val isSwitchingCycle: Boolean = false,
        val isRefreshing: Boolean = false,
    ) : AdjustmentsUiState
    data class Error(val message: String) : AdjustmentsUiState
}

@HiltViewModel
class AdjustmentsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cyclesRepository: CyclesRepository,
    private val adjustmentsRepository: CycleAdjustmentsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<AdjustmentsUiState>(AdjustmentsUiState.Loading)
    val state: StateFlow<AdjustmentsUiState> = _state.asStateFlow()

    init {
        load()
        observeDrawerRefresh(savedStateHandle) { pullRefresh() }
    }

    fun refresh() {
        if (_state.value is AdjustmentsUiState.Loading) return
        _state.value = AdjustmentsUiState.Loading
        load()
    }

    fun pullRefresh() {
        val current = _state.value
        if (current !is AdjustmentsUiState.Content) {
            refresh()
            return
        }
        _state.update { (it as? AdjustmentsUiState.Content)?.copy(isRefreshing = true) ?: it }
        viewModelScope.launch {
            try {
                val cycle = cyclesRepository.getCurrent()
                val list = adjustmentsRepository.list(
                    from = cycle.startDate,
                    to = cycle.endDate,
                )
                val cycles = runCatching { cyclesRepository.list() }.getOrDefault(emptyList())
                _state.update {
                    (it as? AdjustmentsUiState.Content)?.copy(
                        cycle = cycle,
                        adjustments = list.sortedByDescending { it.date },
                        cycles = cycles,
                        isRefreshing = false,
                    ) ?: it
                }
            } catch (e: Exception) {
                _state.update { (it as? AdjustmentsUiState.Content)?.copy(isRefreshing = false) ?: it }
            }
        }
    }

    // ------------------------------------------------------------------------
    // Delete flow (swipe-left)
    // ------------------------------------------------------------------------

    fun requestDelete(item: CycleAdjustmentResponse) {
        _state.update { current ->
            if (current is AdjustmentsUiState.Content) current.copy(pendingDelete = item)
            else current
        }
    }

    fun cancelDelete() {
        _state.update { current ->
            if (current is AdjustmentsUiState.Content) current.copy(pendingDelete = null)
            else current
        }
    }

    fun confirmDelete() {
        val current = _state.value
        if (current !is AdjustmentsUiState.Content) return
        val item = current.pendingDelete ?: return

        _state.update {
            (it as? AdjustmentsUiState.Content)
                ?.copy(pendingDelete = null, deletingId = item.id)
                ?: it
        }

        viewModelScope.launch {
            try {
                adjustmentsRepository.delete(item.id)
                _state.update { s ->
                    if (s is AdjustmentsUiState.Content) {
                        s.copy(
                            adjustments = s.adjustments.filterNot { it.id == item.id },
                            deletingId = null,
                        )
                    } else {
                        s
                    }
                }
            } catch (e: HttpException) {
                _state.value = AdjustmentsUiState.Error("Erro ao deletar (HTTP ${e.code()})")
            } catch (e: IOException) {
                _state.value = AdjustmentsUiState.Error("Sem conexão. Tenta de novo.")
            } catch (e: Exception) {
                _state.value = AdjustmentsUiState.Error(e.message ?: "Algo deu errado.")
            }
        }
    }

    // ------------------------------------------------------------------------
    // Edit flow (swipe-right)
    // ------------------------------------------------------------------------

    fun requestEdit(item: CycleAdjustmentResponse) {
        _state.update { current ->
            if (current is AdjustmentsUiState.Content) current.copy(editing = item)
            else current
        }
    }

    fun cancelEdit() {
        _state.update { current ->
            if (current is AdjustmentsUiState.Content) current.copy(editing = null)
            else current
        }
    }

    // ------------------------------------------------------------------------
    // Cycle navigation (setinhas)
    // ------------------------------------------------------------------------

    fun goToPreviousCycle() = navigate(CycleDirection.PREVIOUS)
    fun goToNextCycle()     = navigate(CycleDirection.NEXT)

    private fun navigate(direction: CycleDirection) {
        val current = _state.value as? AdjustmentsUiState.Content ?: return
        if (current.isSwitchingCycle) return
        val target = resolveAdjacentCycle(current.cycles, current.cycle.id, direction)
            ?: return

        _state.update {
            (it as? AdjustmentsUiState.Content)?.copy(isSwitchingCycle = true) ?: it
        }

        viewModelScope.launch {
            try {
                val list = adjustmentsRepository.list(
                    from = target.startDate,
                    to = target.endDate,
                )
                _state.update { s ->
                    (s as? AdjustmentsUiState.Content)?.copy(
                        cycle = target,
                        adjustments = list.sortedByDescending { it.date },
                        isSwitchingCycle = false,
                        pendingDelete = null,
                        editing = null,
                    ) ?: s
                }
            } catch (e: Exception) {
                _state.update { s ->
                    (s as? AdjustmentsUiState.Content)?.copy(isSwitchingCycle = false) ?: s
                }
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = try {
                val cycle = cyclesRepository.getCurrent()
                val list = adjustmentsRepository.list(
                    from = cycle.startDate,
                    to = cycle.endDate,
                )
                val cycles = runCatching { cyclesRepository.list() }.getOrDefault(emptyList())
                AdjustmentsUiState.Content(
                    cycle = cycle,
                    adjustments = list.sortedByDescending { it.date },
                    cycles = cycles,
                )
            } catch (e: HttpException) {
                if (e.code() == HTTP_NOT_FOUND) {
                    AdjustmentsUiState.NoCycle
                } else {
                    AdjustmentsUiState.Error("Erro ao carregar (HTTP ${e.code()})")
                }
            } catch (e: IOException) {
                AdjustmentsUiState.Error("Sem conexão. Verifique sua internet.")
            } catch (e: Exception) {
                AdjustmentsUiState.Error(e.message ?: "Algo deu errado.")
            }
        }
    }

    private companion object {
        const val HTTP_NOT_FOUND = 404
    }
}

/**
 * Soma com sinal dos ajustes do ciclo:
 *  + inflows
 *  − outflows
 * Útil pra card de total na tela.
 */
fun AdjustmentsUiState.Content.netAmount(): Double =
    adjustments.sumOf { adj ->
        if (adj.type.equals(CycleAdjustmentTypes.INFLOW, ignoreCase = true)) adj.amount
        else -adj.amount
    }
