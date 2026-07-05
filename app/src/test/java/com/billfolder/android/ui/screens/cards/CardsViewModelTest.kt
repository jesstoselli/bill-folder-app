package com.billfolder.android.ui.screens.cards

import androidx.lifecycle.SavedStateHandle
import com.billfolder.android.data.dto.CardEntryResponse
import com.billfolder.android.data.dto.CreditCardAccountResponse
import com.billfolder.android.data.dto.EntryInstallmentDto
import com.billfolder.android.data.repository.CardsRepository
import com.billfolder.android.data.sync.DataChangeNotifier
import com.billfolder.android.testutil.FakeBillFolderApi
import com.billfolder.android.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CardsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = FakeBillFolderApi()
    private val notifier = DataChangeNotifier()
    private val repo = CardsRepository(api, notifier)

    private fun card(id: String) = CreditCardAccountResponse(
        id = id, name = "Cartão $id", closingDay = 17, dueDay = 25,
        createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
    )

    private fun entry(id: String, cardId: String, dueIso: String) = CardEntryResponse(
        id = id, cardId = cardId, cardName = "Cartão $cardId", purchaseDate = "2026-06-10",
        label = "compra $id", totalAmount = 100.0, installmentsCount = 1,
        categoryId = "cat", categoryName = "Cat",
        createdAt = "2026-06-10T00:00:00Z", updatedAt = "2026-06-10T00:00:00Z",
        installments = listOf(
            EntryInstallmentDto(
                installmentId = "inst-$id", installmentNumber = 1, amount = 100.0,
                statementId = "stmt", statementDueDate = dueIso,
            ),
        ),
    )

    private fun viewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) =
        CardsViewModel(savedStateHandle, repo, notifier)

    @Test
    fun `carrega Content com o primeiro cartao selecionado`() {
        api.creditCards = listOf(card("c1"), card("c2"))
        api.cardEntries = listOf(entry("e1", "c1", "2026-06-25"))

        val state = viewModel().state.value

        assertTrue(state is CardsUiState.Content)
        state as CardsUiState.Content
        assertEquals("c1", state.selectedCardId)
        assertEquals(2, state.cards.size)
        assertEquals(1, state.allEntries.size)
    }

    @Test
    fun `sem cartoes cai em NoCards`() {
        api.creditCards = emptyList()

        assertTrue(viewModel().state.value is CardsUiState.NoCards)
    }

    @Test
    fun `deep link cardId seleciona o cartao correspondente`() {
        api.creditCards = listOf(card("c1"), card("c2"))
        api.cardEntries = emptyList()
        val handle = SavedStateHandle(mapOf("cardId" to "c2"))

        val state = viewModel(handle).state.value as CardsUiState.Content

        assertEquals("c2", state.selectedCardId)
    }

    @Test
    fun `onSelectCard troca o cartao selecionado`() {
        api.creditCards = listOf(card("c1"), card("c2"))
        api.cardEntries = emptyList()
        val vm = viewModel()

        vm.onSelectCard("c2")

        assertEquals("c2", (vm.state.value as CardsUiState.Content).selectedCardId)
    }

    @Test
    fun `confirmDelete remove a entry e chama o backend`() {
        api.creditCards = listOf(card("c1"))
        val target = entry("e1", "c1", "2026-06-25")
        api.cardEntries = listOf(target, entry("e2", "c1", "2026-06-25"))
        val vm = viewModel()

        vm.requestDelete(target)
        vm.confirmDelete()

        val state = vm.state.value as CardsUiState.Content
        assertEquals(listOf("e2"), state.allEntries.map { it.id })
        assertTrue(api.deletedCardEntryIds.contains("e1"))
    }

    @Test
    fun `mudanca de dados global dispara refetch in-place`() {
        api.creditCards = listOf(card("c1"))
        api.cardEntries = listOf(entry("e1", "c1", "2026-06-25"))
        val vm = viewModel()
        assertEquals(1, (vm.state.value as CardsUiState.Content).allEntries.size)

        // Simula um write vindo de outra tela: nova lista + bump no bus.
        api.cardEntries = listOf(entry("e1", "c1", "2026-06-25"), entry("e2", "c1", "2026-06-25"))
        notifier.notifyDataChanged()

        assertEquals(2, (vm.state.value as CardsUiState.Content).allEntries.size)
    }

    @Test
    fun `sem parcelas no cartao a navegacao de faturas e no-op`() {
        // Cartão selecionado sem nenhuma parcela → faixa nula → guards bloqueiam
        // ambas as direções, independente da data de hoje (determinístico).
        api.creditCards = listOf(card("c1"))
        api.cardEntries = emptyList()
        val vm = viewModel()
        val before = (vm.state.value as CardsUiState.Content).referencePurchaseDate

        vm.goToNextCycle()
        vm.goToPreviousCycle()

        val content = vm.state.value as CardsUiState.Content
        assertEquals(before, content.referencePurchaseDate)
        assertFalse(content.canGoToNextStatement())
        assertFalse(content.canGoToPreviousStatement())
    }
}
