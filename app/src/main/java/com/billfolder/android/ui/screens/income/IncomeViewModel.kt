package com.billfolder.android.ui.screens.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.CycleResponse
import com.billfolder.android.data.dto.IncomeEntryResponse
import com.billfolder.android.data.dto.IncomeSourceResponse
import com.billfolder.android.data.repository.CyclesRepository
import com.billfolder.android.data.repository.IncomeRepository
import com.billfolder.android.data.sync.DataChangeNotifier
import com.billfolder.android.ui.util.CycleDirection
import com.billfolder.android.ui.util.observeDataChanges
import com.billfolder.android.ui.util.resolveAdjacentCycle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/**
 * Estados da tela "recebimentos".
 *
 * Content carrega 2 datasets paralelos: entries do ciclo + sources
 * recorrentes do usuário (não tem filtro por ciclo nas sources, é
 * config global).
 *
 * Dois flows de swipe paralelos, com nomes distintos pra evitar colisão:
 *  - Entries: pendingDelete / editing / deletingId
 *  - Sources: pendingDeleteSource / editingSource / deletingSourceId
 *
 * Importante:
 *  - Confirm-received (tap em entry expected/late → ConfirmReceivedSheet)
 *    é fluxo separado e continua intacto. Swipe-right de entry edita
 *    campos próprios (sourceId, expectedAmount, expectedDate, notes),
 *    sem mexer em status/actual*.
 *  - Deletar source no backend usa ON DELETE SET NULL — entries históricas
 *    perdem só a referência ao template, não somem.
 */
sealed interface IncomeUiState {
    data object Loading : IncomeUiState
    data object NoCycle : IncomeUiState
    data class Content(
        val cycle: CycleResponse,
        val entries: List<IncomeEntryResponse>,
        val sources: List<IncomeSourceResponse>,
        // Entry flow state
        val pendingDelete: IncomeEntryResponse? = null,
        val editing: IncomeEntryResponse? = null,
        val deletingId: String? = null,
        // Source flow state
        val pendingDeleteSource: IncomeSourceResponse? = null,
        val editingSource: IncomeSourceResponse? = null,
        val deletingSourceId: String? = null,
        // Cycle navigation
        val cycles: List<CycleResponse> = emptyList(),
        val isSwitchingCycle: Boolean = false,
        val isRefreshing: Boolean = false,
    ) : IncomeUiState
    data class Error(val message: String) : IncomeUiState
}

