package com.billfolder.android.ui.screens.cards

import com.billfolder.android.data.dto.CardEntryResponse
import com.billfolder.android.data.repository.CardsRepository
import com.billfolder.android.data.sync.DataChangeNotifier
import com.billfolder.android.testutil.FakeBillFolderApi
import com.billfolder.android.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * ViewModel do RepriceSubscriptionSheet — "reprecificar" uma assinatura de
 * cartão. Valida amount > 0 e chama updateSubscriptionAmount com o literal
 * camelCase do escopo (ATENÇÃO: difere do snake_case do delete).
 */
class RepriceSubscriptionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = FakeBillFolderApi()
    private val notifier = DataChangeNotifier()
    private val repo = CardsRepository(api, notifier)

    private fun entry(id: String) = CardEntryResponse(
        id = id, cardId = "c1", cardName = "Cartão", purchaseDate = "2026-06-10",
        label = "Netflix", totalAmount = 39.9, installmentsCount = 1,
        categoryId = "cat", categoryName = "Cat", templateId = "tmpl-1",
        createdAt = "2026-06-10T00:00:00Z", updatedAt = "2026-06-10T00:00:00Z",
    )

    private fun viewModel() = RepriceSubscriptionViewModel(repo)

    @Test
    fun `initializeFor pre-preenche o valor atual`() {
        val vm = viewModel()
        vm.initializeFor(entryId = "e1", currentAmount = 39.9)
        assertEquals("39.9", vm.state.value.amount)
    }

    @Test
    fun `submit com valor invalido nao chama o backend`() {
        val vm = viewModel()
        vm.initializeFor(entryId = "e1", currentAmount = 39.9)
        vm.onAmountChange("0")

        vm.submit(scope = "this", amountInvalidMessage = "inválido")

        assertTrue(api.updateCardSubscriptionAmountCalls.isEmpty())
        assertNotNull(vm.state.value.errorMessage)
    }

    @Test
    fun `submit thread o amount e o literal camelCase thisAndFollowing`() {
        api.onUpdateCardSubscriptionAmount = { _, _ -> entry("e1") }
        val vm = viewModel()
        vm.initializeFor(entryId = "e1", currentAmount = 39.9)
        vm.onAmountChange("49,90")

        vm.submit(scope = "thisAndFollowing", amountInvalidMessage = "inválido")

        assertEquals(1, api.updateCardSubscriptionAmountCalls.size)
        val (id, request) = api.updateCardSubscriptionAmountCalls.first()
        assertEquals("e1", id)
        assertEquals(49.9, request.amount, 0.0001)
        assertEquals("thisAndFollowing", request.scope)
        assertTrue(vm.state.value.savedSuccessfully)
        assertFalse(vm.state.value.isSaving)
    }
}
