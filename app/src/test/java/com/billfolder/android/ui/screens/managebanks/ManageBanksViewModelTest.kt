package com.billfolder.android.ui.screens.managebanks

import com.billfolder.android.data.api.BillFolderApi
import com.billfolder.android.data.dto.CheckingAccountResponse
import com.billfolder.android.data.repository.CheckingAccountsRepository
import com.billfolder.android.data.sync.DataChangeNotifier
import com.billfolder.android.testutil.FakeBillFolderApi
import com.billfolder.android.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class ManageBanksViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = FakeBillFolderApi()
    private val notifier = DataChangeNotifier()
    private val repo = CheckingAccountsRepository(api, notifier)

    private fun account(id: String) = CheckingAccountResponse(
        id = id, bankName = "Banco $id", branch = "0001", accountNumber = "12345-6",
        initialBalance = 0.0, isPrimary = false,
        createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
    )

    private fun viewModel() = ManageBanksViewModel(repo)

    @Test
    fun `carrega Content com a lista de contas`() {
        api.checkingAccounts = listOf(account("a1"), account("a2"))

        val state = viewModel().state.value

        assertTrue(state is ManageBanksUiState.Content)
        state as ManageBanksUiState.Content
        assertEquals(listOf("a1", "a2"), state.accounts.map { it.id })
    }

    @Test
    fun `lista vazia cai em Content com lista vazia`() {
        api.checkingAccounts = emptyList()

        val state = viewModel().state.value

        assertTrue(state is ManageBanksUiState.Content)
        assertTrue((state as ManageBanksUiState.Content).accounts.isEmpty())
    }

    @Test
    fun `IOException na carga cai em Error`() {
        val failingApi = object : BillFolderApi by api {
            override suspend fun getCheckingAccounts(): List<CheckingAccountResponse> =
                throw IOException("offline")
        }
        val failingRepo = CheckingAccountsRepository(failingApi, notifier)

        val state = ManageBanksViewModel(failingRepo).state.value

        assertTrue(state is ManageBanksUiState.Error)
    }

    @Test
    fun `delete flow remove otimisticamente e chama o backend`() {
        api.checkingAccounts = listOf(account("a1"), account("a2"))
        val vm = viewModel()

        vm.requestDelete(account("a1"))
        assertEquals("a1", (vm.state.value as ManageBanksUiState.Content).pendingDelete?.id)
        vm.confirmDelete()

        val state = vm.state.value as ManageBanksUiState.Content
        assertEquals(listOf("a2"), state.accounts.map { it.id })
        assertNull(state.pendingDelete)
        assertNull(state.deletingId)
        assertTrue(api.deletedCheckingAccountIds.contains("a1"))
    }

    @Test
    fun `cancelDelete limpa o pending sem deletar`() {
        api.checkingAccounts = listOf(account("a1"))
        val vm = viewModel()

        vm.requestDelete(account("a1"))
        vm.cancelDelete()

        val state = vm.state.value as ManageBanksUiState.Content
        assertNull(state.pendingDelete)
        assertEquals(listOf("a1"), state.accounts.map { it.id })
        assertTrue(api.deletedCheckingAccountIds.isEmpty())
    }

    @Test
    fun `edit flow abre e fecha o item em edicao`() {
        api.checkingAccounts = listOf(account("a1"))
        val vm = viewModel()

        vm.requestEdit(account("a1"))
        assertEquals("a1", (vm.state.value as ManageBanksUiState.Content).editing?.id)

        vm.cancelEdit()
        assertNull((vm.state.value as ManageBanksUiState.Content).editing)
    }
}
