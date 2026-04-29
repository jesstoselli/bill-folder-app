package com.billfolder.android.ui.screens.cards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.CardEntryResponse
import com.billfolder.android.data.dto.CreditCardAccountResponse
import com.billfolder.android.data.dto.CycleResponse
import com.billfolder.android.data.repository.CardsRepository
import com.billfolder.android.data.repository.CyclesRepository
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
 * Tela "despesas no cartão" (consumo). Mostra:
 *  - Carousel horizontal de cartões cadastrados (chips)
 *  - Lista vertical de compras (CardEntry) do cartão selecionado,
 *    filtrada pelo ciclo atual via purchaseDate
 *
 * Ciclo é necessário porque "compras desse cartão" só faz sentido
 * janelado num período. Se não houver ciclo, mostra NoCycle.
 *
 * Cartões + entries carregadas em paralelo. Trocar de cartão no carousel
 * só atualiza o `selectedCardId` no estado — entries já estão todas
 * carregadas e filtramos local.
 *
 * Estados de ação por entry:
 *  - pendingDelete: entry em swipe-left aguardando confirmação no AlertDialog
 *  - editing: entry em swipe-right com sheet de edit aberta
 *  - deletingId: id da entry sendo deletada (entre confirm e resposta)
 *
 * Cascata de delete: deletar uma entry parcelada remove TODAS as
 * installments associadas e recalcula statements futuros — o backend
 * lida com isso. CardsScreen não mostra statements (só compras), então
 * a remoção otimística local da entry é suficiente. Outras telas (Home)
 * vão refletir as mudanças no próximo refresh.
 */
sealed interface CardsUiState {
    data object Loading : CardsUiState
    data object NoCycle : CardsUiState
    data object NoCards : CardsUiState
    data class Content(
        val cycle: CycleResponse,
        val cards: List<CreditCardAccountResponse>,
        val allEntries: List<CardEntryResponse>,
        val selectedCardId: String,
        val pendingDelete: CardEntryResponse? = null,
        val editing: CardEntryResponse? = null,
        val deletingId: String? = null,
    ) : CardsUiState
    data class Error(val message: String) : CardsUiState
}

