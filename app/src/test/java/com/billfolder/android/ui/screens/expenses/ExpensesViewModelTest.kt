package com.billfolder.android.ui.screens.expenses

import com.billfolder.android.data.dto.CycleResponse
import com.billfolder.android.data.dto.ExpenseResponse
import com.billfolder.android.data.repository.CyclesRepository
import com.billfolder.android.data.repository.ExpensesRepository
import com.billfolder.android.data.sync.DataChangeNotifier
import com.billfolder.android.testutil.FakeBillFolderApi
import com.billfolder.android.testutil.MainDispatcherRule
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ExpensesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = FakeBillFolderApi()
    private val notifier = DataChangeNotifier()
    private val cyclesRepo = CyclesRepository(api, notifier)
    private val expensesRepo = ExpensesRepository(api, notifier)

    // ---- DTO factories --------------------------------------------------------

    private fun cycle(id: String, start: String, end: String) = CycleResponse(
        id = id, startDate = start, endDate = end, label = "Ciclo $id",
        isRecurrenceGenerated = false, isCurrent = false,
        createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
    )

    private fun expense(id: String, dueDate: String = "2026-06-10", status: String = "pending") =
        ExpenseResponse(
            id = id, dueDate = dueDate, label = "Despesa $id", expectedAmount = 100.0,
            status = status, categoryId = "cat", categoryName = "Cat",
            createdAt = "2026-06-01T00:00:00Z", updatedAt = "2026-06-01T00:00:00Z",
        )

    private fun httpException(code: Int) =
        HttpException(Response.error<Unit>(code, ResponseBody.create(null, "")))

    private fun viewModel() = ExpensesViewModel(cyclesRepo, expensesRepo, notifier)

    // ---- Initial load ---------------------------------------------------------

    @Test
    fun `carga inicial cai em Content com o ciclo e as despesas`() {
        val c = cycle("cy1", "2026-06-01", "2026-06-30")
        api.onGetCurrentCycle = { c }
        api.expenses = listOf(expense("e2", "2026-06-20"), expense("e1", "2026-06-05"))
        api.cycles = listOf(c)

        val state = viewModel().state.value

        assertTrue(state is ExpensesUiState.Content)
        state as ExpensesUiState.Content
        assertEquals("cy1", state.cycle.id)
        // ordenadas por dueDate ascendente
        assertEquals(listOf("e1", "e2"), state.expenses.map { it.id })
    }

    @Test
    fun `sem ciclo aberto (404) cai em NoCycle`() {
        api.onGetCurrentCycle = { throw httpException(404) }

        assertTrue(viewModel().state.value is ExpensesUiState.NoCycle)
    }

    @Test
    fun `HttpException nao-404 no load cai em Error`() {
        api.onGetCurrentCycle = { throw httpException(500) }

        assertTrue(viewModel().state.value is ExpensesUiState.Error)
    }

    @Test
    fun `IOException no load cai em Error`() {
        api.onGetCurrentCycle = { throw IOException("boom") }

        assertTrue(viewModel().state.value is ExpensesUiState.Error)
    }

    // ---- pullRefresh ----------------------------------------------------------

    @Test
    fun `pullRefresh atualiza in-place sem virar Loading`() {
        val c = cycle("cy1", "2026-06-01", "2026-06-30")
        api.onGetCurrentCycle = { c }
        api.expenses = listOf(expense("e1"))
        api.cycles = listOf(c)
        val vm = viewModel()
        assertEquals(1, (vm.state.value as ExpensesUiState.Content).expenses.size)

        api.expenses = listOf(expense("e1"), expense("e2", "2026-06-15"))
        vm.pullRefresh()

        val state = vm.state.value
        assertTrue(state is ExpensesUiState.Content)
        state as ExpensesUiState.Content
        assertEquals(2, state.expenses.size)
        assertFalse(state.isRefreshing)
    }

    // ---- Cycle navigation -----------------------------------------------------

    @Test
    fun `goToNextCycle e goToPreviousCycle trocam o ciclo e refazem a busca`() {
        val c1 = cycle("cy1", "2026-05-01", "2026-05-31")
        val c2 = cycle("cy2", "2026-06-01", "2026-06-30")
        val c3 = cycle("cy3", "2026-07-01", "2026-07-31")
        api.onGetCurrentCycle = { c2 }
        api.expenses = listOf(expense("e-jun", "2026-06-10"))
        api.cycles = listOf(c1, c2, c3)
        val vm = viewModel()
        assertEquals("cy2", (vm.state.value as ExpensesUiState.Content).cycle.id)

        api.expenses = listOf(expense("e-jul", "2026-07-10"))
        vm.goToNextCycle()
        run {
            val s = vm.state.value as ExpensesUiState.Content
            assertEquals("cy3", s.cycle.id)
            assertEquals(listOf("e-jul"), s.expenses.map { it.id })
            assertFalse(s.isSwitchingCycle)
        }

        api.expenses = listOf(expense("e-mai", "2026-05-10"))
        vm.goToPreviousCycle() // cy3 -> cy2
        vm.goToPreviousCycle() // cy2 -> cy1
        assertEquals("cy1", (vm.state.value as ExpensesUiState.Content).cycle.id)
    }

    @Test
    fun `navegacao nos extremos e no-op`() {
        val c1 = cycle("cy1", "2026-05-01", "2026-05-31")
        val c2 = cycle("cy2", "2026-06-01", "2026-06-30")
        api.onGetCurrentCycle = { c1 } // primeiro ciclo
        api.expenses = emptyList()
        api.cycles = listOf(c1, c2)
        val vm = viewModel()

        vm.goToPreviousCycle() // já no primeiro -> no-op

        assertEquals("cy1", (vm.state.value as ExpensesUiState.Content).cycle.id)
    }

    // ---- Delete flow ----------------------------------------------------------

    @Test
    fun `confirmDelete remove a despesa otimisticamente e chama o backend`() {
        val c = cycle("cy1", "2026-06-01", "2026-06-30")
        api.onGetCurrentCycle = { c }
        val target = expense("e1")
        api.expenses = listOf(target, expense("e2", "2026-06-20"))
        api.cycles = listOf(c)
        val vm = viewModel()

        vm.requestDelete(target)
        assertEquals("e1", (vm.state.value as ExpensesUiState.Content).pendingDelete?.id)

        vm.confirmDelete()

        val state = vm.state.value as ExpensesUiState.Content
        assertEquals(listOf("e2"), state.expenses.map { it.id })
        assertNull(state.pendingDelete)
        assertNull(state.deletingId)
        assertTrue(api.deletedExpenseIds.contains("e1"))
    }

    @Test
    fun `cancelDelete limpa o pendingDelete sem deletar`() {
        val c = cycle("cy1", "2026-06-01", "2026-06-30")
        api.onGetCurrentCycle = { c }
        val target = expense("e1")
        api.expenses = listOf(target)
        api.cycles = listOf(c)
        val vm = viewModel()

        vm.requestDelete(target)
        vm.cancelDelete()

        val state = vm.state.value as ExpensesUiState.Content
        assertNull(state.pendingDelete)
        assertEquals(1, state.expenses.size)
        assertTrue(api.deletedExpenseIds.isEmpty())
    }

    // ---- Edit flow (toggles) --------------------------------------------------

    @Test
    fun `requestEdit e cancelEdit alternam o flag editing`() {
        val c = cycle("cy1", "2026-06-01", "2026-06-30")
        api.onGetCurrentCycle = { c }
        val target = expense("e1")
        api.expenses = listOf(target)
        api.cycles = listOf(c)
        val vm = viewModel()

        vm.requestEdit(target)
        assertEquals("e1", (vm.state.value as ExpensesUiState.Content).editing?.id)

        vm.cancelEdit()
        assertNull((vm.state.value as ExpensesUiState.Content).editing)
    }

    // ---- Data-change observer -------------------------------------------------

    @Test
    fun `mudanca de dados global dispara refetch in-place`() {
        val c = cycle("cy1", "2026-06-01", "2026-06-30")
        api.onGetCurrentCycle = { c }
        api.expenses = listOf(expense("e1"))
        api.cycles = listOf(c)
        val vm = viewModel()
        assertEquals(1, (vm.state.value as ExpensesUiState.Content).expenses.size)

        api.expenses = listOf(expense("e1"), expense("e2", "2026-06-20"))
        notifier.notifyDataChanged()

        assertEquals(2, (vm.state.value as ExpensesUiState.Content).expenses.size)
    }
}
