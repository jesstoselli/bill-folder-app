package com.billfolder.android.ui.screens.expenses

import com.billfolder.android.data.dto.ExpenseResponse
import com.billfolder.android.data.dto.UpdateOccurrencesRequest
import com.billfolder.android.data.repository.ExpensesRepository
import com.billfolder.android.data.sync.DataChangeNotifier
import com.billfolder.android.testutil.FakeBillFolderApi
import com.billfolder.android.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class AdjustOccurrencesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = FakeBillFolderApi()
    private val repo = ExpensesRepository(api, DataChangeNotifier())

    private fun therapy(total: Int = 5, paid: Int = 0) = ExpenseResponse(
        id = "therapy", dueDate = "2026-12-01", label = "Terapia",
        expectedAmount = 200.0 * total, status = "pending",
        categoryId = "cat", categoryName = "Saúde",
        occurrenceAmount = 200.0, occurrencesTotal = total, occurrencesPaid = paid,
        paidToDate = 200.0 * paid,
        createdAt = "2026-12-01T00:00:00Z", updatedAt = "2026-12-01T00:00:00Z",
    )

    private fun viewModel(expense: ExpenseResponse = therapy()) =
        AdjustOccurrencesViewModel(repo).apply { initializeFor(expense) }

    @Test
    fun `comeca nas sessoes atuais e recalcula o total ao mudar`() {
        val vm = viewModel()
        assertEquals(5, vm.state.value.count)
        assertFalse(vm.state.value.hasChanged)

        vm.decrement()
        vm.decrement()

        assertEquals(3, vm.state.value.count)
        assertEquals(600.0, vm.state.value.newTotal, 0.0)
        assertEquals(1000.0, vm.state.value.originalTotal, 0.0)
    }

    @Test
    fun `nao desce abaixo das sessoes ja pagas nem de uma`() {
        val paidTwo = viewModel(therapy(total = 3, paid = 2))
        paidTwo.decrement()
        paidTwo.decrement()
        assertEquals(2, paidTwo.state.value.count)
        assertFalse(paidTwo.state.value.canDecrement)

        val nonePaid = viewModel(therapy(total = 2))
        repeat(3) { nonePaid.decrement() }
        assertEquals(1, nonePaid.state.value.count)
    }

    @Test
    fun `nao passa do teto de sessoes`() {
        val vm = viewModel(therapy(total = 9))
        repeat(3) { vm.increment() }
        assertEquals(MAX_OCCURRENCES, vm.state.value.count)
    }

    @Test
    fun `salvar manda so o novo total`() {
        api.onUpdateOccurrences = { _, _ -> therapy(total = 3) }
        val vm = viewModel()
        vm.decrement()
        vm.decrement()

        vm.submit()

        assertEquals(listOf("therapy" to UpdateOccurrencesRequest(3)), api.updateOccurrencesCalls)
        assertTrue(vm.state.value.savedSuccessfully)
    }

    @Test
    fun `sem mudanca nao chama o backend`() {
        val vm = viewModel()
        vm.submit()
        assertTrue(api.updateOccurrencesCalls.isEmpty())
    }

    @Test
    fun `falha de rede mostra erro e deixa tentar de novo`() {
        api.onUpdateOccurrences = { _, _ -> throw IOException("offline") }
        val vm = viewModel()
        vm.decrement()

        vm.submit()

        assertEquals("Sem conexão. Tenta de novo.", vm.state.value.errorMessage)
        assertFalse(vm.state.value.isSaving)
        assertFalse(vm.state.value.savedSuccessfully)
    }
}
