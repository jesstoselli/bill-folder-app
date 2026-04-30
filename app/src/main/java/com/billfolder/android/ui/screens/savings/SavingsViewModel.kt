package com.billfolder.android.ui.screens.savings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.CycleResponse
import com.billfolder.android.data.dto.SavingsAccountResponse
import com.billfolder.android.data.dto.SavingsTransactionResponse
import com.billfolder.android.data.dto.SavingsTransactionTypes
import com.billfolder.android.data.repository.CyclesRepository
import com.billfolder.android.data.repository.SavingsRepository
import com.billfolder.android.ui.navigation.Routes
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
import java.time.LocalDate
import javax.inject.Inject

/**
 * Tela "poupança" (consumo). Mesmo molde de CardsViewModel:
 *  - Carousel horizontal de poupanças cadastradas (chips)
 *  - Lista vertical de movimentações (SavingsTransaction) da poupança
 *    selecionada, filtrada pelo ciclo atual via `date`
 *
 * Fluxo de carregamento:
 *  - Cycle (404 → NoCycle)
 *  - Em paralelo: contas + transações do ciclo
 *  - Sem contas → NoAccounts (estado dedicado pra empty state com CTA
 *    levando pro ManageSavings)
 *
 * O filtro por ciclo é feito no backend via from/to (diferente de Cards,
 * que filtra client-side). Faz sentido aqui porque o domínio tende a
 * acumular MUITAS movimentações por poupança ao longo do tempo
 * (depósito mensal, rendimento mensal etc) — não vale carregar tudo só
 * pra exibir o ciclo atual.
 *
 * O filtro por poupança selecionada é feito client-side: trocar de chip
 * no carousel só muda o `selectedAccountId` e a lista re-renderiza
 * filtrando localmente. Mais responsivo que refazer o request a cada
 * tap. Ainda é uma janela do ciclo, então o volume é baixo.
 *
 * Estados de ação por transação:
 *  - pendingDelete: transaction em swipe-left aguardando confirmação no
 *    AlertDialog
 *  - editing: transaction em swipe-right com sheet de edit aberta
 *  - deletingId: id da transaction sendo deletada (entre confirm e
 *    resposta) — usado pelo SwipeToActionRow pra mostrar "isPending"
 *
 * Observação sobre TransferOut/TransferIn: o read inclui esses tipos
 * normalmente (eles aparecem na lista quando existem), mas a UI de
 * create/edit não os oferece em Fase B. Editar uma transferência via
 * swipe-right cai numa sheet que oferece só Deposit/Withdrawal/Yield —
 * limitação documentada na sheet em Passo 4.
 */
sealed interface SavingsUiState {
    data object Loading : SavingsUiState
    data object NoCycle : SavingsUiState
    data object NoAccounts : SavingsUiState
    data class Content(
        val cycle: CycleResponse,
        val accounts: List<SavingsAccountResponse>,
        val allTransactions: List<SavingsTransactionResponse>,
        val selectedAccountId: String,
        val pendingDelete: SavingsTransactionResponse? = null,
        val editing: SavingsTransactionResponse? = null,
        val deletingId: String? = null,
    ) : SavingsUiState
    data class Error(val message: String) : SavingsUiState
}

