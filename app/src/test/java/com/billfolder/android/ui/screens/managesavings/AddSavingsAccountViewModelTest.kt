package com.billfolder.android.ui.screens.managesavings

import com.billfolder.android.data.dto.CheckingAccountResponse
import com.billfolder.android.data.dto.CreateSavingsAccountRequest
import com.billfolder.android.data.dto.SavingsAccountResponse
import com.billfolder.android.data.repository.ReferenceDataRepository
import com.billfolder.android.data.repository.SavingsRepository
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

class AddSavingsAccountViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = FakeBillFolderApi()
    private val notifier = DataChangeNotifier()
    private val referenceRepo = ReferenceDataRepository(api)
    private val savingsRepo = SavingsRepository(api, notifier)

    private fun checking(
        id: String,
        isPrimary: Boolean = false,
        bank: String = "Banco $id",
    ) = CheckingAccountResponse(
        id = id, bankName = bank, branch = "0001", accountNumber = "12345-$id",
        initialBalance = 0.0, isPrimary = isPrimary,
        createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
    )

    private fun savingsResponse(id: String = "sav-1") = SavingsAccountResponse(
        id = id, checkingAccountId = "chk-1", bankName = "Banco", branch = "0001",
        accountNumber = "999", initialBalance = 100.0,
        createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
    )

    private fun viewModel() = AddSavingsAccountViewModel(referenceRepo, savingsRepo)

    @Test
    fun `init carrega checkings e pre-seleciona a primary`() {
        api.checkingAccounts = listOf(
            checking("chk-1", isPrimary = false),
            checking("chk-2", isPrimary = true, bank = "Banco Primary"),
        )

        val state = viewModel().state.value

        assertFalse(state.isLoadingReferences)
        assertEquals(2, state.checkingAccounts.size)
        assertEquals("chk-2", state.checkingAccountId)
        assertEquals("Banco Primary", state.bankName)
    }

    @Test
    fun `submit sem bankName falha validacao e nao chama create`() {
        var captured: CreateSavingsAccountRequest? = null
        api.onCreateSavingsAccount = { captured = it; savingsResponse() }
        // Checking sem bankName → o campo herdado fica em branco → validação barra.
        api.checkingAccounts = listOf(
            CheckingAccountResponse(
                id = "chk-empty", bankName = "", branch = "0001", accountNumber = "1",
                initialBalance = 0.0, isPrimary = true,
                createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
            ),
        )
        val vm = viewModel()
        vm.onInitialBalanceChange("100,00")

        vm.submit("checking vazio", "banco vazio", "agência vazia", "conta vazia", "saldo inválido", "duplicado")

        assertEquals("banco vazio", vm.state.value.errorMessage)
        assertNull(captured)
    }

    @Test
    fun `submit com saldo negativo falha validacao e nao chama create`() {
        var captured: CreateSavingsAccountRequest? = null
        api.onCreateSavingsAccount = { captured = it; savingsResponse() }
        api.checkingAccounts = listOf(checking("chk-1", isPrimary = true))
        val vm = viewModel()
        vm.onInitialBalanceChange("-5")

        vm.submit("checking vazio", "banco vazio", "agência vazia", "conta vazia", "saldo inválido", "duplicado")

        assertEquals("saldo inválido", vm.state.value.errorMessage)
        assertNull(captured)
    }

    @Test
    fun `submit valido chama create com os valores corretos e seta sucesso`() {
        var captured: CreateSavingsAccountRequest? = null
        api.onCreateSavingsAccount = { captured = it; savingsResponse() }
        api.checkingAccounts = listOf(checking("chk-1", isPrimary = true, bank = "Itaú"))
        val vm = viewModel()
        vm.onInitialBalanceChange("1500,50")

        vm.submit("checking vazio", "banco vazio", "agência vazia", "conta vazia", "saldo inválido", "duplicado")

        assertNotNull(captured)
        assertEquals("chk-1", captured!!.checkingAccountId)
        assertEquals("Itaú", captured!!.bankName)
        assertEquals("0001", captured!!.branch)
        assertEquals(1500.50, captured!!.initialBalance, 0.001)
        val state = vm.state.value
        assertTrue(state.savedSuccessfully)
        assertFalse(state.isSaving)
        assertNull(state.errorMessage)
    }

    @Test
    fun `prefill popula o form em modo edit`() {
        api.checkingAccounts = listOf(checking("chk-1", isPrimary = true))
        val vm = viewModel()

        vm.prefill(savingsResponse(id = "sav-77"))

        val state = vm.state.value
        assertEquals("sav-77", state.editingId)
        assertEquals("chk-1", state.checkingAccountId)
        assertEquals("Banco", state.bankName)
        assertEquals("0001", state.branch)
        assertEquals("999", state.accountNumber)
        assertTrue(state.initialBalance.isNotBlank())
    }
}
