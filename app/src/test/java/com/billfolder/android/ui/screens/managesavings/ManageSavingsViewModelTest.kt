package com.billfolder.android.ui.screens.managesavings

import com.billfolder.android.data.api.BillFolderApi
import com.billfolder.android.data.dto.SavingsAccountResponse
import com.billfolder.android.data.repository.SavingsRepository
import com.billfolder.android.data.sync.DataChangeNotifier
import com.billfolder.android.testutil.FakeBillFolderApi
import com.billfolder.android.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class ManageSavingsViewModelTest {

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

    private fun viewModel() = ManageSavingsViewModel(repo)

    @Test
    fun `carrega Content com a lista de contas`() {
        api.savingsAccounts = listOf(account("a1"), account("a2"))

        val state = viewModel().state.value

        assertTrue(state is ManageSavingsUiState.Content)
        state as ManageSavingsUiState.Content
        assertEquals(listOf("a1", "a2"), state.accounts.map { it.id })
    }

    @Test
    fun `lista vazia cai em Content com lista vazia`() {
        api.savingsAccounts = emptyList()

        val state = viewModel().state.value

        assertTrue(state is ManageSavingsUiState.Content)
        assertTrue((state as ManageSavingsUiState.Content).accounts.isEmpty())
    }

    @Test
    fun `IOException na carga cai em Error`() {
        val failingApi = object : BillFolderApi by api {
            override suspend fun getSavingsAccounts(): List<SavingsAccountResponse> =
                throw IOException("offline")
        }
        val failingRepo = SavingsRepository(failingApi, notifier)

        val state = ManageSavingsViewModel(failingRepo).state.value

        assertTrue(state is ManageSavingsUiState.Error)
    }

    @Test
    fun `delete flow remove otimisticamente e chama o backend`() {
        api.savingsAccounts = listOf(account("a1"), account("a2"))
        val vm = viewModel()

        vm.requestDelete(account("a1"))
        assertEquals("a1", (vm.state.value as ManageSavingsUiState.Content).pendingDelete?.id)
        vm.confirmDelete()

        val state = vm.state.value as ManageSavingsUiState.Content
        assertEquals(listOf("a2"), state.accounts.map { it.id })
        assertNull(state.pendingDelete)
        assertNull(state.deletingId)
        assertTrue(api.deletedSavingsAccountIds.contains("a1"))
    }

    @Test
    fun `cancelDelete limpa o pending sem deletar`() {
        api.savingsAccounts = listOf(account("a1"))
        val vm = viewModel()

        vm.requestDelete(account("a1"))
        vm.cancelDelete()

        val state = vm.state.value as ManageSavingsUiState.Content
        assertNull(state.pendingDelete)
        assertEquals(listOf("a1"), state.accounts.map { it.id })
        assertTrue(api.deletedSavingsAccountIds.isEmpty())
    }

    @Test
    fun `edit flow abre e fecha o item em edicao`() {
        api.savingsAccounts = listOf(account("a1"))
        val vm = viewModel()

        vm.requestEdit(account("a1"))
        assertEquals("a1", (vm.state.value as ManageSavingsUiState.Content).editing?.id)

        vm.cancelEdit()
        assertNull((vm.state.value as ManageSavingsUiState.Content).editing)
    }
}
