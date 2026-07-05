package com.billfolder.android.ui.screens.savings

import androidx.lifecycle.SavedStateHandle
import com.billfolder.android.data.dto.CycleResponse
import com.billfolder.android.data.dto.SavingsAccountResponse
import com.billfolder.android.data.dto.SavingsTransactionResponse
import com.billfolder.android.data.dto.SavingsTransactionTypes
import com.billfolder.android.data.repository.CyclesRepository
import com.billfolder.android.data.repository.SavingsRepository
import com.billfolder.android.data.sync.DataChangeNotifier
import com.billfolder.android.ui.navigation.Routes
import com.billfolder.android.testutil.FakeBillFolderApi
import com.billfolder.android.testutil.MainDispatcherRule
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class SavingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = FakeBillFolderApi()
    private val notifier = DataChangeNotifier()
    private val savingsRepo = SavingsRepository(api, notifier)
    private val cyclesRepo = CyclesRepository(api, notifier)

    private fun cycle(id: String, start: String, end: String) = CycleResponse(
        id = id, startDate = start, endDate = end, label = "Ciclo $id",
        isRecurrenceGenerated = false, isCurrent = false,
        createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
    )

    private fun account(id: String) = SavingsAccountResponse(
        id = id, checkingAccountId = "chk-$id", bankName = "Banco $id",
        branch = "0001", accountNumber = "12345-6", initialBalance = 0.0,
        createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
    )

    private fun tx(
        id: String,
        accountId: String,
        date: String,
        type: String = SavingsTransactionTypes.DEPOSIT,
    ) = SavingsTransactionResponse(
        id = id, savingsAccountId = accountId, type = type, amount = 100.0,
        date = date, label = "mov $id", linkedTransactionId = null,
        createdAt = "2026-06-01T00:00:00Z", updatedAt = "2026-06-01T00:00:00Z",
    )

    private fun http404() = HttpException(Response.error<Any>(404, ResponseBody.create(null, "")))

    private fun viewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) =
        SavingsViewModel(savedStateHandle, savingsRepo, cyclesRepo, notifier)

    @Test
    fun `carrega Content com primeira conta selecionada`() {
        val c = cycle("cy1", "2026-06-01", "2026-06-30")
        api.onGetCurrentCycle = { c }
        api.cycles = listOf(c)
        api.savingsAccounts = listOf(account("a1"), account("a2"))
        api.savingsTransactions = listOf(tx("t1", "a1", "2026-06-10"))

        val state = viewModel().state.value

        assertTrue(state is SavingsUiState.Content)
        state as SavingsUiState.Content
        assertEquals("a1", state.selectedAccountId)
        assertEquals(2, state.accounts.size)
        assertEquals(1, state.allTransactions.size)
    }

    @Test
    fun `getCurrent 404 cai em NoCycle`() {
        api.onGetCurrentCycle = { throw http404() }

        assertTrue(viewModel().state.value is SavingsUiState.NoCycle)
    }

    @Test
    fun `ciclo ok mas sem contas cai em NoAccounts`() {
        val c = cycle("cy1", "2026-06-01", "2026-06-30")
        api.onGetCurrentCycle = { c }
        api.cycles = listOf(c)
        api.savingsAccounts = emptyList()
        api.savingsTransactions = emptyList()

        assertTrue(viewModel().state.value is SavingsUiState.NoAccounts)
    }

    @Test
    fun `IOException cai em Error`() {
        api.onGetCurrentCycle = { throw IOException("offline") }

        assertTrue(viewModel().state.value is SavingsUiState.Error)
    }

    @Test
    fun `deep link savingsAccountId seleciona a conta correspondente`() {
        val c = cycle("cy1", "2026-06-01", "2026-06-30")
        api.onGetCurrentCycle = { c }
        api.cycles = listOf(c)
        api.savingsAccounts = listOf(account("a1"), account("a2"))
        api.savingsTransactions = emptyList()
        val handle = SavedStateHandle(mapOf(Routes.SAVINGS_ARG_ID to "a2"))

        val state = viewModel(handle).state.value as SavingsUiState.Content

        assertEquals("a2", state.selectedAccountId)
    }

    @Test
    fun `deep link invalido cai na primeira conta`() {
        val c = cycle("cy1", "2026-06-01", "2026-06-30")
        api.onGetCurrentCycle = { c }
        api.cycles = listOf(c)
        api.savingsAccounts = listOf(account("a1"), account("a2"))
        api.savingsTransactions = emptyList()
        val handle = SavedStateHandle(mapOf(Routes.SAVINGS_ARG_ID to "nao-existe"))

        val state = viewModel(handle).state.value as SavingsUiState.Content

        assertEquals("a1", state.selectedAccountId)
    }

    @Test
    fun `onSelectAccount troca a conta selecionada`() {
        val c = cycle("cy1", "2026-06-01", "2026-06-30")
        api.onGetCurrentCycle = { c }
        api.cycles = listOf(c)
        api.savingsAccounts = listOf(account("a1"), account("a2"))
        api.savingsTransactions = emptyList()
        val vm = viewModel()

        vm.onSelectAccount("a2")

        assertEquals("a2", (vm.state.value as SavingsUiState.Content).selectedAccountId)
    }

    @Test
    fun `pullRefresh atualiza in-place sem virar Loading`() {
        val c = cycle("cy1", "2026-06-01", "2026-06-30")
        api.onGetCurrentCycle = { c }
        api.cycles = listOf(c)
        api.savingsAccounts = listOf(account("a1"))
        api.savingsTransactions = listOf(tx("t1", "a1", "2026-06-10"))
        val vm = viewModel()
        assertEquals(1, (vm.state.value as SavingsUiState.Content).allTransactions.size)

        api.savingsTransactions = listOf(tx("t1", "a1", "2026-06-10"), tx("t2", "a1", "2026-06-11"))
        vm.pullRefresh()

        val state = vm.state.value
        assertTrue(state is SavingsUiState.Content)
        assertEquals(2, (state as SavingsUiState.Content).allTransactions.size)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `navegacao de ciclo prev-next refetch das transacoes`() {
        val c1 = cycle("cy1", "2026-05-01", "2026-05-31")
        val c2 = cycle("cy2", "2026-06-01", "2026-06-30")
        val c3 = cycle("cy3", "2026-07-01", "2026-07-31")
        api.onGetCurrentCycle = { c2 }
        api.cycles = listOf(c1, c2, c3)
        api.savingsAccounts = listOf(account("a1"))
        api.savingsTransactions = listOf(tx("t2", "a1", "2026-06-10"))
        val vm = viewModel()

        api.savingsTransactions = listOf(tx("t1", "a1", "2026-05-10"))
        vm.goToPreviousCycle()
        var content = vm.state.value as SavingsUiState.Content
        assertEquals("cy1", content.cycle.id)
        assertEquals(listOf("t1"), content.allTransactions.map { it.id })

        api.savingsTransactions = listOf(tx("t3", "a1", "2026-07-10"))
        vm.goToNextCycle()
        content = vm.state.value as SavingsUiState.Content
        assertEquals("cy2", content.cycle.id)
    }

    @Test
    fun `navegacao no extremo e no-op`() {
        val c1 = cycle("cy1", "2026-05-01", "2026-05-31")
        val c2 = cycle("cy2", "2026-06-01", "2026-06-30")
        api.onGetCurrentCycle = { c1 } // c1 é o primeiro (startDate menor)
        api.cycles = listOf(c1, c2)
        api.savingsAccounts = listOf(account("a1"))
        api.savingsTransactions = emptyList()
        val vm = viewModel()

        vm.goToPreviousCycle() // c1 já é extremo inferior → no-op

        assertEquals("cy1", (vm.state.value as SavingsUiState.Content).cycle.id)
    }

    @Test
    fun `delete flow remove otimisticamente e chama o backend`() {
        val c = cycle("cy1", "2026-06-01", "2026-06-30")
        api.onGetCurrentCycle = { c }
        api.cycles = listOf(c)
        api.savingsAccounts = listOf(account("a1"))
        val target = tx("t1", "a1", "2026-06-10")
        api.savingsTransactions = listOf(target, tx("t2", "a1", "2026-06-11"))
        val vm = viewModel()

        vm.requestDelete(target)
        assertEquals("t1", (vm.state.value as SavingsUiState.Content).pendingDelete?.id)
        vm.confirmDelete()

        val state = vm.state.value as SavingsUiState.Content
        assertEquals(listOf("t2"), state.allTransactions.map { it.id })
        assertTrue(api.deletedSavingsTransactionIds.contains("t1"))
    }

    @Test
    fun `edit flow abre e fecha o item em edicao`() {
        val c = cycle("cy1", "2026-06-01", "2026-06-30")
        api.onGetCurrentCycle = { c }
        api.cycles = listOf(c)
        api.savingsAccounts = listOf(account("a1"))
        val target = tx("t1", "a1", "2026-06-10")
        api.savingsTransactions = listOf(target)
        val vm = viewModel()

        vm.requestEdit(target)
        assertEquals("t1", (vm.state.value as SavingsUiState.Content).editing?.id)

        vm.cancelEdit()
        assertEquals(null, (vm.state.value as SavingsUiState.Content).editing)
    }

    @Test
    fun `mudanca de dados global dispara refetch in-place`() {
        val c = cycle("cy1", "2026-06-01", "2026-06-30")
        api.onGetCurrentCycle = { c }
        api.cycles = listOf(c)
        api.savingsAccounts = listOf(account("a1"))
        api.savingsTransactions = listOf(tx("t1", "a1", "2026-06-10"))
        val vm = viewModel()
        assertEquals(1, (vm.state.value as SavingsUiState.Content).allTransactions.size)

        api.savingsTransactions = listOf(tx("t1", "a1", "2026-06-10"), tx("t2", "a1", "2026-06-11"))
        notifier.notifyDataChanged()

        assertEquals(2, (vm.state.value as SavingsUiState.Content).allTransactions.size)
    }
}
