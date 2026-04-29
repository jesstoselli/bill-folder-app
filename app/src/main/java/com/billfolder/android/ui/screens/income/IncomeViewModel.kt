package com.billfolder.android.ui.screens.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.CycleResponse
import com.billfolder.android.data.dto.IncomeEntryResponse
import com.billfolder.android.data.dto.IncomeSourceResponse
import com.billfolder.android.data.repository.CyclesRepository
import com.billfolder.android.data.repository.IncomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 */
sealed interface IncomeUiState {
    data object Loading : IncomeUiState
    data object NoCycle : IncomeUiState
    data class Content(
        val cycle: CycleResponse,
        val entries: List<IncomeEntryResponse>,
        val sources: List<IncomeSourceResponse>,
    ) : IncomeUiState
    data class Error(val message: String) : IncomeUiState
}

@HiltViewModel
class IncomeViewModel @Inject constructor(
    private val cyclesRepository: CyclesRepository,
    private val incomeRepository: IncomeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<IncomeUiState>(IncomeUiState.Loading)
    val state: StateFlow<IncomeUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun refresh() {
        if (_state.value is IncomeUiState.Loading) return
        _state.value = IncomeUiState.Loading
        load()
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
                IncomeUiState.Content(
                    cycle = cycle,
                    entries = entries.sortedBy { it.expectedDate },
                    sources = sources.filter { it.isActive },
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
