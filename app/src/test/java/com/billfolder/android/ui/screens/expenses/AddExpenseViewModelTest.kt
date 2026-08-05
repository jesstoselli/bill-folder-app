package com.billfolder.android.ui.screens.expenses

import com.billfolder.android.data.dto.CategoryDto
import com.billfolder.android.data.dto.ExpenseResponse
import com.billfolder.android.data.repository.ExpenseRecurrencesRepository
import com.billfolder.android.data.repository.ExpensesRepository
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

class AddExpenseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = FakeBillFolderApi()
    private val notifier = DataChangeNotifier()
    private val referenceDataRepo = ReferenceDataRepository(api)
    private val expensesRepo = ExpensesRepository(api, notifier)
    private val expenseRecurrencesRepo = ExpenseRecurrencesRepository(api, notifier)

    private fun category(id: String, order: Int) = CategoryDto(
        id = id, key = "key-$id", namePt = "Cat $id", isSystem = true, displayOrder = order,
    )

    private fun expenseResponse(id: String) = ExpenseResponse(
        id = id, dueDate = "2026-06-10", label = "Despesa $id", expectedAmount = 100.0,
        status = "pending", categoryId = "cat1", categoryName = "Cat cat1",
        createdAt = "2026-06-01T00:00:00Z", updatedAt = "2026-06-01T00:00:00Z",
    )

    private fun recurrenceResponse() = com.billfolder.android.data.dto.ExpenseRecurrenceResponse(
        id = "rec-1", defaultLabel = "Aluguel", defaultAmount = 1500.0, defaultCategoryId = "cat1",
        frequency = "monthly", dueDay = 5, weekday = null, startDate = "2026-08-05",
        createdAt = "2026-08-01T00:00:00Z", updatedAt = "2026-08-01T00:00:00Z",
    )

    private fun viewModel() = AddExpenseViewModel(referenceDataRepo, expensesRepo, expenseRecurrencesRepo)

    // ---- init loads reference data --------------------------------------------

    @Test
    fun `init carrega categorias no form state`() {
        api.categories = listOf(category("cat2", 2), category("cat1", 1))

        val state = viewModel().state.value

        assertFalse(state.isLoadingReferences)
        // ordenadas por displayOrder (feito no repo)
        assertEquals(listOf("cat1", "cat2"), state.categories.map { it.id })
    }

    // ---- INVALID submit -------------------------------------------------------

    @Test
    fun `submit com label em branco expoe erro e nao chama o repo`() {
        api.categories = listOf(category("cat1", 1))
        val vm = viewModel()
        vm.onAmountChange("100,00")
        vm.onCategoryChange("cat1")
        // label deixado em branco

        vm.submit("label vazio", "valor inválido", "categoria vazia")

        assertEquals("label vazio", vm.state.value.errorMessage)
        assertTrue(api.createExpenseCalls.isEmpty())
    }

    @Test
    fun `submit com valor zero expoe erro e nao chama o repo`() {
        api.categories = listOf(category("cat1", 1))
        val vm = viewModel()
        vm.onLabelChange("Aluguel")
        vm.onAmountChange("0")
        vm.onCategoryChange("cat1")

        vm.submit("label vazio", "valor inválido", "categoria vazia")

        assertEquals("valor inválido", vm.state.value.errorMessage)
        assertTrue(api.createExpenseCalls.isEmpty())
    }

    @Test
    fun `submit sem categoria expoe erro e nao chama o repo`() {
        api.categories = listOf(category("cat1", 1))
        val vm = viewModel()
        vm.onLabelChange("Aluguel")
        vm.onAmountChange("100,00")
        // categoria não selecionada

        vm.submit("label vazio", "valor inválido", "categoria vazia")

        assertEquals("categoria vazia", vm.state.value.errorMessage)
        assertTrue(api.createExpenseCalls.isEmpty())
    }

    // ---- VALID submit ---------------------------------------------------------

    @Test
    fun `submit valido sem repetir cria despesa unica`() {
        api.categories = listOf(category("cat1", 1))
        api.onCreateExpense = { expenseResponse("new-1") }
        val vm = viewModel()
        vm.onLabelChange("  Aluguel  ")
        vm.onAmountChange("1234,50")
        vm.onCategoryChange("cat1")
        vm.onNotesChange("mensal")
        vm.onRepeatMonthlyChange(false)

        vm.submit("label vazio", "valor inválido", "categoria vazia")

        assertEquals(1, api.createExpenseCalls.size)
        val req = api.createExpenseCalls.first()
        assertEquals("Aluguel", req.label) // trimmed
        assertEquals(1234.50, req.expectedAmount, 0.0001)
        assertEquals("cat1", req.categoryId)
        assertEquals("mensal", req.notes)
        assertTrue(req.dueDate.isNotBlank())

        val state = vm.state.value
        assertTrue(state.savedSuccessfully)
        assertFalse(state.isSaving)
        assertNull(state.errorMessage)
    }

    @Test
    fun `submit com repetir ligado por padrao cria recorrencia mensal`() {
        api.categories = listOf(category("cat1", 1))
        api.onCreateExpenseRecurrence = { recurrenceResponse() }
        val vm = viewModel()
        vm.onLabelChange("Aluguel")
        vm.onAmountChange("1500,00")
        vm.onCategoryChange("cat1")
        vm.onDueDateChange("2026-08-05")
        // repeatMonthly fica no default (true)

        vm.submit("label vazio", "valor inválido", "categoria vazia")

        assertTrue(api.createExpenseCalls.isEmpty())
        assertEquals(1, api.createExpenseRecurrenceCalls.size)
        val req = api.createExpenseRecurrenceCalls.first()
        assertEquals("monthly", req.frequency)
        assertEquals(5, req.dueDay)
        assertNull(req.weekday)
        assertEquals(1500.0, req.defaultAmount, 0.0001)
        assertEquals("2026-08-05", req.startDate)
        assertTrue(vm.state.value.savedSuccessfully)
    }

    // ---- prefill / edit -------------------------------------------------------

    @Test
    fun `prefill preenche os campos e ativa modo edit`() {
        api.categories = listOf(category("cat1", 1))
        val vm = viewModel()

        vm.prefill(
            expenseResponse("exp-42").copy(
                label = "Internet", expectedAmount = 89.9, categoryId = "cat1",
                dueDate = "2026-07-15", notes = "fibra",
            ),
        )

        val state = vm.state.value
        assertEquals("exp-42", state.editingId)
        assertEquals("Internet", state.label)
        assertEquals("cat1", state.selectedCategoryId)
        assertEquals("2026-07-15", state.dueDate)
        assertEquals("fibra", state.notes)
        // amount formatado com vírgula decimal
        assertEquals("89,90", state.amount)
    }
}
