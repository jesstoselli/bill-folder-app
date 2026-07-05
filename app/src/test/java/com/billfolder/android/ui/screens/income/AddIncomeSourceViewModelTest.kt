package com.billfolder.android.ui.screens.income

import com.billfolder.android.data.dto.CreateIncomeSourceRequest
import com.billfolder.android.data.dto.IncomeSourceResponse
import com.billfolder.android.data.repository.IncomeRepository
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

class AddIncomeSourceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = FakeBillFolderApi()
    private val notifier = DataChangeNotifier()
    private val repo = IncomeRepository(api, notifier)

    private fun viewModel() = AddIncomeSourceViewModel(repo)

    private fun response(id: String = "src-1") = IncomeSourceResponse(
        id = id, origin = "Salário", originType = "work", defaultAmount = 5000.0,
        expectedDay = 5, startDate = "2026-01-01", endDate = "2026-12-31",
        isActive = true, createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
    )

    @Test
    fun `submit com origin em branco falha validacao e nao chama create`() {
        var captured: CreateIncomeSourceRequest? = null
        api.onCreateIncomeSource = { captured = it; response() }
        val vm = viewModel()
        vm.onOriginChange("")
        vm.onDefaultAmountChange("5000")
        vm.onExpectedDayChange("5")

        vm.submit("origem vazia", "valor inválido", "dia inválido", "fim antes do início")

        val state = vm.state.value
        assertEquals("origem vazia", state.errorMessage)
        assertFalse(state.savedSuccessfully)
        assertNull(captured)
    }

    @Test
    fun `submit com valor invalido falha validacao e nao chama create`() {
        var captured: CreateIncomeSourceRequest? = null
        api.onCreateIncomeSource = { captured = it; response() }
        val vm = viewModel()
        vm.onOriginChange("Salário")
        vm.onDefaultAmountChange("0")
        vm.onExpectedDayChange("5")

        vm.submit("origem vazia", "valor inválido", "dia inválido", "fim antes do início")

        assertEquals("valor inválido", vm.state.value.errorMessage)
        assertNull(captured)
    }

    @Test
    fun `submit com expectedDay invalido falha validacao e nao chama create`() {
        var captured: CreateIncomeSourceRequest? = null
        api.onCreateIncomeSource = { captured = it; response() }
        val vm = viewModel()
        vm.onOriginChange("Salário")
        vm.onDefaultAmountChange("5000")
        vm.onExpectedDayChange("40")

        vm.submit("origem vazia", "valor inválido", "dia inválido", "fim antes do início")

        assertEquals("dia inválido", vm.state.value.errorMessage)
        assertNull(captured)
    }

    @Test
    fun `submit com endDate antes de startDate falha validacao e nao chama create`() {
        var captured: CreateIncomeSourceRequest? = null
        api.onCreateIncomeSource = { captured = it; response() }
        val vm = viewModel()
        vm.onOriginChange("Salário")
        vm.onDefaultAmountChange("5000")
        vm.onExpectedDayChange("5")
        vm.onStartDateChange("2026-06-01")
        vm.onEndDateChange("2026-01-01")

        vm.submit("origem vazia", "valor inválido", "dia inválido", "fim antes do início")

        assertEquals("fim antes do início", vm.state.value.errorMessage)
        assertNull(captured)
    }

    @Test
    fun `submit valido chama create com os valores corretos e seta sucesso`() {
        var captured: CreateIncomeSourceRequest? = null
        api.onCreateIncomeSource = { captured = it; response() }
        val vm = viewModel()
        vm.onOriginChange("  Salário  ")
        vm.onOriginTypeChange("work")
        vm.onDefaultAmountChange("5000,75")
        vm.onExpectedDayChange("5")
        vm.onStartDateChange("2026-01-01")
        vm.onEndDateChange("2026-12-31")

        vm.submit("origem vazia", "valor inválido", "dia inválido", "fim antes do início")

        assertNotNull(captured)
        assertEquals("Salário", captured!!.origin)
        assertEquals("work", captured!!.originType)
        assertEquals(5000.75, captured!!.defaultAmount, 0.001)
        assertEquals(5, captured!!.expectedDay)
        assertEquals("2026-01-01", captured!!.startDate)
        assertEquals("2026-12-31", captured!!.endDate)
        val state = vm.state.value
        assertTrue(state.savedSuccessfully)
        assertFalse(state.isSaving)
        assertNull(state.errorMessage)
    }

    @Test
    fun `prefill popula o form em modo edit`() {
        val vm = viewModel()

        vm.prefill(response(id = "src-88"))

        val state = vm.state.value
        assertEquals("src-88", state.editingId)
        assertEquals("Salário", state.origin)
        assertEquals("work", state.originType)
        assertEquals("5", state.expectedDay)
        assertEquals("2026-01-01", state.startDate)
        assertEquals("2026-12-31", state.endDate)
        assertTrue(state.defaultAmount.isNotBlank())
    }
}
