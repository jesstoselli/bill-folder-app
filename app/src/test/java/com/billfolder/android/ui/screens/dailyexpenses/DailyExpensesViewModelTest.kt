package com.billfolder.android.ui.screens.dailyexpenses

import com.billfolder.android.data.dto.CycleResponse
import com.billfolder.android.data.dto.DailyExpenseResponse
import com.billfolder.android.data.repository.CyclesRepository
import com.billfolder.android.data.repository.DailyExpensesRepository
import com.billfolder.android.data.sync.DataChangeNotifier
import com.billfolder.android.testutil.FakeBillFolderApi
import com.billfolder.android.testutil.MainDispatcherRule
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class DailyExpensesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = FakeBillFolderApi()
    private val notifier = DataChangeNotifier()
    private val cyclesRepo = CyclesRepository(api, notifier)
    private val dailyRepo = DailyExpensesRepository(api, notifier)

    private fun cycle(id: String, start: String, end: String) = CycleResponse(
        id = id, startDate = start, endDate = end, label = "Ciclo $id",
        isRecurrenceGenerated = false, isCurrent = false,
        createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
    )

    private fun expense(id: String, date: String) = DailyExpenseResponse(
        id = id, date = date, label = "despesa $id", amount = 10.0,
        categoryId = "cat", categoryName = "Cat", accountId = "acc", accountName = "Acc",
        notes = null, createdAt = "2026-06-01T00:00:00Z", updatedAt = "2026-06-01T00:00:00Z",
    )

    private fun http404() = HttpException(Response.error<Any>(404, ResponseBody.create(null, "")))

    private fun viewModel() = DailyExpensesViewModel(cyclesRepo, dailyRepo, notifier)

    @Test
    fun `carrega Content com ciclo e despesas ordenadas por data desc`() {
        val c = cycle("cy1", "2026-06-01", "2026-06-30")
        api.onGetCurrentCycle = { c }
        api.cycles = listOf(c)
        api.dailyExpenses = listOf(expense("e1", "2026-06-05"), expense("e2", "2026-06-20"))

        val state = viewModel().state.value

        assertTrue(state is DailyExpensesUiState.Content)
        state as DailyExpensesUiState.Content
        assertEquals("cy1", state.cycle.id)
        assertEquals(listOf("e2", "e1"), state.expenses.map { it.id })
        assertEquals(1, state.cycles.size)
    }

    @Test
    fun `getCurrent 404 cai em NoCycle`() {
        api.onGetCurrentCycle = { throw http404() }

        assertTrue(viewModel().state.value is DailyExpensesUiState.NoCycle)
    }

    @Test
    fun `IOException cai em Error`() {
        api.onGetCurrentCycle = { throw IOException("offline") }

        assertTrue(viewModel().state.value is DailyExpensesUiState.Error)
    }

    @Test
    fun `pullRefresh atualiza in-place sem virar Loading`() {
        val c = cycle("cy1", "2026-06-01", "2026-06-30")
        api.onGetCurrentCycle = { c }
        api.cycles = listOf(c)
        api.dailyExpenses = listOf(expense("e1", "2026-06-05"))
        val vm = viewModel()
        assertEquals(1, (vm.state.value as DailyExpensesUiState.Content).expenses.size)

        api.dailyExpenses = listOf(expense("e1", "2026-06-05"), expense("e2", "2026-06-06"))
        vm.pullRefresh()

        val state = vm.state.value
        assertTrue(state is DailyExpensesUiState.Content)
        assertEquals(2, (state as DailyExpensesUiState.Content).expenses.size)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `navegacao de ciclo prev-next refetch e troca de ciclo`() {
        val c1 = cycle("cy1", "2026-05-01", "2026-05-31")
        val c2 = cycle("cy2", "2026-06-01", "2026-06-30")
        val c3 = cycle("cy3", "2026-07-01", "2026-07-31")
        api.onGetCurrentCycle = { c2 }
        api.cycles = listOf(c1, c2, c3)
        api.dailyExpenses = listOf(expense("e2", "2026-06-10"))
        val vm = viewModel()

        // PREVIOUS: startDate menor → cy1
        api.dailyExpenses = listOf(expense("e1", "2026-05-10"))
        vm.goToPreviousCycle()
        var content = vm.state.value as DailyExpensesUiState.Content
        assertEquals("cy1", content.cycle.id)
        assertEquals(listOf("e1"), content.expenses.map { it.id })

        // NEXT de volta pra cy2
        api.dailyExpenses = listOf(expense("e2", "2026-06-10"))
        vm.goToNextCycle()
        content = vm.state.value as DailyExpensesUiState.Content
        assertEquals("cy2", content.cycle.id)
    }

    @Test
    fun `navegacao nos extremos e no-op`() {
        val c1 = cycle("cy1", "2026-05-01", "2026-05-31")
        val c2 = cycle("cy2", "2026-06-01", "2026-06-30")
        api.onGetCurrentCycle = { c2 } // c2 é o ultimo (startDate maior)
        api.cycles = listOf(c1, c2)
        api.dailyExpenses = emptyList()
        val vm = viewModel()

        vm.goToNextCycle() // c2 já é extremo superior → no-op

        assertEquals("cy2", (vm.state.value as DailyExpensesUiState.Content).cycle.id)
    }

    @Test
    fun `delete flow remove otimisticamente e chama o backend`() {
        val c = cycle("cy1", "2026-06-01", "2026-06-30")
        api.onGetCurrentCycle = { c }
        api.cycles = listOf(c)
        val target = expense("e1", "2026-06-05")
        api.dailyExpenses = listOf(target, expense("e2", "2026-06-06"))
        val vm = viewModel()

        vm.requestDelete(target)
        assertEquals("e1", (vm.state.value as DailyExpensesUiState.Content).pendingDelete?.id)
        vm.confirmDelete()

        val state = vm.state.value as DailyExpensesUiState.Content
        assertEquals(listOf("e2"), state.expenses.map { it.id })
        assertTrue(api.deletedDailyExpenseIds.contains("e1"))
    }

    @Test
    fun `edit flow abre e fecha o item em edicao`() {
        val c = cycle("cy1", "2026-06-01", "2026-06-30")
        api.onGetCurrentCycle = { c }
        api.cycles = listOf(c)
        val target = expense("e1", "2026-06-05")
        api.dailyExpenses = listOf(target)
        val vm = viewModel()

        vm.requestEdit(target)
        assertEquals("e1", (vm.state.value as DailyExpensesUiState.Content).editing?.id)

        vm.cancelEdit()
        assertEquals(null, (vm.state.value as DailyExpensesUiState.Content).editing)
    }

    @Test
    fun `mudanca de dados global dispara refetch in-place`() {
        val c = cycle("cy1", "2026-06-01", "2026-06-30")
        api.onGetCurrentCycle = { c }
        api.cycles = listOf(c)
        api.dailyExpenses = listOf(expense("e1", "2026-06-05"))
        val vm = viewModel()
        assertEquals(1, (vm.state.value as DailyExpensesUiState.Content).expenses.size)

        api.dailyExpenses = listOf(expense("e1", "2026-06-05"), expense("e2", "2026-06-06"))
        notifier.notifyDataChanged()

        assertEquals(2, (vm.state.value as DailyExpensesUiState.Content).expenses.size)
    }
}
