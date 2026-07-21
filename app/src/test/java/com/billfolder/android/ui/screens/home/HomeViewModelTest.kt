package com.billfolder.android.ui.screens.home

import com.billfolder.android.data.dto.HomeBalanceDto
import com.billfolder.android.data.dto.HomeCycleDto
import com.billfolder.android.data.dto.HomeExpenseBreakdownDto
import com.billfolder.android.data.dto.HomeIncomeBreakdownDto
import com.billfolder.android.data.dto.HomeResponse
import com.billfolder.android.data.dto.CycleResponse
import com.billfolder.android.data.dto.DailyExpenseResponse
import com.billfolder.android.data.dto.SavingsAccountResponse
import com.billfolder.android.data.repository.AuthRepository
import com.billfolder.android.data.repository.CyclesRepository
import com.billfolder.android.data.repository.DailyExpensesRepository
import com.billfolder.android.data.repository.HomeRepository
import com.billfolder.android.data.repository.SavingsRepository
import com.billfolder.android.data.sync.DataChangeNotifier
import com.billfolder.android.testutil.FakeBillFolderApi
import com.billfolder.android.testutil.MainDispatcherRule
import io.mockk.mockk
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = FakeBillFolderApi()
    private val notifier = DataChangeNotifier()
    private val homeRepo = HomeRepository(api)
    private val savingsRepo = SavingsRepository(api, notifier)
    private val cyclesRepo = CyclesRepository(api, notifier)
    private val dailyExpensesRepo = DailyExpensesRepository(api, notifier)

    // HomeViewModel só usa authRepository dentro de logout(), que estes testes
    // nunca exercitam. AuthRepository é uma classe final que exige TokenStorage
    // (e portanto um android Context), inviável num unit test JVM puro — então
    // usamos um mock relaxed só pra satisfazer o parâmetro do construtor.
    private val authRepo: AuthRepository = mockk(relaxed = true)

    // ------------------------------------------------------------------------
    // Factory helpers (mínimos válidos, determinísticos — sem LocalDate.now())
    // ------------------------------------------------------------------------

    private fun home(cycleId: String, label: String = "Ciclo $cycleId") = HomeResponse(
        cycle = HomeCycleDto(
            id = cycleId,
            startDate = "2026-06-01",
            endDate = "2026-06-30",
            label = label,
        ),
        balance = HomeBalanceDto(
            checkingAccountsTotal = 1000.0,
            expectedIncome = 500.0,
            receivedIncome = 300.0,
            expectedExpenses = 400.0,
            paidExpenses = 200.0,
            expectedCardStatements = 100.0,
            dailyExpensesSpent = 50.0,
            remaining = 750.0,
        ),
        incomeBreakdown = HomeIncomeBreakdownDto(expected = 2, received = 1, late = 0, notOccurred = 1),
        expenseBreakdown = HomeExpenseBreakdownDto(pending = 1, overdue = 0, paid = 2),
        upcomingExpenses = emptyList(),
        cardStatementsInCycle = emptyList(),
    )

    private fun cycle(id: String, startDate: String) = CycleResponse(
        id = id,
        startDate = startDate,
        endDate = startDate.dropLast(2) + "28",
        label = "Ciclo $id",
        isRecurrenceGenerated = false,
        isCurrent = false,
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z",
    )

    private fun savingsAccount(id: String) = SavingsAccountResponse(
        id = id,
        checkingAccountId = "chk-$id",
        bankName = "Banco",
        branch = "0001",
        accountNumber = "123",
        initialBalance = 0.0,
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z",
    )

    private fun http404() = HttpException(Response.error<Any>(404, ResponseBody.create(null, "")))

    private fun http500() = HttpException(Response.error<Any>(500, ResponseBody.create(null, "")))

    private fun daily(id: String, date: String) = DailyExpenseResponse(
        id = id, date = date, label = "avulsa $id", amount = 10.0,
        categoryId = "cat", categoryName = "Cat", accountId = "acc", accountName = "Conta",
        createdAt = "2026-06-01T00:00:00Z", updatedAt = "2026-06-01T00:00:00Z",
    )

    private fun viewModel() = HomeViewModel(homeRepo, authRepo, savingsRepo, cyclesRepo, dailyExpensesRepo, notifier)

    // ------------------------------------------------------------------------
    // Initial load
    // ------------------------------------------------------------------------

    @Test
    fun `carrega Content com os dados da home`() {
        api.onGetHome = { home("c1") }
        api.savingsAccounts = listOf(savingsAccount("s1"))
        api.cycles = listOf(cycle("c1", "2026-06-01"))

        val state = viewModel().state.value

        assertTrue(state is HomeUiState.Content)
        state as HomeUiState.Content
        assertEquals("c1", state.data.cycle.id)
        assertTrue(state.hasAnySavingsAccount)
        assertEquals(1, state.cycles.size)
        assertFalse(state.isRefreshing)
        assertFalse(state.isSwitchingCycle)
    }

    @Test
    fun `carrega recentDailyExpenses do ciclo ordenadas desc`() {
        api.onGetHome = { home("c1") }
        api.cycles = listOf(cycle("c1", "2026-06-01"))
        api.dailyExpenses = listOf(daily("d1", "2026-06-05"), daily("d2", "2026-06-20"))

        val state = viewModel().state.value as HomeUiState.Content

        assertEquals(listOf("d2", "d1"), state.recentDailyExpenses.map { it.id })
    }

    @Test
    fun `falha ao listar avulsas nao derruba a Home`() {
        api.onGetHome = { home("c1") }
        api.cycles = listOf(cycle("c1", "2026-06-01"))
        api.onGetDailyExpenses = { _, _, _ -> throw http500() }

        val state = viewModel().state.value

        assertTrue(state is HomeUiState.Content)
        state as HomeUiState.Content
        assertTrue(state.recentDailyExpenses.isEmpty())
    }

    @Test
    fun `sem poupanca hasAnySavingsAccount fica false`() {
        api.onGetHome = { home("c1") }
        api.savingsAccounts = emptyList()
        api.cycles = listOf(cycle("c1", "2026-06-01"))

        val state = viewModel().state.value as HomeUiState.Content

        assertFalse(state.hasAnySavingsAccount)
    }

    // ------------------------------------------------------------------------
    // NoCycle / Error paths
    // ------------------------------------------------------------------------

    @Test
    fun `home 404 cai em NoCycle`() {
        api.onGetHome = { throw http404() }

        assertTrue(viewModel().state.value is HomeUiState.NoCycle)
    }

    @Test
    fun `home HTTP nao-404 cai em Error`() {
        api.onGetHome = { throw http500() }

        assertTrue(viewModel().state.value is HomeUiState.Error)
    }

    @Test
    fun `home IOException cai em Error`() {
        api.onGetHome = { throw IOException("offline") }

        assertTrue(viewModel().state.value is HomeUiState.Error)
    }

    // ------------------------------------------------------------------------
    // pullRefresh
    // ------------------------------------------------------------------------

    @Test
    fun `pullRefresh atualiza in-place sem virar Loading`() {
        api.onGetHome = { home("c1", label = "antigo") }
        api.savingsAccounts = emptyList()
        api.cycles = listOf(cycle("c1", "2026-06-01"))
        val vm = viewModel()
        assertEquals("antigo", (vm.state.value as HomeUiState.Content).data.cycle.label)

        api.onGetHome = { home("c1", label = "novo") }
        api.savingsAccounts = listOf(savingsAccount("s1"))
        vm.pullRefresh()

        val state = vm.state.value
        assertTrue(state is HomeUiState.Content)
        state as HomeUiState.Content
        assertEquals("novo", state.data.cycle.label)
        assertTrue(state.hasAnySavingsAccount)
        assertFalse(state.isRefreshing)
    }

    // ------------------------------------------------------------------------
    // Cycle navigation
    // ------------------------------------------------------------------------

    @Test
    fun `goToPreviousCycle e goToNextCycle refazem fetch e trocam o ciclo`() {
        api.onGetHome = { cycleId -> home(cycleId ?: "c2") }
        api.savingsAccounts = emptyList()
        api.cycles = listOf(
            cycle("c1", "2026-05-01"),
            cycle("c2", "2026-06-01"),
            cycle("c3", "2026-07-01"),
        )
        // Ciclo atual = c2 (o do meio) — via getHome default (cycleId null → "c2").
        val vm = viewModel()
        assertEquals("c2", (vm.state.value as HomeUiState.Content).data.cycle.id)

        vm.goToPreviousCycle()
        assertEquals("c1", (vm.state.value as HomeUiState.Content).data.cycle.id)

        vm.goToNextCycle()
        assertEquals("c2", (vm.state.value as HomeUiState.Content).data.cycle.id)

        vm.goToNextCycle()
        assertEquals("c3", (vm.state.value as HomeUiState.Content).data.cycle.id)
    }

    @Test
    fun `navegacao no extremo e no-op`() {
        api.onGetHome = { cycleId -> home(cycleId ?: "c1") }
        api.savingsAccounts = emptyList()
        api.cycles = listOf(
            cycle("c1", "2026-05-01"),
            cycle("c2", "2026-06-01"),
        )
        // Atual = c1 (primeiro) → previous é no-op.
        val vm = viewModel()
        assertEquals("c1", (vm.state.value as HomeUiState.Content).data.cycle.id)

        vm.goToPreviousCycle()

        assertEquals("c1", (vm.state.value as HomeUiState.Content).data.cycle.id)
    }

    // ------------------------------------------------------------------------
    // Data-change observer
    // ------------------------------------------------------------------------

    @Test
    fun `mudanca de dados global dispara refetch in-place`() {
        api.onGetHome = { home("c1", label = "v1") }
        api.savingsAccounts = emptyList()
        api.cycles = listOf(cycle("c1", "2026-06-01"))
        val vm = viewModel()
        assertEquals("v1", (vm.state.value as HomeUiState.Content).data.cycle.label)

        api.onGetHome = { home("c1", label = "v2") }
        notifier.notifyDataChanged()

        assertEquals("v2", (vm.state.value as HomeUiState.Content).data.cycle.label)
    }
}
