package com.billfolder.android.ui.screens.managebanks

import com.billfolder.android.data.dto.CheckingAccountResponse
import com.billfolder.android.data.dto.CreateCheckingAccountRequest
import com.billfolder.android.data.repository.CheckingAccountsRepository
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

class AddCheckingAccountViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = FakeBillFolderApi()
    private val notifier = DataChangeNotifier()
    private val repo = CheckingAccountsRepository(api, notifier)

    private fun viewModel() = AddCheckingAccountViewModel(repo)

    private fun response(id: String = "chk-1") = CheckingAccountResponse(
        id = id, bankName = "Itaú", branch = "0001", accountNumber = "12345-6",
        initialBalance = 250.0, isPrimary = true,
        createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
    )

    @Test
    fun `submit sem bankName falha validacao e nao chama create`() {
        var captured: CreateCheckingAccountRequest? = null
        api.onCreateCheckingAccount = { captured = it; response() }
        val vm = viewModel()
        vm.onBankNameChange("")
        vm.onBranchChange("0001")
        vm.onAccountNumberChange("12345-6")
        vm.onInitialBalanceChange("100")

        vm.submit("banco vazio", "agência vazia", "conta vazia", "saldo inválido")

        val state = vm.state.value
        assertEquals("banco vazio", state.errorMessage)
        assertFalse(state.savedSuccessfully)
        assertNull(captured)
    }

    @Test
    fun `submit com saldo negativo falha validacao e nao chama create`() {
        var captured: CreateCheckingAccountRequest? = null
        api.onCreateCheckingAccount = { captured = it; response() }
        val vm = viewModel()
        vm.onBankNameChange("Itaú")
        vm.onBranchChange("0001")
        vm.onAccountNumberChange("12345-6")
        vm.onInitialBalanceChange("-1")

        vm.submit("banco vazio", "agência vazia", "conta vazia", "saldo inválido")

        assertEquals("saldo inválido", vm.state.value.errorMessage)
        assertNull(captured)
    }

    @Test
    fun `submit valido chama create com os valores corretos e seta sucesso`() {
        var captured: CreateCheckingAccountRequest? = null
        api.onCreateCheckingAccount = { captured = it; response() }
        val vm = viewModel()
        vm.onBankNameChange("  Itaú  ")
        vm.onBranchChange("0001")
        vm.onAccountNumberChange("12345-6")
        vm.onInitialBalanceChange("1000,25")
        vm.onIsPrimaryChange(true)

        vm.submit("banco vazio", "agência vazia", "conta vazia", "saldo inválido")

        assertNotNull(captured)
        assertEquals("Itaú", captured!!.bankName)
        assertEquals("0001", captured!!.branch)
        assertEquals("12345-6", captured!!.accountNumber)
        assertEquals(1000.25, captured!!.initialBalance, 0.001)
        assertTrue(captured!!.isPrimary)
        val state = vm.state.value
        assertTrue(state.savedSuccessfully)
        assertFalse(state.isSaving)
        assertNull(state.errorMessage)
    }

    @Test
    fun `prefill popula o form em modo edit`() {
        val vm = viewModel()

        vm.prefill(response(id = "chk-42"))

        val state = vm.state.value
        assertEquals("chk-42", state.editingId)
        assertEquals("Itaú", state.bankName)
        assertEquals("0001", state.branch)
        assertEquals("12345-6", state.accountNumber)
        assertTrue(state.isPrimary)
        assertTrue(state.initialBalance.isNotBlank())
    }
}
