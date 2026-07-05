package com.billfolder.android.ui.screens.cards

import com.billfolder.android.data.dto.CreateCreditCardAccountRequest
import com.billfolder.android.data.dto.CreditCardAccountResponse
import com.billfolder.android.data.repository.CardsRepository
import com.billfolder.android.data.sync.DataChangeNotifier
import com.billfolder.android.testutil.FakeBillFolderApi
import com.billfolder.android.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AddCreditCardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = FakeBillFolderApi()
    private val notifier = DataChangeNotifier()
    private val repo = CardsRepository(api, notifier)

    private fun viewModel() = AddCreditCardViewModel(repo)

    private fun response(id: String = "card-1") = CreditCardAccountResponse(
        id = id, name = "Nubank", issuerBank = "Nubank", brand = "Mastercard",
        closingDay = 10, dueDay = 17,
        createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
    )

    @Test
    fun `submit com nome em branco falha validacao e nao chama create`() {
        var captured: CreateCreditCardAccountRequest? = null
        api.onCreateCreditCard = { captured = it; response() }
        val vm = viewModel()
        vm.onNameChange("")
        vm.onClosingDayChange("10")
        vm.onDueDayChange("17")

        vm.submit("nome vazio", "fechamento inválido", "vencimento inválido")

        val state = vm.state.value
        assertEquals("nome vazio", state.errorMessage)
        assertFalse(state.savedSuccessfully)
        assertNull(captured)
    }

    @Test
    fun `submit com closingDay invalido falha validacao e nao chama create`() {
        var captured: CreateCreditCardAccountRequest? = null
        api.onCreateCreditCard = { captured = it; response() }
        val vm = viewModel()
        vm.onNameChange("Nubank")
        vm.onClosingDayChange("40")
        vm.onDueDayChange("17")

        vm.submit("nome vazio", "fechamento inválido", "vencimento inválido")

        assertEquals("fechamento inválido", vm.state.value.errorMessage)
        assertNull(captured)
    }

    @Test
    fun `submit com dueDay invalido falha validacao e nao chama create`() {
        var captured: CreateCreditCardAccountRequest? = null
        api.onCreateCreditCard = { captured = it; response() }
        val vm = viewModel()
        vm.onNameChange("Nubank")
        vm.onClosingDayChange("10")
        vm.onDueDayChange("0")

        vm.submit("nome vazio", "fechamento inválido", "vencimento inválido")

        assertEquals("vencimento inválido", vm.state.value.errorMessage)
        assertNull(captured)
    }

    @Test
    fun `submit valido chama create com os valores corretos e seta sucesso`() {
        var captured: CreateCreditCardAccountRequest? = null
        api.onCreateCreditCard = { captured = it; response() }
        val vm = viewModel()
        vm.onNameChange("  Nubank  ")
        vm.onIssuerBankChange("Nu Pagamentos")
        vm.onBrandChange("Mastercard")
        vm.onClosingDayChange("10")
        vm.onDueDayChange("17")

        vm.submit("nome vazio", "fechamento inválido", "vencimento inválido")

        assertNotNull(captured)
        assertEquals("Nubank", captured!!.name)
        assertEquals("Nu Pagamentos", captured!!.issuerBank)
        assertEquals("Mastercard", captured!!.brand)
        assertEquals(10, captured!!.closingDay)
        assertEquals(17, captured!!.dueDay)
        val state = vm.state.value
        assertTrue(state.savedSuccessfully)
        assertFalse(state.isSaving)
        assertNull(state.errorMessage)
    }

    @Test
    fun `campos opcionais em branco viram null no request`() {
        var captured: CreateCreditCardAccountRequest? = null
        api.onCreateCreditCard = { captured = it; response() }
        val vm = viewModel()
        vm.onNameChange("Cartão")
        vm.onClosingDayChange("5")
        vm.onDueDayChange("12")

        vm.submit("nome vazio", "fechamento inválido", "vencimento inválido")

        assertNull(captured!!.issuerBank)
        assertNull(captured!!.brand)
    }

    @Test
    fun `prefill popula o form em modo edit`() {
        val vm = viewModel()

        vm.prefill(response(id = "card-9"))

        val state = vm.state.value
        assertEquals("card-9", state.editingId)
        assertEquals("Nubank", state.name)
        assertEquals("Nubank", state.issuerBank)
        assertEquals("Mastercard", state.brand)
        assertEquals("10", state.closingDay)
        assertEquals("17", state.dueDay)
    }
}
