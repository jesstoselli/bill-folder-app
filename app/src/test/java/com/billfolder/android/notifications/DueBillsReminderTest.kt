package com.billfolder.android.notifications

import com.billfolder.android.data.api.BillFolderApi
import com.billfolder.android.data.auth.TokenStorage
import com.billfolder.android.data.dto.CardStatementResponse
import com.billfolder.android.data.dto.ExpenseResponse
import com.billfolder.android.data.repository.DueBillsRepository
import com.billfolder.android.testutil.FakeBillFolderApi
import com.billfolder.android.testutil.FakeDueBillsScheduler
import com.billfolder.android.testutil.FakeReminderPreferences
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

class DueBillsReminderTest {

    private val today = LocalDate.of(2026, 10, 5)
    private val api = FakeBillFolderApi()
    private val preferences = FakeReminderPreferences(enabled = true)
    private val scheduler = FakeDueBillsScheduler()
    private val shown = mutableListOf<DueBillsMessage>()
    private val tokens: TokenStorage = mockk {
        coEvery { getAccessToken() } returns "token"
    }

    private fun reminder() = DueBillsReminder(
        repository = DueBillsRepository(api),
        preferences = preferences,
        tokens = tokens,
        notifier = { shown += it },
        scheduler = scheduler,
    )

    private fun expense(label: String, amount: Double, dueDate: String = "2026-10-05", status: String = "pending") =
        ExpenseResponse(
            id = label,
            dueDate = dueDate,
            label = label,
            expectedAmount = amount,
            status = status,
            categoryId = "cat",
            categoryName = "Casa",
            createdAt = "2026-10-01T00:00:00Z",
            updatedAt = "2026-10-01T00:00:00Z",
        )

    private fun statement(card: String, amount: Double, status: String = "closed") = CardStatementResponse(
        id = card,
        cardId = card,
        cardName = card,
        periodStart = "2026-09-01",
        periodEnd = "2026-09-30",
        dueDate = "2026-10-05",
        status = status,
        totalAmount = amount,
        installmentsCount = 1,
        createdAt = "2026-10-01T00:00:00Z",
        updatedAt = "2026-10-01T00:00:00Z",
    )

    @Test
    fun `junta despesas e faturas do dia num aviso so, sem as pagas`() = runTest {
        api.expenses = listOf(
            expense("Luz", 180.0),
            expense("Aluguel", 1500.0),
            expense("Água", 90.0, status = "paid"),
            expense("Internet", 120.0, dueDate = "2026-10-06"),
        )
        api.cardStatements = listOf(statement("Nubank", 640.0), statement("Inter", 300.0, status = "paid"))

        reminder().run(today)

        assertEquals(1, shown.size)
        assertEquals("📅 Você tem 3 contas a vencer hoje", shown.single().title)
        assertTrue(shown.single().body.startsWith("Aluguel, Fatura Nubank e Luz"))
        assertEquals(today, preferences.lastCheckedDate.value)
        assertEquals(listOf<LocalDate?>(today), scheduler.scheduled)
    }

    @Test
    fun `dia sem contas marca como conferido sem avisar`() = runTest {
        reminder().run(today)

        assertTrue(shown.isEmpty())
        assertEquals(today, preferences.lastCheckedDate.value)
    }

    @Test
    fun `nao avisa duas vezes no mesmo dia`() = runTest {
        api.expenses = listOf(expense("Luz", 180.0))
        preferences.lastCheckedDate.value = today

        reminder().run(today)

        assertTrue(shown.isEmpty())
    }

    @Test
    fun `desligado ou deslogado nao busca nada`() = runTest {
        api.expenses = listOf(expense("Luz", 180.0))
        preferences.dueRemindersEnabled.value = false
        reminder().run(today)

        preferences.dueRemindersEnabled.value = true
        coEvery { tokens.getAccessToken() } returns null
        reminder().run(today)

        assertTrue(shown.isEmpty())
        assertNull(preferences.lastCheckedDate.value)
    }

    @Test
    fun `falha de rede nao marca o dia, pra tentar de novo`() = runTest {
        val failing: BillFolderApi = mockk {
            coEvery { getExpenses(any(), any(), any(), any()) } throws IOException("offline")
            coEvery { getCardStatements(any(), any(), any()) } returns emptyList()
        }
        val reminder = DueBillsReminder(DueBillsRepository(failing), preferences, tokens, { shown += it }, scheduler)

        val result = runCatching { reminder.run(today) }

        assertTrue(result.exceptionOrNull() is IOException)
        assertNull(preferences.lastCheckedDate.value)
        assertTrue(scheduler.scheduled.isEmpty())
    }
}
