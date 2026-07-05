package com.billfolder.android.ui.screens.income

import com.billfolder.android.data.dto.IncomeEntryResponse
import com.billfolder.android.data.dto.IncomeSourceResponse
import com.billfolder.android.data.repository.IncomeRepository
import com.billfolder.android.data.sync.DataChangeNotifier
import com.billfolder.android.testutil.FakeBillFolderApi
import com.billfolder.android.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AddIncomeEntryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = FakeBillFolderApi()
    private val notifier = DataChangeNotifier()
    private val incomeRepo = IncomeRepository(api, notifier)

    private fun source(id: String, active: Boolean) = IncomeSourceResponse(
        id = id, origin = "Fonte $id", originType = "work", defaultAmount = 5000.0,
        expectedDay = 5, startDate = "2026-01-01", isActive = active,
        createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
    )

    private fun incomeEntry(id: String) = IncomeEntryResponse(
        id = id, sourceId = "src1", expectedAmount = 5000.0, expectedDate = "2026-06-05",
        status = "expected", createdAt = "2026-06-01T00:00:00Z", updatedAt = "2026-06-01T00:00:00Z",
    )

    private fun viewModel() = AddIncomeEntryViewModel(incomeRepo)

    // ---- init loads reference data --------------------------------------------

    @Test
    fun `init carrega apenas as fontes ativas`() {
        api.incomeSources = listOf(source("src1", true), source("src2", false), source("src3", true))

        val state = viewModel().state.value

        assertFalse(state.isLoadingReferences)
        assertEquals(listOf("src1", "src3"), state.sources.map { it.id })
    }

    // ---- INVALID submit -------------------------------------------------------

    @Test
    fun `submit com valor em branco expoe erro e nao chama o repo`() {
        api.incomeSources = emptyList()
        val vm = viewModel()
        // amount vazio

        vm.submit("valor inválido")

        assertEquals("valor inválido", vm.state.value.errorMessage)
        assertTrue(api.createIncomeEntryCalls.isEmpty())
    }

    @Test
    fun `submit com valor zero expoe erro e nao chama o repo`() {
        api.incomeSources = emptyList()
        val vm = viewModel()
        vm.onAmountChange("0")

        vm.submit("valor inválido")

        assertEquals("valor inválido", vm.state.value.errorMessage)
        assertTrue(api.createIncomeEntryCalls.isEmpty())
    }

    // ---- VALID submit ---------------------------------------------------------

    @Test
    fun `submit valido chama o repo e sinaliza sucesso`() {
        api.incomeSources = listOf(source("src1", true))
        api.onCreateIncomeEntry = { incomeEntry("new-1") }
        val vm = viewModel()
        vm.onAmountChange("5000,00")
        vm.onSourceChange("src1")
        vm.onNotesChange("salário")

        vm.submit("valor inválido")

        assertEquals(1, api.createIncomeEntryCalls.size)
        val req = api.createIncomeEntryCalls.first()
        assertEquals("src1", req.sourceId)
        assertEquals(5000.0, req.expectedAmount, 0.0001)
        assertEquals("salário", req.notes)
        assertTrue(req.expectedDate.isNotBlank())

        val state = vm.state.value
        assertTrue(state.savedSuccessfully)
        assertFalse(state.isSaving)
        assertNull(state.errorMessage)
    }

    @Test
    fun `submit valido avulso mantem sourceId nulo`() {
        api.incomeSources = emptyList()
        api.onCreateIncomeEntry = { incomeEntry("new-2") }
        val vm = viewModel()
        vm.onAmountChange("300,00")
        // sourceId permanece null (recebimento avulso)

        vm.submit("valor inválido")

        assertEquals(1, api.createIncomeEntryCalls.size)
        assertNull(api.createIncomeEntryCalls.first().sourceId)
        assertTrue(vm.state.value.savedSuccessfully)
    }

    // ---- prefill / edit -------------------------------------------------------

    @Test
    fun `prefill preenche os campos e ativa modo edit`() {
        api.incomeSources = listOf(source("src1", true))
        val vm = viewModel()

        vm.prefill(
            incomeEntry("ie-3").copy(
                expectedAmount = 1200.0, sourceId = "src1",
                expectedDate = "2026-07-10", notes = "freela",
            ),
        )

        val state = vm.state.value
        assertEquals("ie-3", state.editingId)
        assertEquals("src1", state.selectedSourceId)
        assertEquals("2026-07-10", state.expectedDate)
        assertEquals("freela", state.notes)
        assertEquals("1200,00", state.amount)
    }
}
