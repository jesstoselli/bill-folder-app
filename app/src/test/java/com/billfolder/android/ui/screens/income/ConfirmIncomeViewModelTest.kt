package com.billfolder.android.ui.screens.income

import com.billfolder.android.data.dto.IncomeEntryResponse
import com.billfolder.android.data.dto.UpdateIncomeEntryRequest
import com.billfolder.android.data.repository.IncomeRepository
import com.billfolder.android.data.sync.DataChangeNotifier
import com.billfolder.android.testutil.FakeBillFolderApi
import com.billfolder.android.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ConfirmIncomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = FakeBillFolderApi()
    private val notifier = DataChangeNotifier()
    private val repo = IncomeRepository(api, notifier)

    private fun entryResponse(id: String) = IncomeEntryResponse(
        id = id, sourceId = null, sourceOrigin = null, expectedAmount = 100.0,
        actualAmount = 100.0, expectedDate = "2026-06-05", actualDate = "2026-06-05",
        status = "received", notes = null,
        createdAt = "2026-06-01T00:00:00Z", updatedAt = "2026-06-05T00:00:00Z",
    )

    private fun viewModel() = ConfirmIncomeViewModel(repo)

    @Test
    fun `initializeFor pre-preenche entryId e actualAmount`() {
        val vm = viewModel()

        vm.initializeFor(entryId = "ent1", expectedAmount = 300.0)

        val state = vm.state.value
        assertEquals("ent1", state.entryId)
        assertTrue(state.actualAmount.isNotBlank())
    }

    @Test
    fun `submit com amount invalido nao chama o repo`() {
        val vm = viewModel()
        vm.initializeFor(entryId = "ent1", expectedAmount = 100.0)
        vm.onActualAmountChange("0")

        var called = false
        api.onUpdateIncomeEntry = { _, _ -> called = true; entryResponse("ent1") }
        vm.submit(amountInvalidMessage = "valor invalido")

        assertEquals("valor invalido", vm.state.value.errorMessage)
        assertFalse(vm.state.value.savedSuccessfully)
        assertFalse(called)
    }

    @Test
    fun `submit valido marca como received via onUpdateIncomeEntry`() {
        var capturedId: String? = null
        var capturedReq: UpdateIncomeEntryRequest? = null
        api.onUpdateIncomeEntry = { id, req ->
            capturedId = id
            capturedReq = req
            entryResponse(id)
        }
        val vm = viewModel()
        vm.initializeFor(entryId = "ent1", expectedAmount = 100.0)
        vm.onActualAmountChange("95,50")

        vm.submit(amountInvalidMessage = "valor invalido")

        assertEquals("ent1", capturedId)
        assertNotNull(capturedReq)
        assertEquals("received", capturedReq!!.status)
        assertEquals(95.50, capturedReq!!.actualAmount!!, 0.0001)
        assertTrue(capturedReq!!.actualDate!!.isNotBlank())
        assertTrue(vm.state.value.savedSuccessfully)
        assertFalse(vm.state.value.isSaving)
    }
}
