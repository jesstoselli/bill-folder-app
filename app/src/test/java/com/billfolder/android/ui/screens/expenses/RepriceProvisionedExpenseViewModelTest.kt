package com.billfolder.android.ui.screens.expenses

import com.billfolder.android.data.dto.ExpenseResponse
import com.billfolder.android.data.repository.ExpensesRepository
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
 * ViewModel do RepriceProvisionedExpenseSheet — reajustar o valor POR SESSÃO
 * (occurrenceAmount) de uma despesa provisionada. Valida amount > 0 e chama
 * repriceProvisioned com o literal camelCase do escopo (ATENÇÃO: difere do
 * snake_case do delete).
 */
class RepriceProvisionedExpenseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = FakeBillFolderApi()
    private val notifier = DataChangeNotifier()
    private val repo = ExpensesRepository(api, notifier)

    private fun expense(id: String) = ExpenseResponse(
        id = id, dueDate = "2026-06-10", label = "Fisioterapia",
        expectedAmount = 600.0, status = "pending",
        categoryId = "cat", categoryName = "Saúde",
        occurrenceAmount = 150.0, occurrencesTotal = 4, occurrencesPaid = 0,
        paidToDate = 0.0,
        createdAt = "2026-06-10T00:00:00Z", updatedAt = "2026-06-10T00:00:00Z",
    )

    private fun viewModel() = RepriceProvisionedExpenseViewModel(repo)

    @Test
    fun `initializeFor pre-preenche o valor atual por sessao`() {
        val vm = viewModel()
        vm.initializeFor(expenseId = "e1", currentAmount = 150.0)
        assertEquals("150", vm.state.value.amount)
    }

    @Test
    fun `submit com valor invalido nao chama o backend`() {
        val vm = viewModel()
        vm.initializeFor(expenseId = "e1", currentAmount = 150.0)
        vm.onAmountChange("0")

        vm.submit(scope = "this", amountInvalidMessage = "inválido")

        assertTrue(api.repriceProvisionedExpenseCalls.isEmpty())
        assertNotNull(vm.state.value.errorMessage)
    }

    @Test
    fun `submit thread o amount e o literal camelCase thisAndFollowing`() {
        api.onRepriceProvisionedExpense = { _, _ -> expense("e1") }
        val vm = viewModel()
        vm.initializeFor(expenseId = "e1", currentAmount = 150.0)
        vm.onAmountChange("175,50")

        vm.submit(scope = "thisAndFollowing", amountInvalidMessage = "inválido")

        assertEquals(1, api.repriceProvisionedExpenseCalls.size)
        val (id, request) = api.repriceProvisionedExpenseCalls.first()
        assertEquals("e1", id)
        assertEquals(175.5, request.amount, 0.0001)
        assertEquals("thisAndFollowing", request.scope)
        assertTrue(vm.state.value.savedSuccessfully)
        assertFalse(vm.state.value.isSaving)
    }
}
