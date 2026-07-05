package com.billfolder.android.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.CycleResponse
import com.billfolder.android.data.dto.HomeResponse
import com.billfolder.android.data.repository.AuthRepository
import com.billfolder.android.data.repository.CyclesRepository
import com.billfolder.android.data.repository.HomeRepository
import com.billfolder.android.data.repository.SavingsRepository
import com.billfolder.android.data.sync.DataChangeNotifier
import com.billfolder.android.ui.util.CycleDirection
import com.billfolder.android.ui.util.observeDataChanges
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
 * Estados da Home. Carrega no init; o usuário pode pedir refresh manual.
 *
 * Nota: optei por NÃO carregar o HomeResponse mapeado pra um "domain model"
 * próprio. Pra UI direta, o DTO tá bom — quando ganhar formatação custom
 * por feature ou virar offline-first com Room, aí extraímos.
 *
 * `hasAnySavingsAccount` é carregado fora do HomeResponse (o agregado do
 * backend não inclui essa info). Usado pra desabilitar o atalho "poupança"
 * do Speed Dial quando o user ainda não cadastrou nenhuma poupança —
 * evita abrir um sheet que ia cair direto em validation error. Falha do
 * fetch (rede etc) NÃO derruba a Home: cai pra default `false`, deixando
 * o atalho disabled (efeito conservador).
 */
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Content(
        val data: HomeResponse,
        val hasAnySavingsAccount: Boolean,
        /**
         * Lista completa de ciclos do user, usada pra resolver prev/next
         * client-side no CycleNavigator. Vem ordenada por startDate asc.
         * Se falha ao carregar (rede etc), fica vazia — setinhas ficam
         * sem efeito, mas a Home renderiza o ciclo atual normalmente.
         */
        val cycles: List<CycleResponse> = emptyList(),
        /**
         * true enquanto refetch de ciclo diferente estiver rolando. UI
         * pode usar pra desabilitar as setinhas e evitar tap-spam. Não
         * troco pro HomeUiState.Loading porque isso apaga a tela inteira.
         */
        val isSwitchingCycle: Boolean = false,
        /**
         * true durante pull-to-refresh. Diferente de switch pra
         * HomeUiState.Loading (que apaga a tela toda), este flag mantém
         * dados visíveis e mostra só o spinner do PullToRefreshBox.
         */
        val isRefreshing: Boolean = false,
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
    /** Ciclo ainda não criado pelo usuário — backend retorna 404/erro específico. */
    data object NoCycle : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val authRepository: AuthRepository,
    private val savingsRepository: SavingsRepository,
    private val cyclesRepository: CyclesRepository,
    private val dataChangeNotifier: DataChangeNotifier,
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        load()
        observeDataChanges(dataChangeNotifier) { pullRefresh() }
    }

    fun refresh() {
        if (_state.value is HomeUiState.Loading) return
        _state.value = HomeUiState.Loading
        load()
    }

    /**
     * Pull-to-refresh — refresca SEM apagar a tela. Se o state já é
     * Content, marca isRefreshing = true, refaz o fetch e no fim desliga
     * o flag. Se o state é Error/NoCycle/Loading, delega pra refresh()
     * normal (troca tudo pra Loading).
     */
    fun pullRefresh() {
        val current = _state.value
        if (current !is HomeUiState.Content) {
            refresh()
            return
        }
        _state.update { (it as? HomeUiState.Content)?.copy(isRefreshing = true) ?: it }
        viewModelScope.launch {
            try {
                val home = homeRepository.getHome()
                val hasSavings = runCatching {
                    savingsRepository.listAccounts().isNotEmpty()
                }.getOrDefault(false)
                val cycles = runCatching { cyclesRepository.list() }.getOrDefault(emptyList())
                _state.update {
                    (it as? HomeUiState.Content)?.copy(
                        data = home,
                        hasAnySavingsAccount = hasSavings,
                        cycles = cycles,
                        isRefreshing = false,
                    ) ?: it
                }
            } catch (e: Exception) {
                // Mantém os dados atuais; só desliga o spinner.
                _state.update { (it as? HomeUiState.Content)?.copy(isRefreshing = false) ?: it }
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }

    /**
     * Setinhas do CycleNavigator. Nada acontece se já estamos no extremo
     * (primeiro/último ciclo do user) ou se ainda não temos a lista de
     * ciclos carregada. Ignoramos taps enquanto isSwitchingCycle = true.
     */
    fun goToPreviousCycle() = navigate(CycleDirection.PREVIOUS)
    fun goToNextCycle()     = navigate(CycleDirection.NEXT)

    private fun navigate(direction: CycleDirection) {
        val current = _state.value as? HomeUiState.Content ?: return
        if (current.isSwitchingCycle) return
        val target = resolveAdjacentCycle(current.cycles, current.data.cycle.id, direction)
            ?: return

        _state.update {
            (it as? HomeUiState.Content)?.copy(isSwitchingCycle = true) ?: it
        }

        viewModelScope.launch {
            try {
                val home = homeRepository.getHome(cycleId = target.id)
                _state.update { s ->
                    (s as? HomeUiState.Content)?.copy(
                        data = home,
                        isSwitchingCycle = false,
                    ) ?: s
                }
            } catch (e: Exception) {
                // Falha silenciosa — reverte o flag mas mantém dados do
                // ciclo atual. Poderia mostrar snackbar; deixamos pra
                // depois pra não introduzir infra nova.
                _state.update { s ->
                    (s as? HomeUiState.Content)?.copy(isSwitchingCycle = false) ?: s
                }
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = try {
                val home = homeRepository.getHome()
                // Best-effort: falha em listAccounts não derruba a Home
                // (apenas mantém o atalho de poupança disabled). Não
                // usamos coroutineScope/async aqui pra que uma exceção
                // do listAccounts não cancele o sucesso já do getHome.
                val hasSavings = runCatching {
                    savingsRepository.listAccounts().isNotEmpty()
                }.getOrDefault(false)
                // Idem: falha ao listar ciclos NÃO derruba a Home; só
                // deixa as setinhas do CycleNavigator inoperantes até a
                // próxima abertura.
                val cycles = runCatching {
                    cyclesRepository.list()
                }.getOrDefault(emptyList())
                HomeUiState.Content(
                    data = home,
                    hasAnySavingsAccount = hasSavings,
                    cycles = cycles,
                )
            } catch (e: HttpException) {
                // Backend retorna 404 quando não há ciclo aberto pra esse usuário.
                if (e.code() == HTTP_NOT_FOUND) {
                    HomeUiState.NoCycle
                } else {
                    HomeUiState.Error("Erro ao carregar (HTTP ${e.code()})")
                }
            } catch (e: IOException) {
                HomeUiState.Error("Sem conexão. Verifique sua internet.")
            } catch (e: Exception) {
                HomeUiState.Error(e.message ?: "Algo deu errado.")
            }
        }
    }

    private companion object {
        const val HTTP_NOT_FOUND = 404
    }
}
