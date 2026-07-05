package com.billfolder.android.ui.screens.expenses

import com.billfolder.android.data.dto.CheckingAccountResponse
import com.billfolder.android.data.dto.ExpenseResponse
import com.billfolder.android.data.dto.UpdateExpenseRequest
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

class PayExpenseViewModelTest {

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

    private fun expenseResponse(id: String) = ExpenseResponse(
        id = id, dueDate = "2026-06-10", label = "conta luz", expectedAmount = 100.0,
        actualAmount = 100.0, status = "paid", paidDate = "2026-06-10",
        paidFromAccountId = null, paidFromAccountName = null,
        categoryId = "cat", categoryName = "Cat",
        linkedCardStatementId = null, notes = null,
        createdAt = "2026-06-01T00:00:00Z", updatedAt = "2026-06-10T00:00:00Z",
    )

    private fun viewModel() = PayExpenseViewModel(referenceRepo, expensesRepo)

    @Test
    fun `initializeFor carrega contas e pre-seleciona a primaria`() {
        api.checkingAccounts = listOf(account("c1"), account("c2", primary = true))
        val vm = viewModel()

        vm.initializeFor(expenseId = "exp1", expectedAmount = 250.0)

        val state = vm.state.value
        assertEquals("exp1", state.expenseId)
        assertEquals(2, state.accounts.size)
        assertEquals("c2", state.selectedAccountId) // primaria
        assertFalse(state.isLoadingReferences)
        assertTrue(state.actualAmount.isNotBlank())
    }

    @Test
    fun `submit com amount invalido nao chama o repo`() {
        api.checkingAccounts = listOf(account("c1", primary = true))
        val vm = viewModel()
        vm.initializeFor(expenseId = "exp1", expectedAmount = 100.0)
        vm.onActualAmountChange("0")

        var called = false
        api.onUpdateExpense = { _, _ -> called = true; expenseResponse("exp1") }
        vm.submit(amountInvalidMessage = "valor invalido")

        assertEquals("valor invalido", vm.state.value.errorMessage)
        assertFalse(vm.state.value.savedSuccessfully)
        assertFalse(called)
    }

    @Test
    fun `submit valido marca como paid via onUpdateExpense`() {
        api.checkingAccounts = listOf(account("c1", primary = true))
        var capturedId: String? = null
        var capturedReq: UpdateExpenseRequest? = null
        api.onUpdateExpense = { id, req ->
            capturedId = id
            capturedReq = req
            expenseResponse(id)
        }
        val vm = viewModel()
        vm.initializeFor(expenseId = "exp1", expectedAmount = 100.0)
        vm.onActualAmountChange("120,00")

        vm.submit(amountInvalidMessage = "valor invalido")

        assertEquals("exp1", capturedId)
        assertNotNull(capturedReq)
        assertEquals("paid", capturedReq!!.status)
        assertEquals(120.0, capturedReq!!.actualAmount!!, 0.0001)
        assertEquals("c1", capturedReq!!.paidFromAccountId)
        assertTrue(capturedReq!!.paidDate!!.isNotBlank())
        assertTrue(vm.state.value.savedSuccessfully)
        assertFalse(vm.state.value.isSaving)
    }

    @Test
    fun `submit sem conta selecionada envia paidFromAccountId null`() {
        api.checkingAccounts = emptyList() // sem contas -> selectedAccountId null
        var capturedReq: UpdateExpenseRequest? = null
        api.onUpdateExpense = { id, req -> capturedReq = req; expenseResponse(id) }
        val vm = viewModel()
        vm.initializeFor(expenseId = "exp1", expectedAmount = 100.0)

        vm.submit(amountInvalidMessage = "valor invalido")

        assertNotNull(capturedReq)
        assertNull(capturedReq!!.paidFromAccountId)
        assertTrue(vm.state.value.savedSuccessfully)
    }
}
