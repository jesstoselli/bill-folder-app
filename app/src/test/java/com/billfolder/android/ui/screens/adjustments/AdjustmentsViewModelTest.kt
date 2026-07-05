package com.billfolder.android.ui.screens.adjustments

import com.billfolder.android.data.dto.CycleAdjustmentResponse
import com.billfolder.android.data.dto.CycleAdjustmentTypes
import com.billfolder.android.data.dto.CycleResponse
import com.billfolder.android.data.repository.CycleAdjustmentsRepository
import com.billfolder.android.data.repository.CyclesRepository
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

class AdjustmentsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = FakeBillFolderApi()
    private val notifier = DataChangeNotifier()
    private val cyclesRepo = CyclesRepository(api, notifier)
    private val adjustmentsRepo = CycleAdjustmentsRepository(api, notifier)

    // ------------------------------------------------------------------------
    // Factory helpers (determinísticos — sem LocalDate.now())
    // ------------------------------------------------------------------------

    private fun cycle(id: String, startDate: String) = CycleResponse(
        id = id,
        startDate = startDate,
        endDate = startDate.dropLast(2) + "28",
        label = "Ciclo $id",
        isRecurrenceGenerated = false,
        isCurrent = false,
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z",
    )

    private fun adjustment(
        id: String,
        date: String,
        type: String = CycleAdjustmentTypes.INFLOW,
        amount: Double = 100.0,
    ) = CycleAdjustmentResponse(
        id = id,
        type = type,
        label = "ajuste $id",
        amount = amount,
        date = date,
        sourceSavingsTransactionId = null,
        createdAt = "2026-06-01T00:00:00Z",
        updatedAt = "2026-06-01T00:00:00Z",
    )

    private fun http404() = HttpException(Response.error<Any>(404, ResponseBody.create(null, "")))

    private fun http500() = HttpException(Response.error<Any>(500, ResponseBody.create(null, "")))

    private fun viewModel() = AdjustmentsViewModel(cyclesRepo, adjustmentsRepo, notifier)

    // ------------------------------------------------------------------------
    // Initial load
    // ------------------------------------------------------------------------

    @Test
    fun `carrega Content com os ajustes ordenados por data desc`() {
        api.onGetCurrentCycle = { cycle("c1", "2026-06-01") }
        api.cycleAdjustments = listOf(
            adjustment("a1", "2026-06-05"),
            adjustment("a2", "2026-06-20"),
            adjustment("a3", "2026-06-10"),
        )
        api.cycles = listOf(cycle("c1", "2026-06-01"))

        val state = viewModel().state.value

        assertTrue(state is AdjustmentsUiState.Content)
        state as AdjustmentsUiState.Content
        assertEquals("c1", state.cycle.id)
        assertEquals(listOf("a2", "a3", "a1"), state.adjustments.map { it.id })
        assertEquals(1, state.cycles.size)
        assertFalse(state.isRefreshing)
    }

    // ------------------------------------------------------------------------
    // NoCycle / Error paths
    // ------------------------------------------------------------------------

    @Test
    fun `ciclo 404 cai em NoCycle`() {
        api.onGetCurrentCycle = { throw http404() }

        assertTrue(viewModel().state.value is AdjustmentsUiState.NoCycle)
    }

    @Test
    fun `ciclo HTTP nao-404 cai em Error`() {
        api.onGetCurrentCycle = { throw http500() }

        assertTrue(viewModel().state.value is AdjustmentsUiState.Error)
    }

    @Test
    fun `ciclo IOException cai em Error`() {
        api.onGetCurrentCycle = { throw IOException("offline") }

        assertTrue(viewModel().state.value is AdjustmentsUiState.Error)
    }

    // ------------------------------------------------------------------------
    // pullRefresh
    // ------------------------------------------------------------------------

    @Test
    fun `pullRefresh atualiza in-place sem virar Loading`() {
        api.onGetCurrentCycle = { cycle("c1", "2026-06-01") }
        api.cycleAdjustments = listOf(adjustment("a1", "2026-06-05"))
        api.cycles = listOf(cycle("c1", "2026-06-01"))
        val vm = viewModel()
        assertEquals(1, (vm.state.value as AdjustmentsUiState.Content).adjustments.size)

        api.cycleAdjustments = listOf(
            adjustment("a1", "2026-06-05"),
            adjustment("a2", "2026-06-06"),
        )
        vm.pullRefresh()

        val state = vm.state.value
        assertTrue(state is AdjustmentsUiState.Content)
        state as AdjustmentsUiState.Content
        assertEquals(2, state.adjustments.size)
        assertFalse(state.isRefreshing)
    }

    // ------------------------------------------------------------------------
    // Delete flow
    // ------------------------------------------------------------------------

    @Test
    fun `requestDelete marca pendingDelete e confirmDelete remove chamando o backend`() {
        api.onGetCurrentCycle = { cycle("c1", "2026-06-01") }
        val target = adjustment("a1", "2026-06-05")
        api.cycleAdjustments = listOf(target, adjustment("a2", "2026-06-06"))
        api.cycles = listOf(cycle("c1", "2026-06-01"))
        val vm = viewModel()

        vm.requestDelete(target)
        assertEquals("a1", (vm.state.value as AdjustmentsUiState.Content).pendingDelete?.id)

        vm.confirmDelete()

        val state = vm.state.value as AdjustmentsUiState.Content
        assertEquals(listOf("a2"), state.adjustments.map { it.id })
        assertNull(state.pendingDelete)
        assertNull(state.deletingId)
        assertTrue(api.deletedCycleAdjustmentIds.contains("a1"))
    }

    @Test
    fun `cancelDelete limpa pendingDelete sem chamar o backend`() {
        api.onGetCurrentCycle = { cycle("c1", "2026-06-01") }
        val target = adjustment("a1", "2026-06-05")
        api.cycleAdjustments = listOf(target)
        api.cycles = listOf(cycle("c1", "2026-06-01"))
        val vm = viewModel()

        vm.requestDelete(target)
        vm.cancelDelete()

        val state = vm.state.value as AdjustmentsUiState.Content
        assertNull(state.pendingDelete)
        assertEquals(1, state.adjustments.size)
        assertTrue(api.deletedCycleAdjustmentIds.isEmpty())
    }

    // ------------------------------------------------------------------------
    // Edit flow
    // ------------------------------------------------------------------------

    @Test
    fun `requestEdit e cancelEdit alternam o item em edicao`() {
        api.onGetCurrentCycle = { cycle("c1", "2026-06-01") }
        val target = adjustment("a1", "2026-06-05")
        api.cycleAdjustments = listOf(target)
        api.cycles = listOf(cycle("c1", "2026-06-01"))
        val vm = viewModel()

        vm.requestEdit(target)
        assertEquals("a1", (vm.state.value as AdjustmentsUiState.Content).editing?.id)

        vm.cancelEdit()
        assertNull((vm.state.value as AdjustmentsUiState.Content).editing)
    }

    // ------------------------------------------------------------------------
    // Cycle navigation
    // ------------------------------------------------------------------------

    @Test
    fun `goToPreviousCycle e goToNextCycle refazem fetch e trocam o ciclo`() {
        api.onGetCurrentCycle = { cycle("c2", "2026-06-01") }
        api.cycleAdjustments = emptyList()
        api.cycles = listOf(
            cycle("c1", "2026-05-01"),
            cycle("c2", "2026-06-01"),
            cycle("c3", "2026-07-01"),
        )
        val vm = viewModel()
        assertEquals("c2", (vm.state.value as AdjustmentsUiState.Content).cycle.id)

        vm.goToPreviousCycle()
        assertEquals("c1", (vm.state.value as AdjustmentsUiState.Content).cycle.id)

        vm.goToNextCycle()
        assertEquals("c2", (vm.state.value as AdjustmentsUiState.Content).cycle.id)

        vm.goToNextCycle()
        assertEquals("c3", (vm.state.value as AdjustmentsUiState.Content).cycle.id)
    }

    @Test
    fun `navegacao no extremo e no-op`() {
        api.onGetCurrentCycle = { cycle("c1", "2026-05-01") }
        api.cycleAdjustments = emptyList()
        api.cycles = listOf(
            cycle("c1", "2026-05-01"),
            cycle("c2", "2026-06-01"),
        )
        val vm = viewModel()
        assertEquals("c1", (vm.state.value as AdjustmentsUiState.Content).cycle.id)

        vm.goToPreviousCycle()

        assertEquals("c1", (vm.state.value as AdjustmentsUiState.Content).cycle.id)
    }

    // ------------------------------------------------------------------------
    // Data-change observer
    // ------------------------------------------------------------------------

    @Test
    fun `mudanca de dados global dispara refetch in-place`() {
        api.onGetCurrentCycle = { cycle("c1", "2026-06-01") }
        api.cycleAdjustments = listOf(adjustment("a1", "2026-06-05"))
        api.cycles = listOf(cycle("c1", "2026-06-01"))
        val vm = viewModel()
        assertEquals(1, (vm.state.value as AdjustmentsUiState.Content).adjustments.size)

        api.cycleAdjustments = listOf(
            adjustment("a1", "2026-06-05"),
            adjustment("a2", "2026-06-06"),
        )
        notifier.notifyDataChanged()

        assertEquals(2, (vm.state.value as AdjustmentsUiState.Content).adjustments.size)
    }

    // ------------------------------------------------------------------------
    // netAmount extension
    // ------------------------------------------------------------------------

    @Test
    fun `netAmount soma inflows e subtrai outflows`() {
        api.onGetCurrentCycle = { cycle("c1", "2026-06-01") }
        api.cycleAdjustments = listOf(
            adjustment("a1", "2026-06-05", type = CycleAdjustmentTypes.INFLOW, amount = 300.0),
            adjustment("a2", "2026-06-06", type = CycleAdjustmentTypes.OUTFLOW, amount = 100.0),
        )
        api.cycles = listOf(cycle("c1", "2026-06-01"))

        val state = viewModel().state.value as AdjustmentsUiState.Content

        assertEquals(200.0, state.netAmount(), 0.0001)
    }
}
