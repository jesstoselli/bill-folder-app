package com.billfolder.android.ui.screens.dailyexpenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.CycleResponse
import com.billfolder.android.data.dto.DailyExpenseResponse
import com.billfolder.android.data.repository.CyclesRepository
import com.billfolder.android.data.repository.DailyExpensesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 */
sealed interface DailyExpensesUiState {
    data object Loading : DailyExpensesUiState
    data object NoCycle : DailyExpensesUiState
    data class Content(
        val cycle: CycleResponse,
        val expenses: List<DailyExpenseResponse>,
    ) : DailyExpensesUiState
    data class Error(val message: String) : DailyExpensesUiState
}

@HiltViewModel
class DailyExpensesViewModel @Inject constructor(
    private val cyclesRepository: CyclesRepository,
    private val dailyExpensesRepository: DailyExpensesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<DailyExpensesUiState>(DailyExpensesUiState.Loading)
    val state: StateFlow<DailyExpensesUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun refresh() {
        if (_state.value is DailyExpensesUiState.Loading) return
        _state.value = DailyExpensesUiState.Loading
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = try {
                val cycle = cyclesRepository.getCurrent()
                val expenses = dailyExpensesRepository.list(
                    from = cycle.startDate,
                    to = cycle.endDate,
                )
                // Backend já ordena, mas garantimos: mais recentes primeiro.
                DailyExpensesUiState.Content(
                    cycle = cycle,
                    expenses = expenses.sortedByDescending { it.date },
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
