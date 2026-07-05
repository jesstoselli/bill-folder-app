package com.billfolder.android.ui.screens.savings

import com.billfolder.android.data.dto.SavingsAccountResponse
import com.billfolder.android.data.dto.SavingsTransactionResponse
import com.billfolder.android.data.dto.SavingsTransactionTypes
import com.billfolder.android.data.repository.SavingsRepository
import com.billfolder.android.data.sync.DataChangeNotifier
import com.billfolder.android.testutil.FakeBillFolderApi
import com.billfolder.android.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AddSavingsTransactionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = FakeBillFolderApi()
    private val notifier = DataChangeNotifier()
    private val repo = SavingsRepository(api, notifier)

    private fun account(id: String) = SavingsAccountResponse(
        id = id, checkingAccountId = "chk-$id", bankName = "Banco $id",
        branch = "0001", accountNumber = "12345-6", initialBalance = 0.0,
        createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
    )

    private fun txResponse(id: String, accountId: String) = SavingsTransactionResponse(
        id = id, savingsAccountId = accountId, type = SavingsTransactionTypes.DEPOSIT,
        amount = 100.0, date = "2026-06-10", label = "mov", linkedTransactionId = null,
        createdAt = "2026-06-10T00:00:00Z", updatedAt = "2026-06-10T00:00:00Z",
    )

    private fun viewModel() = AddSavingsTransactionViewModel(repo)

    @Test
    fun `init carrega poupancas e pre-seleciona a primeira`() {
        api.savingsAccounts = listOf(account("a1"), account("a2"))

        val state = viewModel().state.value

        assertEquals(2, state.accounts.size)
        assertEquals("a1", state.savingsAccountId)
        assertFalse(state.isLoadingReferences)
    }

    @Test
    fun `submit invalido nao chama o repo e seta erro`() {
        api.savingsAccounts = listOf(account("a1"))
        val vm = viewModel()
        // amount vazio -> parseAmount null -> invalido
        vm.onAmountChange("")

        vm.submit(accountEmptyMessage = "conta obrigatoria", amountInvalidMessage = "valor invalido")

        val state = vm.state.value
        assertEquals("valor invalido", state.errorMessage)
        assertFalse(state.savedSuccessfully)
        assertTrue(api.createSavingsTransactionCalls.isEmpty())
    }

    @Test
    fun `submit valido chama createTransaction com o payload correto`() {
        api.savingsAccounts = listOf(account("a1"))
        api.onCreateSavingsTransaction = { txResponse("t1", "a1") }
        val vm = viewModel()
        vm.onAmountChange("150,50")
        vm.onLabelChange("bonus")
        vm.onTypeChange(SavingsTransactionTypes.YIELD)

        vm.submit(accountEmptyMessage = "conta obrigatoria", amountInvalidMessage = "valor invalido")

        assertEquals(1, api.createSavingsTransactionCalls.size)
        val req = api.createSavingsTransactionCalls.single()
        assertEquals("a1", req.savingsAccountId)
        assertEquals(SavingsTransactionTypes.YIELD, req.type)
        assertEquals(150.50, req.amount, 0.0001)
        assertEquals("bonus", req.label)
        assertTrue(req.date.isNotBlank())
        assertTrue(vm.state.value.savedSuccessfully)
        assertFalse(vm.state.value.isSaving)
    }

    @Test
    fun `submit valido sem conta selecionada bloqueia com mensagem de conta`() {
        api.savingsAccounts = emptyList() // sem contas -> savingsAccountId fica null
        val vm = viewModel()
        vm.onAmountChange("10")

        vm.submit(accountEmptyMessage = "conta obrigatoria", amountInvalidMessage = "valor invalido")

        assertEquals("conta obrigatoria", vm.state.value.errorMessage)
        assertTrue(api.createSavingsTransactionCalls.isEmpty())
    }

    @Test
    fun `label em branco vira null no payload`() {
        api.savingsAccounts = listOf(account("a1"))
        var captured: com.billfolder.android.data.dto.CreateSavingsTransactionRequest? = null
        api.onCreateSavingsTransaction = { captured = it; txResponse("t1", "a1") }
        val vm = viewModel()
        vm.onAmountChange("10")

        vm.submit(accountEmptyMessage = "conta obrigatoria", amountInvalidMessage = "valor invalido")

        assertNotNull(captured)
        assertEquals(null, captured!!.label)
    }
}
