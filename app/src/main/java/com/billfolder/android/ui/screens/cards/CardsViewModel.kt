package com.billfolder.android.ui.screens.cards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.CardEntryResponse
import com.billfolder.android.data.dto.CreditCardAccountResponse
import com.billfolder.android.data.dto.EntryInstallmentDto
import com.billfolder.android.data.repository.CardsRepository
import com.billfolder.android.data.sync.DataChangeNotifier
import com.billfolder.android.ui.navigation.Routes
import com.billfolder.android.ui.util.StatementPeriod
import com.billfolder.android.ui.util.computeStatementForPurchase
import com.billfolder.android.ui.util.observeDataChanges
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
 *    filtrada pela FATURA daquele cartão (não pelo ciclo BillFolder)
 *
 * Cada cartão tem seu próprio ciclo (fechamento → vencimento), diferente
 * do ciclo mensal do BillFolder. Um cartão que fecha dia 17 tem faturas
 * cobrindo (18/mês N-1 → 17/mês N), enquanto o ciclo BillFolder é
 * (01/mês → último dia do mês). Por isso essa tela NÃO usa CycleResponse
 * — ela navega entre faturas do cartão via `referencePurchaseDate`, uma
 * data-âncora que representa qual fatura está sendo visualizada.
 *
 * `referencePurchaseDate` inicial é hoje. Ao setar prev/next, shift ±1 mês
 * na âncora, e o `computeStatementForPurchase` recalcula os limites da
 * fatura com o closingDay/dueDay do cartão. Trocar de cartão no carousel
 * mantém a mesma âncora — a fatura recomputa pros parâmetros do novo
 * cartão, então o user "muda de cartão" mas continua olhando pra fatura
 * de julho, por exemplo.
 *
 * Cartões + entries carregadas em paralelo. Entries carrega TUDO
 * (`cardsRepository.listEntries()` sem from/to) e filtramos client-side
 * porque o volume por usuário é baixo (dezenas/centenas de compras) e
 * permite navegar entre faturas sem re-fetch.
 *
 * Estados de ação por entry:
 *  - pendingDelete: entry em swipe-left aguardando confirmação
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
    data object NoCards : CardsUiState
    data class Content(
        val cards: List<CreditCardAccountResponse>,
        val allEntries: List<CardEntryResponse>,
        val selectedCardId: String,
        /**
         * Data-âncora dentro da fatura atualmente visualizada. Inicia como
         * hoje, e prev/next shiftam ±1 mês. Combinada com closingDay/dueDay
         * do cartão selecionado, resolve deterministicamente qual fatura
         * está sendo vista via `computeStatementForPurchase`.
         */
        val referencePurchaseDate: LocalDate,
        val pendingDelete: CardEntryResponse? = null,
        val editing: CardEntryResponse? = null,
        val deletingId: String? = null,
        val isRefreshing: Boolean = false,
    ) : CardsUiState
    data class Error(val message: String) : CardsUiState
}

@HiltViewModel
class CardsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cardsRepository: CardsRepository,
    private val dataChangeNotifier: DataChangeNotifier,
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
        observeDataChanges(dataChangeNotifier) { pullRefresh() }
    }

    fun refresh() {
        if (_state.value is CardsUiState.Loading) return
        _state.value = CardsUiState.Loading
        load()
    }

    /** Pull-to-refresh: refetch cards+entries sem apagar a tela. */
    fun pullRefresh() {
        val current = _state.value
        if (current !is CardsUiState.Content) {
            refresh()
            return
        }
        _state.update { (it as? CardsUiState.Content)?.copy(isRefreshing = true) ?: it }
        viewModelScope.launch {
            try {
                val (cards, entries) = coroutineScope {
                    val c = async { cardsRepository.listCards() }
                    val e = async { cardsRepository.listEntries() }
                    awaitAll(c, e).let {
                        @Suppress("UNCHECKED_CAST")
                        Pair(it[0] as List<CreditCardAccountResponse>, it[1] as List<CardEntryResponse>)
                    }
                }
                _state.update {
                    (it as? CardsUiState.Content)?.copy(
                        cards = cards,
                        allEntries = entries,
                        isRefreshing = false,
                    ) ?: it
                }
            } catch (e: Exception) {
                _state.update { (it as? CardsUiState.Content)?.copy(isRefreshing = false) ?: it }
            }
        }
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

    // ------------------------------------------------------------------------
    // Fatura anterior/próxima (setinhas do CycleNavigator).
    //
    // Diferente das outras telas, aqui NÃO navegamos entre ciclos BillFolder
    // — navegamos entre FATURAS do cartão selecionado. Shift ±1 mês na
    // referencePurchaseDate; o statement é recomputado no render usando o
    // closingDay/dueDay do cartão via computeStatementForPurchase.
    //
    // Filtro e header ambos usam o mesmo statement, então shift na âncora
    // basta — nada de refetch, sem estado adicional.
    // ------------------------------------------------------------------------

    fun goToPreviousCycle() = shiftReferenceDate(months = -1L)
    fun goToNextCycle()     = shiftReferenceDate(months = 1L)

    private fun shiftReferenceDate(months: Long) {
        _state.update { s ->
            (s as? CardsUiState.Content)?.copy(
                referencePurchaseDate = s.referencePurchaseDate.plusMonths(months),
                pendingDelete = null,
                editing = null,
            ) ?: s
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = try {
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
                        cards = cards,
                        allEntries = entries,
                        selectedCardId = initialSelected,
                        referencePurchaseDate = LocalDate.now(),
                    )
                }
            } catch (e: HttpException) {
                CardsUiState.Error("Erro ao carregar (HTTP ${e.code()})")
            } catch (e: IOException) {
                CardsUiState.Error("Sem conexão. Verifique sua internet.")
            } catch (e: Exception) {
                CardsUiState.Error(e.message ?: "Algo deu errado.")
            }
        }
    }
}

