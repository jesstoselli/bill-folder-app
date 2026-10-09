package com.billfolder.android.ui.screens.home

import com.billfolder.android.data.dto.HomeBalanceDto
import com.billfolder.android.data.dto.HomeCycleDto
import com.billfolder.android.data.dto.HomeExpenseBreakdownDto
import com.billfolder.android.data.dto.HomeIncomeBreakdownDto
import com.billfolder.android.data.dto.HomeResponse
import com.billfolder.android.data.dto.HomeUpcomingExpenseDto
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeOverdueExpensesTest {

    private fun expense(id: String, dueDate: String, status: String) = HomeUpcomingExpenseDto(
        id = id, label = "conta $id", dueDate = dueDate, expectedAmount = 100.0,
        status = status, categoryName = "Cat",
    )

    private fun home(
        upcoming: List<HomeUpcomingExpenseDto> = emptyList(),
        overdue: List<HomeUpcomingExpenseDto> = emptyList(),
    ) = HomeResponse(
        cycle = HomeCycleDto(id = "c1", startDate = "2026-10-01", endDate = "2026-10-31", label = "out"),
        balance = HomeBalanceDto(
            checkingAccountsTotal = 0.0, expectedIncome = 0.0, receivedIncome = 0.0,
            expectedExpenses = 0.0, paidExpenses = 0.0, expectedCardStatements = 0.0,
            dailyExpensesSpent = 0.0, remaining = 0.0,
        ),
        incomeBreakdown = HomeIncomeBreakdownDto(expected = 0, received = 0, late = 0, notOccurred = 0),
        expenseBreakdown = HomeExpenseBreakdownDto(pending = 0, overdue = 0, paid = 0),
        upcomingExpenses = upcoming,
        overdueExpenses = overdue,
        cardStatementsInCycle = emptyList(),
    )

    @Test
    fun `le as atrasadas da lista overdueExpenses`() {
        val data = home(
            upcoming = listOf(expense("u1", "2026-10-20", "pending")),
            overdue = listOf(expense("o2", "2026-10-05", "overdue"), expense("o1", "2026-10-02", "overdue")),
        )

        assertEquals(listOf("o1", "o2"), data.overdueExpenseItems().map { it.id })
    }

    @Test
    fun `backend antigo com atrasadas dentro de upcomingExpenses continua funcionando`() {
        val data = home(
            upcoming = listOf(expense("o1", "2026-10-02", "overdue"), expense("u1", "2026-10-20", "pending")),
        )

        assertEquals(listOf("o1"), data.overdueExpenseItems().map { it.id })
    }

    @Test
    fun `atrasada nas duas listas nao duplica`() {
        val o1 = expense("o1", "2026-10-02", "overdue")

        assertEquals(listOf("o1"), home(upcoming = listOf(o1), overdue = listOf(o1)).overdueExpenseItems().map { it.id })
    }
}
