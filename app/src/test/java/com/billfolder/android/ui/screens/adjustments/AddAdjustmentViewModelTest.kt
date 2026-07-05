package com.billfolder.android.ui.screens.adjustments

import com.billfolder.android.data.dto.CycleAdjustmentResponse
import com.billfolder.android.data.dto.CycleAdjustmentTypes
import com.billfolder.android.data.repository.CycleAdjustmentsRepository
import com.billfolder.android.data.sync.DataChangeNotifier
import com.billfolder.android.testutil.FakeBillFolderApi
import com.billfolder.android.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AddAdjustmentViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = FakeBillFolderApi()
    private val notifier = DataChangeNotifier()
    private val repo = CycleAdjustmentsRepository(api, notifier)

    private fun response(id: String) = CycleAdjustmentResponse(
        id = id, type = CycleAdjustmentTypes.INFLOW, label = "venda",
        amount = 100.0, date = "2026-06-10", sourceSavingsTransactionId = null,
        createdAt = "2026-06-10T00:00:00Z", updatedAt = "2026-06-10T00:00:00Z",
    )

    private fun viewModel() = AddAdjustmentViewModel(repo)

    @Test
    fun `submit com label em branco nao chama o repo`() {
        val vm = viewModel()
        vm.onAmountChange("10")
        // label default "" -> invalido

        vm.submit(labelEmptyMessage = "label obrigatoria", amountInvalidMessage = "valor invalido")

        assertEquals("label obrigatoria", vm.state.value.errorMessage)
        assertFalse(vm.state.value.savedSuccessfully)
        assertTrue(api.createCycleAdjustmentCalls.isEmpty())
    }

    @Test
    fun `submit com amount zero nao chama o repo`() {
        val vm = viewModel()
        vm.onLabelChange("venda bike")
        vm.onAmountChange("0")

        vm.submit(labelEmptyMessage = "label obrigatoria", amountInvalidMessage = "valor invalido")

        assertEquals("valor invalido", vm.state.value.errorMessage)
        assertTrue(api.createCycleAdjustmentCalls.isEmpty())
    }

    @Test
    fun `submit valido chama create com o payload correto`() {
        api.onCreateCycleAdjustment = { response("adj1") }
        val vm = viewModel()
        vm.onTypeChange(CycleAdjustmentTypes.OUTFLOW)
        vm.onLabelChange("  presente sogro  ")
        vm.onAmountChange("42,90")

        vm.submit(labelEmptyMessage = "label obrigatoria", amountInvalidMessage = "valor invalido")

        assertEquals(1, api.createCycleAdjustmentCalls.size)
        val req = api.createCycleAdjustmentCalls.single()
        assertEquals(CycleAdjustmentTypes.OUTFLOW, req.type)
        assertEquals("presente sogro", req.label) // trimado
        assertEquals(42.90, req.amount, 0.0001)
        assertEquals(null, req.sourceSavingsTransactionId)
        assertTrue(req.date.isNotBlank())
        assertTrue(vm.state.value.savedSuccessfully)
        assertFalse(vm.state.value.isSaving)
    }
}
