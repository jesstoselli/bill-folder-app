package com.billfolder.android.ui.screens.expenses

import com.billfolder.android.data.dto.CheckingAccountResponse
import com.billfolder.android.data.dto.ExpenseResponse
import com.billfolder.android.data.dto.PayOccurrenceRequest
import com.billfolder.android.data.repository.ExpensesRepository
import com.billfolder.android.data.repository.ReferenceDataRepository
import com.billfolder.android.data.sync.DataChangeNotifier
import com.billfolder.android.testutil.FakeBillFolderApi
import com.billfolder.android.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PayOccurrenceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = FakeBillFolderApi()
    private val notifier = DataChangeNotifier()
    private val referenceRepo = ReferenceDataRepository(api)
    private val expensesRepo = ExpensesRepository(api, notifier)

    private fun account(id: String, primary: Boolean = false) = CheckingAccountResponse(
        id = id, bankName = "Banco $id", branch = "0001", accountNumber = "123-4",
        initialBalance = 0.0, isPrimary = primary,
        createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
    )

    private fun provisionedResponse(id: String) = ExpenseResponse(
        id = id, dueDate = "2026-07-10", label = "fisio", expectedAmount = 400.0,
        status = "pending", categoryId = "cat", categoryName = "Saúde",
        occurrenceAmount = 100.0, occurrencesTotal = 4, occurrencesPaid = 2, paidToDate = 200.0,
        createdAt = "2026-07-01T00:00:00Z", updatedAt = "2026-07-10T00:00:00Z",
    )

    private fun viewModel() = PayOccurrenceViewModel(referenceRepo, expensesRepo)

    @Test
    fun `initializeFor carrega contas, pre-seleciona a primaria e pre-preenche o valor`() {
        api.checkingAccounts = listOf(account("c1"), account("c2", primary = true))
        val vm = viewModel()

        vm.initializeFor(expenseId = "exp1", occurrenceAmount = 100.0)

        val state = vm.state.value
        assertEquals("exp1", state.expenseId)
        assertEquals("c2", state.selectedAccountId) // primaria
        assertFalse(state.isLoadingReferences)
        assertTrue(state.amount.isNotBlank())
    }

    @Test
    fun `submit com amount invalido nao chama o repo`() {
        api.checkingAccounts = listOf(account("c1", primary = true))
        val vm = viewModel()
        vm.initializeFor(expenseId = "exp1", occurrenceAmount = 100.0)
        vm.onAmountChange("0")

        vm.submit(amountInvalidMessage = "valor invalido")

        assertEquals("valor invalido", vm.state.value.errorMessage)
        assertFalse(vm.state.value.savedSuccessfully)
        assertTrue(api.payOccurrenceCalls.isEmpty())
    }

    @Test
    fun `submit valido chama payOccurrence com o valor e a data corretos`() {
        api.checkingAccounts = listOf(account("c1", primary = true))
        api.onPayOccurrence = { id, _ -> provisionedResponse(id) }
        val vm = viewModel()
        vm.initializeFor(expenseId = "exp1", occurrenceAmount = 100.0)
        vm.onAmountChange("120,00")

        vm.submit(amountInvalidMessage = "valor invalido")

        assertEquals(1, api.payOccurrenceCalls.size)
        val (id, req) = api.payOccurrenceCalls.first()
        assertEquals("exp1", id)
        assertEquals(120.0, req.amount!!, 0.0001)
        assertEquals("c1", req.paidFromAccountId)
        assertNotNull(req.paidDate)
        assertTrue(req.paidDate!!.isNotBlank())
        assertTrue(vm.state.value.savedSuccessfully)
        assertFalse(vm.state.value.isSaving)
    }

    @Test
    fun `submit sem conta selecionada envia paidFromAccountId null`() {
        api.checkingAccounts = emptyList() // sem contas -> selectedAccountId null
        api.onPayOccurrence = { id, _ -> provisionedResponse(id) }
        val vm = viewModel()
        vm.initializeFor(expenseId = "exp1", occurrenceAmount = 100.0)

        vm.submit(amountInvalidMessage = "valor invalido")

        assertEquals(1, api.payOccurrenceCalls.size)
        val (_, req) = api.payOccurrenceCalls.first()
        assertNull(req.paidFromAccountId)
        assertTrue(vm.state.value.savedSuccessfully)
    }

    // Sanity: garantir que o request é do tipo certo (não markPaid).
    @Test
    fun `submit usa PayOccurrenceRequest`() {
        api.checkingAccounts = listOf(account("c1", primary = true))
        var captured: PayOccurrenceRequest? = null
        api.onPayOccurrence = { id, req -> captured = req; provisionedResponse(id) }
        val vm = viewModel()
        vm.initializeFor(expenseId = "exp1", occurrenceAmount = 100.0)

        vm.submit(amountInvalidMessage = "valor invalido")

        assertNotNull(captured)
    }
}