/**
 * Statement (fatura) do cartão selecionado, computado a partir da data-
 * âncora do estado. Se não houver cartão selecionado válido, retorna null.
 */
fun CardsUiState.Content.currentStatement(): StatementPeriod? {
    val card = cards.firstOrNull { it.id == selectedCardId } ?: return null
    return computeStatementForPurchase(
        purchaseDate = referencePurchaseDate,
        closingDay = card.closingDay,
        dueDay = card.dueDay,
    )
}

/**
 * Row de parcela na fatura — achata a relação (entry → installments) num
 * item de UI plano. Cada parcela vira uma row separada nas faturas
 * onde ela vence.
 *
 * Ex: compra "Karoline Miranda R$ 330,76 em 4x" com purchaseDate = 29/abr
 * e cartão fechando dia 17 gera 4 rows:
 *  - maio/2026:  Karoline Miranda (1/4) R$ 82,69
 *  - junho/2026: Karoline Miranda (2/4) R$ 82,69
 *  - julho/2026: Karoline Miranda (3/4) R$ 82,69
 *  - agosto/2026: Karoline Miranda (4/4) R$ 82,69
 *
 * entryId aponta pro CardEntry pai — usado pelo swipe-to-delete/edit,
 * porque o backend deleta a compra INTEIRA (e todas as installments
 * associadas). Editar 1 parcela isolada também não faz sentido.
 */
data class CardInstallmentDisplay(
    val entryId: String,
    val installmentId: String,
    val cardId: String,
    val label: String,
    val categoryName: String,
    val purchaseDate: String,
    val amount: Double,
    val installmentNumber: Int,
    val installmentsCount: Int,
)

/**
 * Parcelas do cartão selecionado que vencem na FATURA atualmente vista.
 *
 * Semântica: "quais parcelas estão nessa fatura?" — flat map das entries
 * do cartão pras suas installments, filtrando pelo `statementDueDate`
 * bater com a `dueDate` da fatura sendo vista.
 *
 * Backend é source of truth: cada `EntryInstallmentDto` já tem
 * `statementDueDate` calculada pelo servidor (baseada no closingDay/
 * dueDay do cartão no momento em que a compra foi criada). Não
 * recomputamos — só matcheamos.
 *
 * Comparação por string ISO ("yyyy-MM-dd") é segura porque ambos os
 * lados usam a mesma representação DateOnly.
 */
fun CardsUiState.Content.installmentsForSelectedStatement(): List<CardInstallmentDisplay> {
    val statement = currentStatement() ?: return emptyList()
    val statementDueIso = statement.dueDate.toString()

    return allEntries
        .asSequence()
        .filter { it.cardId == selectedCardId }
        .flatMap { entry -> entry.installments.map { entry to it } }
        .filter { (_, inst) -> inst.statementDueDate == statementDueIso }
        .map { (entry, inst) -> entry.toInstallmentDisplay(inst) }
        // Ordem: parcelas mais antigas (por purchaseDate) primeiro dentro
        // da mesma fatura. Se duas parcelas caem no mesmo dia (mesma compra),
        // ordenamos por installmentNumber pra estabilidade visual.
        .sortedWith(
            compareByDescending<CardInstallmentDisplay> { it.purchaseDate }
                .thenBy { it.installmentNumber },
        )
        .toList()
}

private fun CardEntryResponse.toInstallmentDisplay(
    installment: EntryInstallmentDto,
): CardInstallmentDisplay = CardInstallmentDisplay(
    entryId = id,
    installmentId = installment.installmentId,
    cardId = cardId,
    label = label,
    categoryName = categoryName,
    purchaseDate = purchaseDate,
    amount = installment.amount,
    installmentNumber = installment.installmentNumber,
    installmentsCount = installmentsCount,
)
