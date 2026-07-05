package com.billfolder.android.ui.screens.dailyexpenses

import com.billfolder.android.data.dto.CategoryDto
import com.billfolder.android.data.dto.CheckingAccountResponse
import com.billfolder.android.data.dto.DailyExpenseResponse
import com.billfolder.android.data.repository.DailyExpensesRepository
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

class AddDailyExpenseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = FakeBillFolderApi()
    private val notifier = DataChangeNotifier()
    private val referenceDataRepo = ReferenceDataRepository(api)
    private val dailyExpensesRepo = DailyExpensesRepository(api, notifier)

    private fun category(id: String, order: Int) = CategoryDto(
        id = id, key = "key-$id", namePt = "Cat $id", isSystem = true, displayOrder = order,
    )

    private fun account(id: String, primary: Boolean) = CheckingAccountResponse(
        id = id, bankName = "Banco $id", initialBalance = 0.0, isPrimary = primary,
        createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
    )

    private fun dailyExpense(id: String) = DailyExpenseResponse(
        id = id, date = "2026-06-10", label = "Mercado $id", amount = 50.0,
        categoryId = "cat1", categoryName = "Cat cat1",
        accountId = "acc1", accountName = "Banco acc1",
        createdAt = "2026-06-10T00:00:00Z", updatedAt = "2026-06-10T00:00:00Z",
    )

    private fun viewModel() = AddDailyExpenseViewModel(referenceDataRepo, dailyExpensesRepo)

    private fun AddDailyExpenseViewModel.submitAll() =
        submit("label vazio", "valor inválido", "categoria vazia", "conta vazia")

    // ---- init loads reference data --------------------------------------------

    @Test
    fun `init carrega categorias e contas e pre-seleciona a conta primaria`() {
        api.categories = listOf(category("cat2", 2), category("cat1", 1))
        // acc2 é a primária; repo ordena primária primeiro
        api.checkingAccounts = listOf(account("acc1", false), account("acc2", true))

        val state = viewModel().state.value

        assertFalse(state.isLoadingReferences)
        assertEquals(listOf("cat1", "cat2"), state.categories.map { it.id })
        assertEquals("acc2", state.selectedAccountId)
    }

    // ---- INVALID submit -------------------------------------------------------

    @Test
    fun `submit com label em branco expoe erro e nao chama o repo`() {
        api.categories = listOf(category("cat1", 1))
        api.checkingAccounts = listOf(account("acc1", true))
        val vm = viewModel()
        vm.onAmountChange("50,00")
        vm.onCategoryChange("cat1")

        vm.submitAll()

        assertEquals("label vazio", vm.state.value.errorMessage)
        assertTrue(api.createDailyExpenseCalls.isEmpty())
    }

    @Test
    fun `submit com valor zero expoe erro e nao chama o repo`() {
        api.categories = listOf(category("cat1", 1))
        api.checkingAccounts = listOf(account("acc1", true))
        val vm = viewModel()
        vm.onLabelChange("Padaria")
        vm.onAmountChange("0")
        vm.onCategoryChange("cat1")

        vm.submitAll()

        assertEquals("valor inválido", vm.state.value.errorMessage)
        assertTrue(api.createDailyExpenseCalls.isEmpty())
    }

    // ---- VALID submit ---------------------------------------------------------

    @Test
    fun `submit valido chama o repo e sinaliza sucesso`() {
        api.categories = listOf(category("cat1", 1))
        api.checkingAccounts = listOf(account("acc1", true))
        api.onCreateDailyExpense = { dailyExpense("new-1") }
        val vm = viewModel()
        vm.onLabelChange("  Padaria  ")
        vm.onAmountChange("42,90")
        vm.onCategoryChange("cat1")
        // conta primária acc1 já pré-selecionada
        vm.onNotesChange("café")

        vm.submitAll()

        assertEquals(1, api.createDailyExpenseCalls.size)
        val req = api.createDailyExpenseCalls.first()
        assertEquals("Padaria", req.label)
        assertEquals(42.90, req.amount, 0.0001)
        assertEquals("cat1", req.categoryId)
        assertEquals("acc1", req.accountId)
        assertEquals("café", req.notes)
        assertTrue(req.date.isNotBlank())

        val state = vm.state.value
        assertTrue(state.savedSuccessfully)
        assertFalse(state.isSaving)
        assertNull(state.errorMessage)
    }

    // ---- prefill / edit -------------------------------------------------------

    @Test
    fun `prefill preenche os campos e ativa modo edit`() {
        api.categories = listOf(category("cat1", 1))
        api.checkingAccounts = listOf(account("acc1", true), account("acc2", false))
        val vm = viewModel()

        vm.prefill(
            dailyExpense("de-7").copy(
                label = "Farmácia", amount = 33.5, categoryId = "cat1",
                accountId = "acc2", date = "2026-07-02", notes = "remédio",
            ),
        )

        val state = vm.state.value
        assertEquals("de-7", state.editingId)
        assertEquals("Farmácia", state.label)
        assertEquals("cat1", state.selectedCategoryId)
        assertEquals("acc2", state.selectedAccountId)
        assertEquals("2026-07-02", state.date)
        assertEquals("remédio", state.notes)
        assertEquals("33,50", state.amount)
    }
}