@HiltViewModel
class SavingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val savingsRepository: SavingsRepository,
    private val cyclesRepository: CyclesRepository,
) : ViewModel() {

    /**
     * savingsAccountId opcional vindo do NavHost (via "savings?savingsAccountId=...").
     * Quando presente e a poupança existir na lista carregada, é usada
     * como seleção inicial do carousel; senão, fallback pra primeira da
     * lista. Lido só uma vez na construção — refresh não considera (faz
     * sentido, é "deep link" inicial via tap na ManageSavingsScreen, que
     * será wireado no Passo 4).
     */
    private val initialAccountId: String? = savedStateHandle[Routes.SAVINGS_ARG_ID]

    private val _state = MutableStateFlow<SavingsUiState>(SavingsUiState.Loading)
    val state: StateFlow<SavingsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun refresh() {
        if (_state.value is SavingsUiState.Loading) return
        _state.value = SavingsUiState.Loading
        load()
    }

    /** Trocar de poupança no carousel — só muda o ID selecionado. */
    fun onSelectAccount(accountId: String) {
        _state.update { current ->
            if (current is SavingsUiState.Content) {
                current.copy(selectedAccountId = accountId)
            } else {
                current
            }
        }
    }

    // ------------------------------------------------------------------------
    // Delete flow (swipe-left) — só pra SavingsTransaction (movimentação).
    // Deletar a poupança em si (SavingsAccount) é flow do ManageSavingsScreen.
    // ------------------------------------------------------------------------

    fun requestDelete(item: SavingsTransactionResponse) {
        _state.update { current ->
            if (current is SavingsUiState.Content) {
                current.copy(pendingDelete = item)
            } else {
                current
            }
        }
    }

    fun cancelDelete() {
        _state.update { current ->
            if (current is SavingsUiState.Content) {
                current.copy(pendingDelete = null)
            } else {
                current
            }
        }
    }

    fun confirmDelete() {
        val current = _state.value
        if (current !is SavingsUiState.Content) return
        val item = current.pendingDelete ?: return

        _state.update {
            (it as? SavingsUiState.Content)
                ?.copy(pendingDelete = null, deletingId = item.id)
                ?: it
        }

        viewModelScope.launch {
            try {
                savingsRepository.deleteTransaction(item.id)
                // Optimistic-ish: remove a movimentação da lista local. Próximo
                // refresh confirma com o backend de qualquer jeito.
                //
                // Atenção pra TransferOut/TransferIn: deletar uma das pernas
                // não deleta automaticamente a outra (backend trata cada
                // SavingsTransaction como entidade independente; o linked id é
                // só uma referência). Quando a feature de transferência entrar,
                // pode ser necessário deletar as duas no mesmo fluxo — fica
                // documentado pra revisar quando chegar lá.
                _state.update { s ->
                    if (s is SavingsUiState.Content) {
                        s.copy(
                            allTransactions = s.allTransactions.filterNot { it.id == item.id },
                            deletingId = null,
                        )
                    } else {
                        s
                    }
                }
            } catch (e: HttpException) {
                _state.value = SavingsUiState.Error("Erro ao deletar (HTTP ${e.code()})")
            } catch (e: IOException) {
                _state.value = SavingsUiState.Error("Sem conexão. Tenta de novo.")
            } catch (e: Exception) {
                _state.value = SavingsUiState.Error(e.message ?: "Algo deu errado.")
            }
        }
    }

    // ------------------------------------------------------------------------
    // Edit flow (swipe-right) — toggles do flag; PATCH é feito pela sheet.
    // ------------------------------------------------------------------------

    fun requestEdit(item: SavingsTransactionResponse) {
        _state.update { current ->
            if (current is SavingsUiState.Content) {
                current.copy(editing = item)
            } else {
                current
            }
        }
    }

    fun cancelEdit() {
        _state.update { current ->
            if (current is SavingsUiState.Content) {
                current.copy(editing = null)
            } else {
                current
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = try {
                val cycle = cyclesRepository.getCurrent()
                val (accounts, transactions) = coroutineScope {
                    val a = async { savingsRepository.listAccounts() }
                    val t = async {
                        // Filtra do servidor pelo intervalo do ciclo. type=null
                        // pra trazer os 5 tipos (incluindo Transfer*) e renderizar
                        // certinho qualquer movimento que já exista no banco.
                        savingsRepository.listTransactions(
                            savingsAccountId = null,
                            from = cycle.startDate,
                            to = cycle.endDate,
                            type = null,
                        )
                    }
                    awaitAll(a, t).let {
                        @Suppress("UNCHECKED_CAST")
                        Pair(
                            it[0] as List<SavingsAccountResponse>,
                            it[1] as List<SavingsTransactionResponse>,
                        )
                    }
                }

                if (accounts.isEmpty()) {
                    SavingsUiState.NoAccounts
                } else {
                    // Honra deep link "savings?savingsAccountId=...", se
                    // válido. Caso contrário (id ausente ou poupança deletada
                    // entre navegações), cai na primeira da lista.
                    val initialSelected = accounts
                        .firstOrNull { it.id == initialAccountId }
                        ?.id
                        ?: accounts.first().id
                    SavingsUiState.Content(
                        cycle = cycle,
                        accounts = accounts,
                        allTransactions = transactions,
                        selectedAccountId = initialSelected,
                    )
                }
            } catch (e: HttpException) {
                if (e.code() == HTTP_NOT_FOUND) {
                    SavingsUiState.NoCycle
                } else {
                    SavingsUiState.Error("Erro ao carregar (HTTP ${e.code()})")
                }
            } catch (e: IOException) {
                SavingsUiState.Error("Sem conexão. Verifique sua internet.")
            } catch (e: Exception) {
                SavingsUiState.Error(e.message ?: "Algo deu errado.")
            }
        }
    }

    private companion object {
        const val HTTP_NOT_FOUND = 404
    }
}

/**
 * Movimentações da poupança selecionada, ordenadas por data desc (mais
 * recentes primeiro). Filtra também por intervalo do ciclo como cinto-
 * suspensório — o backend já filtrou via from/to, mas é barato re-checar
 * client-side e protege contra eventuais movimentos com data fora do
 * intervalo (ex: edit que mudou data via PATCH).
 */
fun SavingsUiState.Content.transactionsForSelectedAccount(): List<SavingsTransactionResponse> {
    val start = LocalDate.parse(cycle.startDate)
    val end = LocalDate.parse(cycle.endDate)
    return allTransactions
        .filter { it.savingsAccountId == selectedAccountId }
        .filter {
            val d = runCatching { LocalDate.parse(it.date) }.getOrNull()
            d != null && d >= start && d <= end
        }
        .sortedByDescending { it.date }
}

/**
 * Soma com sinal das movimentações da poupança selecionada no ciclo.
 *  + Deposit, Yield, TransferIn
 *  − Withdrawal, TransferOut
 *
 * Útil pra exibir um "movimentado no ciclo: +R$ X" / "−R$ Y" no
 * cabeçalho da tela (Passo 3 vai consumir).
 */
fun SavingsUiState.Content.netFlowForSelectedAccount(): Double =
    transactionsForSelectedAccount().sumOf { tx ->
        if (SavingsTransactionTypes.isInflow(tx.type)) tx.amount else -tx.amount
    }
