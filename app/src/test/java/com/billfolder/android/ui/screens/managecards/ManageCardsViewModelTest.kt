package com.billfolder.android.ui.screens.managecards

import com.billfolder.android.data.api.BillFolderApi
import com.billfolder.android.data.dto.CreditCardAccountResponse
import com.billfolder.android.data.repository.CardsRepository
import com.billfolder.android.data.sync.DataChangeNotifier
import com.billfolder.android.testutil.FakeBillFolderApi
import com.billfolder.android.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class ManageCardsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = FakeBillFolderApi()
    private val notifier = DataChangeNotifier()
    private val repo = CardsRepository(api, notifier)

    private fun card(id: String) = CreditCardAccountResponse(
        id = id, name = "Cartão $id", closingDay = 17, dueDay = 25,
        createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
    )

    private fun viewModel() = ManageCardsViewModel(repo)

    @Test
    fun `carrega Content com a lista de cartoes`() {
        api.creditCards = listOf(card("c1"), card("c2"))

        val state = viewModel().state.value

        assertTrue(state is ManageCardsUiState.Content)
        state as ManageCardsUiState.Content
        assertEquals(listOf("c1", "c2"), state.cards.map { it.id })
    }

    @Test
    fun `lista vazia cai em Content com lista vazia`() {
        api.creditCards = emptyList()

        val state = viewModel().state.value

        assertTrue(state is ManageCardsUiState.Content)
        assertTrue((state as ManageCardsUiState.Content).cards.isEmpty())
    }

    @Test
    fun `IOException na carga cai em Error`() {
        // getCreditCards é um read de lista sem hook de erro no fake; delegamos
        // tudo pro fake e sobrescrevemos só esse read pra lançar.
        val failingApi = object : BillFolderApi by api {
            override suspend fun getCreditCards(): List<CreditCardAccountResponse> =
                throw IOException("offline")
        }
        val failingRepo = CardsRepository(failingApi, notifier)

        val state = ManageCardsViewModel(failingRepo).state.value

        assertTrue(state is ManageCardsUiState.Error)
    }

    @Test
    fun `delete flow remove otimisticamente e chama o backend`() {
        api.creditCards = listOf(card("c1"), card("c2"))
        val vm = viewModel()

        vm.requestDelete(card("c1"))
        assertEquals("c1", (vm.state.value as ManageCardsUiState.Content).pendingDelete?.id)
        vm.confirmDelete()

        val state = vm.state.value as ManageCardsUiState.Content
        assertEquals(listOf("c2"), state.cards.map { it.id })
        assertNull(state.pendingDelete)
        assertNull(state.deletingId)
        assertTrue(api.deletedCreditCardIds.contains("c1"))
    }

    @Test
    fun `cancelDelete limpa o pending sem deletar`() {
        api.creditCards = listOf(card("c1"))
        val vm = viewModel()

        vm.requestDelete(card("c1"))
        vm.cancelDelete()

        val state = vm.state.value as ManageCardsUiState.Content
        assertNull(state.pendingDelete)
        assertEquals(listOf("c1"), state.cards.map { it.id })
        assertTrue(api.deletedCreditCardIds.isEmpty())
    }

    @Test
    fun `edit flow abre e fecha o item em edicao`() {
        api.creditCards = listOf(card("c1"))
        val vm = viewModel()

        vm.requestEdit(card("c1"))
        assertEquals("c1", (vm.state.value as ManageCardsUiState.Content).editing?.id)

        vm.cancelEdit()
        assertNull((vm.state.value as ManageCardsUiState.Content).editing)
    }
}