@HiltViewModel
class CardsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cardsRepository: CardsRepository,
    private val cyclesRepository: CyclesRepository,
) : ViewModel() {

    /**
     * cardId opcional vindo do NavHost (via "cards?cardId=..."). Quando
     * presente e o cartão existir na lista carregada, é usado como
     * seleção inicial do carousel; senão, fallback pro primeiro cartão.
     * Lido só uma vez na construção — refresh não considera (faz sentido,
     * é "deep link" inicial).
     */
    private val initialCardId: String? = savedStateHandle[Routes.CARDS_ARG_ID]

    private val _state = MutableStateFlow<CardsUiState>(CardsUiState.Loading)
    val state: StateFlow<CardsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun refresh() {
        if (_state.value is CardsUiState.Loading) return
        _state.value = CardsUiState.Loading
        load()
    }

    /** Trocar de cartão no carousel — só muda o ID selecionado. */
    fun onSelectCard(cardId: String) {
        _state.update { current ->
            if (current is CardsUiState.Content) {
                current.copy(selectedCardId = cardId)
            } else {
                current
            }
        }
    }

    // ------------------------------------------------------------------------
    // Delete flow (swipe-left) — só pra CardEntries (compras).
    // Deletar cartão (CreditCardAccount) é flow do ManageCardsScreen.
    // ------------------------------------------------------------------------

    fun requestDelete(item: CardEntryResponse) {
        _state.update { current ->
            if (current is CardsUiState.Content) {
                current.copy(pendingDelete = item)
            } else {
                current
            }
        }
    }

    fun cancelDelete() {
        _state.update { current ->
            if (current is CardsUiState.Content) {
                current.copy(pendingDelete = null)
            } else {
                current
            }
        }
    }

    fun confirmDelete() {
        val current = _state.value
        if (current !is CardsUiState.Content) return
        val item = current.pendingDelete ?: return

        _state.update {
            (it as? CardsUiState.Content)
                ?.copy(pendingDelete = null, deletingId = item.id)
                ?: it
        }

        viewModelScope.launch {
            try {
                cardsRepository.deleteEntry(item.id)
                _state.update { s ->
                    if (s is CardsUiState.Content) {
                        s.copy(
                            allEntries = s.allEntries.filterNot { it.id == item.id },
                            deletingId = null,
                        )
                    } else {
                        s
                    }
                }
            } catch (e: HttpException) {
                _state.value = CardsUiState.Error("Erro ao deletar (HTTP ${e.code()})")
            } catch (e: IOException) {
                _state.value = CardsUiState.Error("Sem conexão. Tenta de novo.")
            } catch (e: Exception) {
                _state.value = CardsUiState.Error(e.message ?: "Algo deu errado.")
            }
        }
    }

    // ------------------------------------------------------------------------
    // Edit flow (swipe-right) — toggles do flag; PATCH é feito pela sheet.
    // ------------------------------------------------------------------------

    fun requestEdit(item: CardEntryResponse) {
        _state.update { current ->
            if (current is CardsUiState.Content) {
                current.copy(editing = item)
            } else {
                current
            }
        }
    }

    fun cancelEdit() {
        _state.update { current ->
            if (current is CardsUiState.Content) {
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
                val (cards, entries) = coroutineScope {
                    val c = async { cardsRepository.listCards() }
                    val e = async { cardsRepository.listEntries() }
                    awaitAll(c, e).let {
                        @Suppress("UNCHECKED_CAST")
                        Pair(it[0] as List<CreditCardAccountResponse>, it[1] as List<CardEntryResponse>)
                    }
                }

                if (cards.isEmpty()) {
                    CardsUiState.NoCards
                } else {
                    // Honra deep link "cards?cardId=...", se válido. Caso
                    // contrário (id ausente ou cartão deletado entre
                    // navegações), cai no primeiro cartão da lista.
                    val initialSelected = cards
                        .firstOrNull { it.id == initialCardId }
                        ?.id
                        ?: cards.first().id
                    CardsUiState.Content(
                        cycle = cycle,
                        cards = cards,
                        allEntries = entries,
                        selectedCardId = initialSelected,
                    )
                }
            } catch (e: HttpException) {
                if (e.code() == HTTP_NOT_FOUND) {
                    CardsUiState.NoCycle
                } else {
                    CardsUiState.Error("Erro ao carregar (HTTP ${e.code()})")
                }
            } catch (e: IOException) {
                CardsUiState.Error("Sem conexão. Verifique sua internet.")
            } catch (e: Exception) {
                CardsUiState.Error(e.message ?: "Algo deu errado.")
            }
        }
    }

    private companion object {
        const val HTTP_NOT_FOUND = 404
    }
}

/**
 * Filtra entries do cartão selecionado E dentro do ciclo atual.
 * Critério: purchaseDate dentro de [cycle.startDate, cycle.endDate].
 *
 * Nota: parcelamento. Uma compra de R$ 600 em 6x feita no ciclo atual
 * aparece UMA VEZ aqui (a entry), mesmo que as parcelas se distribuam
 * em 6 statements futuros. Faz sentido pra "compras feitas com esse
 * cartão nesse mês".
 */
fun CardsUiState.Content.entriesForSelectedCard(): List<CardEntryResponse> {
    val start = LocalDate.parse(cycle.startDate)
    val end = LocalDate.parse(cycle.endDate)
    return allEntries
        .filter { it.cardId == selectedCardId }
        .filter {
            val date = runCatching { LocalDate.parse(it.purchaseDate) }.getOrNull()
            date != null && date >= start && date <= end
        }
        .sortedByDescending { it.purchaseDate }
}
