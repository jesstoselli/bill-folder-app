package com.billfolder.android.ui.screens.cards

import com.billfolder.android.data.dto.CardEntryResponse
import com.billfolder.android.data.dto.CategoryDto
import com.billfolder.android.data.dto.CreditCardAccountResponse
import com.billfolder.android.data.repository.CardsRepository
import com.billfolder.android.data.repository.ReferenceDataRepository
import com.billfolder.android.data.sync.DataChangeNotifier
import com.billfolder.android.testutil.FakeBillFolderApi
import com.billfolder.android.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AddCardEntryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = FakeBillFolderApi()
    private val notifier = DataChangeNotifier()
    private val cardsRepo = CardsRepository(api, notifier)
    private val referenceDataRepo = ReferenceDataRepository(api)

    private fun category(id: String, order: Int) = CategoryDto(
        id = id, key = "key-$id", namePt = "Cat $id", isSystem = true, displayOrder = order,
    )

    private fun card(id: String) = CreditCardAccountResponse(
        id = id, name = "Cartão $id", closingDay = 17, dueDay = 25,
        createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
    )

    private fun cardEntry(id: String) = CardEntryResponse(
        id = id, cardId = "c1", cardName = "Cartão c1", purchaseDate = "2026-06-10",
        label = "compra $id", totalAmount = 300.0, installmentsCount = 3,
        categoryId = "cat1", categoryName = "Cat cat1",
        createdAt = "2026-06-10T00:00:00Z", updatedAt = "2026-06-10T00:00:00Z",
    )

    private fun viewModel() = AddCardEntryViewModel(cardsRepo, referenceDataRepo)

    private val msgs = arrayOf(
        "label vazio", "valor inválido", "cartão vazio", "parcelas inválidas", "categoria vazia",
    )
    private fun AddCardEntryViewModel.submitAll() =
        submit(msgs[0], msgs[1], msgs[2], msgs[3], msgs[4])

    // ---- init loads reference data --------------------------------------------

    @Test
    fun `init carrega cartoes e categorias e pre-seleciona o primeiro cartao`() {
        api.creditCards = listOf(card("c1"), card("c2"))
        api.categories = listOf(category("cat2", 2), category("cat1", 1))

        val state = viewModel().state.value

        assertFalse(state.isLoadingReferences)
        assertEquals(listOf("c1", "c2"), state.cards.map { it.id })
        assertEquals(listOf("cat1", "cat2"), state.categories.map { it.id })
        assertEquals("c1", state.selectedCardId)
    }

    // ---- INVALID submit -------------------------------------------------------

    @Test
    fun `submit com label em branco expoe erro e nao chama o repo`() {
        api.creditCards = listOf(card("c1"))
        api.categories = listOf(category("cat1", 1))
        val vm = viewModel()
        vm.onTotalAmountChange("300,00")
        vm.onCategoryChange("cat1")
        // label em branco

        vm.submitAll()

        assertEquals("label vazio", vm.state.value.errorMessage)
        assertTrue(api.createCardEntryCalls.isEmpty())
    }

    @Test
    fun `submit com parcelas invalidas expoe erro e nao chama o repo`() {
        api.creditCards = listOf(card("c1"))
        api.categories = listOf(category("cat1", 1))
        val vm = viewModel()
        vm.onLabelChange("Notebook")
        vm.onTotalAmountChange("300,00")
        vm.onCategoryChange("cat1")
        vm.onInstallmentsChange("0")

        vm.submitAll()

        assertEquals("parcelas inválidas", vm.state.value.errorMessage)
        assertTrue(api.createCardEntryCalls.isEmpty())
    }

    // ---- VALID submit ---------------------------------------------------------

    @Test
    fun `submit valido chama o repo e sinaliza sucesso`() {
        api.creditCards = listOf(card("c1"))
        api.categories = listOf(category("cat1", 1))
        api.onCreateCardEntry = { cardEntry("new-1") }
        val vm = viewModel()
        vm.onLabelChange("  Notebook  ")
        vm.onTotalAmountChange("1500,00")
        vm.onInstallmentsChange("3")
        vm.onCategoryChange("cat1")
        vm.onNotesChange("dell")

        vm.submitAll()

        assertEquals(1, api.createCardEntryCalls.size)
        val req = api.createCardEntryCalls.first()
        assertEquals("c1", req.cardId)
        assertEquals("Notebook", req.label)
        assertEquals(1500.0, req.totalAmount, 0.0001)
        assertEquals(3, req.installmentsCount)
        assertEquals("cat1", req.categoryId)
        assertEquals("dell", req.notes)
        assertTrue(req.purchaseDate.isNotBlank())

        val state = vm.state.value
        assertTrue(state.savedSuccessfully)
        assertFalse(state.isSaving)
        assertNull(state.errorMessage)
    }

    // ---- prefill / edit -------------------------------------------------------

    @Test
    fun `prefill preenche os campos e ativa modo edit`() {
        api.creditCards = listOf(card("c1"))
        api.categories = listOf(category("cat1", 1))
        val vm = viewModel()

        vm.prefill(
            cardEntry("entry-9").copy(
                label = "TV", totalAmount = 2400.0, installmentsCount = 12,
                cardId = "c1", categoryId = "cat1", purchaseDate = "2026-05-01", notes = "sala",
            ),
        )

        val state = vm.state.value
        assertEquals("entry-9", state.editingId)
        assertEquals("TV", state.label)
        assertEquals("12", state.installmentsCount)
        assertEquals("c1", state.selectedCardId)
        assertEquals("cat1", state.selectedCategoryId)
        assertEquals("2026-05-01", state.purchaseDate)
        assertEquals("sala", state.notes)
        assertEquals("2400,00", state.totalAmount)
    }

    @Test
    fun `submit em modo edit valida so label e categoria e chama updateEntry`() {
        api.creditCards = listOf(card("c1"))
        api.categories = listOf(category("cat1", 1))
        var updatedId: String? = null
        api.onUpdateCardEntry = { id, _ -> updatedId = id; cardEntry(id) }
        val vm = viewModel()
        vm.prefill(cardEntry("entry-9").copy(label = "TV", categoryId = "cat1"))
        vm.onLabelChange("TV nova")

        vm.submitAll()

        // Nenhum create — edit vai por updateEntry
        assertTrue(api.createCardEntryCalls.isEmpty())
        assertEquals("entry-9", updatedId)
        assertTrue(vm.state.value.savedSuccessfully)
    }
}