@HiltViewModel
class IncomeViewModel @Inject constructor(
    private val cyclesRepository: CyclesRepository,
    private val incomeRepository: IncomeRepository,
    private val dataChangeNotifier: DataChangeNotifier,
) : ViewModel() {

    private val _state = MutableStateFlow<IncomeUiState>(IncomeUiState.Loading)
    val state: StateFlow<IncomeUiState> = _state.asStateFlow()

    init {
        load()
        observeDataChanges(dataChangeNotifier) { pullRefresh() }
    }

    fun refresh() {
        if (_state.value is IncomeUiState.Loading) return
        _state.value = IncomeUiState.Loading
        load()
    }

    /** Pull-to-refresh: refetch sem apagar a tela. */
    fun pullRefresh() {
        val current = _state.value
        if (current !is IncomeUiState.Content) {
            refresh()
            return
        }
        _state.update { (it as? IncomeUiState.Content)?.copy(isRefreshing = true) ?: it }
        viewModelScope.launch {
            try {
                val cycle = cyclesRepository.getCurrent()
                val (entries, sources) = coroutineScope {
                    val e = async { incomeRepository.listEntries(from = cycle.startDate, to = cycle.endDate) }
                    val s = async { incomeRepository.listSources() }
                    awaitAll(e, s).let {
                        @Suppress("UNCHECKED_CAST")
                        Pair(it[0] as List<IncomeEntryResponse>, it[1] as List<IncomeSourceResponse>)
                    }
                }
                val cycles = runCatching { cyclesRepository.list() }.getOrDefault(emptyList())
                _state.update {
                    (it as? IncomeUiState.Content)?.copy(
                        cycle = cycle,
                        entries = entries.sortedBy { it.expectedDate },
                        sources = sources,
                        cycles = cycles,
                        isRefreshing = false,
                    ) ?: it
                }
            } catch (e: Exception) {
                _state.update { (it as? IncomeUiState.Content)?.copy(isRefreshing = false) ?: it }
            }
        }
    }

    // ------------------------------------------------------------------------
    // Delete flow (swipe-left) — só pra entries; sources entram em outra onda
    // ------------------------------------------------------------------------

    fun requestDelete(item: IncomeEntryResponse) {
        _state.update { current ->
            if (current is IncomeUiState.Content) {
                current.copy(pendingDelete = item)
            } else {
                current
            }
        }
    }

    fun cancelDelete() {
        _state.update { current ->
            if (current is IncomeUiState.Content) {
                current.copy(pendingDelete = null)
            } else {
                current
            }
        }
    }

    fun confirmDelete() {
        val current = _state.value
        if (current !is IncomeUiState.Content) return
        val item = current.pendingDelete ?: return

        _state.update {
            (it as? IncomeUiState.Content)
                ?.copy(pendingDelete = null, deletingId = item.id)
                ?: it
        }

        viewModelScope.launch {
            try {
                incomeRepository.deleteEntry(item.id)
                _state.update { s ->
                    if (s is IncomeUiState.Content) {
                        s.copy(
                            entries = s.entries.filterNot { it.id == item.id },
                            deletingId = null,
                        )
                    } else {
                        s
                    }
                }
            } catch (e: HttpException) {
                _state.value = IncomeUiState.Error("Erro ao deletar (HTTP ${e.code()})")
            } catch (e: IOException) {
                _state.value = IncomeUiState.Error("Sem conexão. Tenta de novo.")
            } catch (e: Exception) {
                _state.value = IncomeUiState.Error(e.message ?: "Algo deu errado.")
            }
        }
    }

    // ------------------------------------------------------------------------
    // Edit flow (swipe-right) — toggles do flag; PATCH é feito pela sheet
    // ------------------------------------------------------------------------

    fun requestEdit(item: IncomeEntryResponse) {
        _state.update { current ->
            if (current is IncomeUiState.Content) {
                current.copy(editing = item)
            } else {
                current
            }
        }
    }

    fun cancelEdit() {
        _state.update { current ->
            if (current is IncomeUiState.Content) {
                current.copy(editing = null)
            } else {
                current
            }
        }
    }

    // ========================================================================
    // Source flows (swipe nas IncomeSourceRow). Names com sufixo "Source" pra
    // evitar colisão com entry flows acima — e dar contexto rápido na call site.
    // ========================================================================

    fun requestDeleteSource(item: IncomeSourceResponse) {
        _state.update { current ->
            if (current is IncomeUiState.Content) {
                current.copy(pendingDeleteSource = item)
            } else {
                current
            }
        }
    }

    fun cancelDeleteSource() {
        _state.update { current ->
            if (current is IncomeUiState.Content) {
                current.copy(pendingDeleteSource = null)
            } else {
                current
            }
        }
    }

    /**
     * User confirmou delete da source. Backend usa ON DELETE SET NULL
     * em income_entries.source_id — entries históricas ficam, perdem só
     * a referência ao template. Atualizamos otimisticamente: removemos
     * a source local e zeramos sourceId/sourceOrigin nas entries que
     * apontavam pra ela (próximo refresh confirma com backend).
     */
    fun confirmDeleteSource() {
        val current = _state.value
        if (current !is IncomeUiState.Content) return
        val source = current.pendingDeleteSource ?: return

        _state.update {
            (it as? IncomeUiState.Content)
                ?.copy(pendingDeleteSource = null, deletingSourceId = source.id)
                ?: it
        }

        viewModelScope.launch {
            try {
                incomeRepository.deleteSource(source.id)
                _state.update { s ->
                    if (s is IncomeUiState.Content) {
                        s.copy(
                            sources = s.sources.filterNot { it.id == source.id },
                            // Reflete o ON DELETE SET NULL local: entries que
                            // apontavam pra essa source perdem o link visual.
                            entries = s.entries.map { entry ->
                                if (entry.sourceId == source.id) {
                                    entry.copy(sourceId = null, sourceOrigin = null)
                                } else {
                                    entry
                                }
                            },
                            deletingSourceId = null,
                        )
                    } else {
                        s
                    }
                }
            } catch (e: HttpException) {
                _state.value = IncomeUiState.Error("Erro ao deletar fonte (HTTP ${e.code()})")
            } catch (e: IOException) {
                _state.value = IncomeUiState.Error("Sem conexão. Tenta de novo.")
            } catch (e: Exception) {
                _state.value = IncomeUiState.Error(e.message ?: "Algo deu errado.")
            }
        }
    }

    fun requestEditSource(item: IncomeSourceResponse) {
        _state.update { current ->
            if (current is IncomeUiState.Content) {
                current.copy(editingSource = item)
            } else {
                current
            }
        }
    }

    fun cancelEditSource() {
        _state.update { current ->
            if (current is IncomeUiState.Content) {
                current.copy(editingSource = null)
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
        val current = _state.value as? IncomeUiState.Content ?: return
        if (current.isSwitchingCycle) return
        val target = resolveAdjacentCycle(current.cycles, current.cycle.id, direction)
            ?: return

        _state.update {
            (it as? IncomeUiState.Content)?.copy(isSwitchingCycle = true) ?: it
        }

        viewModelScope.launch {
            try {
                // Só entries muda de ciclo — sources é config global do user,
                // não janelada por período.
                val entries = incomeRepository.listEntries(
                    from = target.startDate,
                    to = target.endDate,
                )
                _state.update { s ->
                    (s as? IncomeUiState.Content)?.copy(
                        cycle = target,
                        entries = entries.sortedBy { it.expectedDate },
                        isSwitchingCycle = false,
                        pendingDelete = null,
                        editing = null,
                        pendingDeleteSource = null,
                        editingSource = null,
                    ) ?: s
                }
            } catch (e: Exception) {
                _state.update { s ->
                    (s as? IncomeUiState.Content)?.copy(isSwitchingCycle = false) ?: s
                }
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = try {
                val cycle = cyclesRepository.getCurrent()
                // Entries + sources em paralelo — não dependem entre si.
                val (entries, sources) = coroutineScope {
                    val e = async {
                        incomeRepository.listEntries(
                            from = cycle.startDate,
                            to = cycle.endDate,
                        )
                    }
                    val s = async { incomeRepository.listSources() }
                    awaitAll(e, s).let {
                        @Suppress("UNCHECKED_CAST")
                        Pair(it[0] as List<IncomeEntryResponse>, it[1] as List<IncomeSourceResponse>)
                    }
                }
                // Best-effort: falha em listar ciclos não derruba a tela.
                val cycles = runCatching { cyclesRepository.list() }.getOrDefault(emptyList())
                IncomeUiState.Content(
                    cycle = cycle,
                    entries = entries.sortedBy { it.expectedDate },
                    // Mostramos TODAS as sources (ativas + inativas) pro user
                    // poder gerenciar via swipe. O campo isActive é soft-toggle
                    // do backend; até termos UI pra ele, na prática toda source
                    // do user vai estar ativa, então o filter era vestigial.
                    // Quando expor isActive na sheet de edit, vale considerar
                    // visual diferenciado pra inativas.
                    sources = sources,
                    cycles = cycles,
                )
            } catch (e: HttpException) {
                if (e.code() == HTTP_NOT_FOUND) {
                    IncomeUiState.NoCycle
                } else {
                    IncomeUiState.Error("Erro ao carregar (HTTP ${e.code()})")
                }
            } catch (e: IOException) {
                IncomeUiState.Error("Sem conexão. Verifique sua internet.")
            } catch (e: Exception) {
                IncomeUiState.Error(e.message ?: "Algo deu errado.")
            }
        }
    }

    private companion object {
        const val HTTP_NOT_FOUND = 404
    }
}
