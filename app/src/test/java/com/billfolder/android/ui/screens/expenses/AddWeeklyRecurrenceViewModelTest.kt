package com.billfolder.android.ui.screens.expenses

import com.billfolder.android.data.dto.CategoryDto
import com.billfolder.android.data.dto.ExpenseRecurrenceResponse
import com.billfolder.android.data.repository.ExpenseRecurrencesRepository
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

class AddWeeklyRecurrenceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = FakeBillFolderApi()
    private val notifier = DataChangeNotifier()
    private val referenceDataRepo = ReferenceDataRepository(api)
    private val recurrencesRepo = ExpenseRecurrencesRepository(api, notifier)

    private fun category(id: String, order: Int) = CategoryDto(
        id = id, key = "key-$id", namePt = "Cat $id", isSystem = true, displayOrder = order,
    )

    private fun recurrenceResponse(id: String) = ExpenseRecurrenceResponse(
        id = id, defaultLabel = "fisio", defaultAmount = 100.0, defaultCategoryId = "cat1",
        frequency = "weekly", weekday = 3, startDate = "2026-07-01",
        createdAt = "2026-07-01T00:00:00Z", updatedAt = "2026-07-01T00:00:00Z",
    )

    private fun viewModel() = AddWeeklyRecurrenceViewModel(referenceDataRepo, recurrencesRepo)

    private fun msgs() = arrayOf("label vazio", "valor inválido", "categoria vazia", "dia vazio")

    private fun AddWeeklyRecurrenceViewModel.submitWithDefaults() =
        submit(msgs()[0], msgs()[1], msgs()[2], msgs()[3])

    // ---- init loads reference data --------------------------------------------

    @Test
    fun `init carrega categorias no form state`() {
        api.categories = listOf(category("cat2", 2), category("cat1", 1))

        val state = viewModel().state.value

        assertFalse(state.isLoadingReferences)
        assertEquals(listOf("cat1", "cat2"), state.categories.map { it.id })
    }

    // ---- INVALID submit -------------------------------------------------------

    @Test
    fun `submit com label em branco expoe erro e nao chama o repo`() {
        api.categories = listOf(category("cat1", 1))
        val vm = viewModel()
        vm.onAmountChange("100,00")
        vm.onCategoryChange("cat1")
        vm.onWeekdayChange(3)
        // label em branco

        vm.submitWithDefaults()

        assertEquals("label vazio", vm.state.value.errorMessage)
        assertTrue(api.createExpenseRecurrenceCalls.isEmpty())
    }

    @Test
    fun `submit com valor zero expoe erro e nao chama o repo`() {
        api.categories = listOf(category("cat1", 1))
        val vm = viewModel()
        vm.onLabelChange("Fisioterapia")
        vm.onAmountChange("0")
        vm.onCategoryChange("cat1")
        vm.onWeekdayChange(3)

        vm.submitWithDefaults()

        assertEquals("valor inválido", vm.state.value.errorMessage)
        assertTrue(api.createExpenseRecurrenceCalls.isEmpty())
    }

    @Test
    fun `submit sem categoria expoe erro e nao chama o repo`() {
        api.categories = listOf(category("cat1", 1))
        val vm = viewModel()
        vm.onLabelChange("Fisioterapia")
        vm.onAmountChange("100,00")
        vm.onWeekdayChange(3)
        // categoria não selecionada

        vm.submitWithDefaults()

        assertEquals("categoria vazia", vm.state.value.errorMessage)
        assertTrue(api.createExpenseRecurrenceCalls.isEmpty())
    }

    @Test
    fun `submit sem dia da semana expoe erro e nao chama o repo`() {
        api.categories = listOf(category("cat1", 1))
        val vm = viewModel()
        vm.onLabelChange("Fisioterapia")
        vm.onAmountChange("100,00")
        vm.onCategoryChange("cat1")
        // weekday não selecionado

        vm.submitWithDefaults()

        assertEquals("dia vazio", vm.state.value.errorMessage)
        assertTrue(api.createExpenseRecurrenceCalls.isEmpty())
    }

    // ---- VALID submit ---------------------------------------------------------

    @Test
    fun `submit valido chama o repo com frequency weekly e sinaliza sucesso`() {
        api.categories = listOf(category("cat1", 1))
        api.onCreateExpenseRecurrence = { recurrenceResponse("new-1") }
        val vm = viewModel()
        vm.onLabelChange("  Fisioterapia  ")
        vm.onAmountChange("120,00")
        vm.onCategoryChange("cat1")
        vm.onWeekdayChange(3) // quarta-feira

        vm.submitWithDefaults()

        assertEquals(1, api.createExpenseRecurrenceCalls.size)
        val req = api.createExpenseRecurrenceCalls.first()
        assertEquals("Fisioterapia", req.defaultLabel) // trimmed
        assertEquals(120.0, req.defaultAmount, 0.0001)
        assertEquals("cat1", req.defaultCategoryId)
        assertEquals("weekly", req.frequency)
        assertEquals(3, req.weekday)
        assertNull(req.dueDay)
        assertTrue(req.startDate.isNotBlank())

        val state = vm.state.value
        assertTrue(state.savedSuccessfully)
        assertFalse(state.isSaving)
        assertNull(state.errorMessage)
    }

    @Test
    fun `weekday zero (domingo) e valido`() {
        api.categories = listOf(category("cat1", 1))
        api.onCreateExpenseRecurrence = { recurrenceResponse("new-1") }
        val vm = viewModel()
        vm.onLabelChange("Faxina")
        vm.onAmountChange("80")
        vm.onCategoryChange("cat1")
        vm.onWeekdayChange(0) // domingo

        vm.submitWithDefaults()

        assertEquals(1, api.createExpenseRecurrenceCalls.size)
        assertEquals(0, api.createExpenseRecurrenceCalls.first().weekday)
        assertTrue(vm.state.value.savedSuccessfully)
    }
}
