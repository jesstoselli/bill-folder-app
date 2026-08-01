package com.billfolder.android.data.repository

import com.billfolder.android.data.dto.CardEntryRecurrenceResponse
import com.billfolder.android.data.dto.CardEntryResponse
import com.billfolder.android.data.dto.CardStatementResponse
import com.billfolder.android.data.dto.CreateCardEntryRecurrenceRequest
import com.billfolder.android.data.dto.CreateCycleRequest
import com.billfolder.android.data.dto.CreateExpenseRecurrenceRequest
import com.billfolder.android.data.dto.CycleResponse
import com.billfolder.android.data.dto.ExpenseRecurrenceResponse
import com.billfolder.android.data.dto.ExpenseResponse
import com.billfolder.android.data.dto.PayOccurrenceRequest
import com.billfolder.android.data.dto.PayCardStatementRequest
import com.billfolder.android.data.dto.UpdateCardSubscriptionAmountRequest
import com.billfolder.android.data.sync.DataChangeNotifier
import com.billfolder.android.testutil.FakeBillFolderApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * Garante que todo repository sinaliza o DataChangeNotifier após um write
 * bem-sucedido (é o que faz o refresh cross-screen funcionar) e NÃO sinaliza
 * quando o write falha.
 */
class RepositoryNotifyTest {

    private val api = FakeBillFolderApi()
    private val notifier = DataChangeNotifier()

    /** Executa [block] e verifica que a versão do notifier avançou de 1. */
    private suspend fun assertBumps(block: suspend () -> Unit) {
        val before = notifier.changes.value
        block()
        assertEquals(before + 1, notifier.changes.value)
    }

    private fun statement(status: String) = CardStatementResponse(
        id = "statement-1",
        cardId = "card-1",
        cardName = "Nubank",
        periodStart = "2026-07-01",
        periodEnd = "2026-07-31",
        dueDate = "2026-08-08",
        status = status,
        paidDate = if (status == "paid") "2026-08-01" else null,
        actualAmount = if (status == "paid") 800.0 else null,
        paidFromAccountId = if (status == "paid") "account-1" else null,
        paidFromAccountName = if (status == "paid") "Conta principal" else null,
        totalAmount = 800.0,
        installmentsCount = 3,
        linkedExpenseId = if (status == "paid") "expense-1" else null,
        createdAt = "2026-07-01T00:00:00Z",
        updatedAt = "2026-08-01T00:00:00Z",
    )

    @Test
    fun `payCardStatement dispara o bump e propaga request`() = runTest {
        val expected = statement(status = "paid")
        val request = PayCardStatementRequest(
            paidDate = "2026-08-01",
            actualAmount = 800.0,
            paidFromAccountId = "account-1",
        )
        api.onPayCardStatement = { _, _ -> expected }
        val before = notifier.changes.value

        val result = CardStatementsRepository(api, notifier).pay("statement-1", request)

        assertEquals(expected, result)
        assertEquals(before + 1, notifier.changes.value)
        assertEquals(listOf("statement-1" to request), api.payCardStatementCalls)
    }

    @Test
    fun `get de despesa retorna detalhe sem disparar o bump`() = runTest {
        val expected = ExpenseResponse(
            id = "expense-1", dueDate = "2026-08-10", label = "Aluguel", expectedAmount = 1200.0,
            status = "pending", categoryId = "housing", categoryName = "Moradia",
            createdAt = "2026-07-01T00:00:00Z", updatedAt = "2026-07-01T00:00:00Z",
        )
        api.onGetExpense = { expected }
        val before = notifier.changes.value

        val result = ExpensesRepository(api, notifier).get("expense-1")

        assertEquals(expected, result)
        assertEquals(before, notifier.changes.value)
        assertEquals(listOf("expense-1"), api.getExpenseCalls)
    }

    @Test
    fun `delete em cada repo com delete dispara o bump`() = runTest {
        assertBumps { CardsRepository(api, notifier).deleteEntry("x") }
        assertBumps { CheckingAccountsRepository(api, notifier).deleteAccount("x") }
        assertBumps { CycleAdjustmentsRepository(api, notifier).delete("x") }
        assertBumps { DailyExpensesRepository(api, notifier).delete("x") }
        assertBumps { ExpensesRepository(api, notifier).delete("x") }
        assertBumps { IncomeRepository(api, notifier).deleteEntry("x") }
        assertBumps { SavingsRepository(api, notifier).deleteTransaction("x") }
    }

    @Test
    fun `createCycle dispara o bump`() = runTest {
        api.onCreateCycle = {
            CycleResponse(
                id = "c1", startDate = "2026-07-01", endDate = "2026-07-31", label = "julho/2026",
                isRecurrenceGenerated = false, isCurrent = true,
                createdAt = "2026-07-01T00:00:00Z", updatedAt = "2026-07-01T00:00:00Z",
            )
        }
        assertBumps {
            CyclesRepository(api, notifier).create(
                CreateCycleRequest(startDate = "2026-07-01", endDate = "2026-07-31", label = "julho/2026"),
            )
        }
    }

    @Test
    fun `payOccurrence dispara o bump`() = runTest {
        api.onPayOccurrence = { id, _ ->
            ExpenseResponse(
                id = id, dueDate = "2026-07-10", label = "fisio", expectedAmount = 400.0,
                status = "pending", categoryId = "cat", categoryName = "Saúde",
                occurrenceAmount = 100.0, occurrencesTotal = 4, occurrencesPaid = 1,
                paidToDate = 100.0,
                createdAt = "2026-07-01T00:00:00Z", updatedAt = "2026-07-10T00:00:00Z",
            )
        }
        assertBumps {
            ExpensesRepository(api, notifier).payOccurrence(
                id = "e1",
                request = PayOccurrenceRequest(amount = 100.0),
            )
        }
    }

    @Test
    fun `createExpenseRecurrence dispara o bump`() = runTest {
        api.onCreateExpenseRecurrence = {
            ExpenseRecurrenceResponse(
                id = "r1", defaultLabel = "fisio", defaultAmount = 100.0,
                defaultCategoryId = "cat", frequency = "weekly", weekday = 3,
                startDate = "2026-07-01",
                createdAt = "2026-07-01T00:00:00Z", updatedAt = "2026-07-01T00:00:00Z",
            )
        }
        assertBumps {
            ExpenseRecurrencesRepository(api, notifier).create(
                CreateExpenseRecurrenceRequest(
                    defaultLabel = "fisio", defaultAmount = 100.0, defaultCategoryId = "cat",
                    frequency = "weekly", weekday = 3, startDate = "2026-07-01",
                ),
            )
        }
    }

    @Test
    fun `createCardEntryRecurrence dispara o bump`() = runTest {
        api.onCreateCardEntryRecurrence = {
            CardEntryRecurrenceResponse(
                id = "cr1", cardId = "card1", cardName = "Itaú", defaultLabel = "Netflix",
                defaultAmount = 55.9, defaultCategoryId = "cat", defaultCategoryName = "Lazer",
                dayOfMonth = 10, startDate = "2026-07-01", isActive = true,
                createdAt = "2026-07-01T00:00:00Z", updatedAt = "2026-07-01T00:00:00Z",
            )
        }
        assertBumps {
            CardEntryRecurrencesRepository(api, notifier).create(
                CreateCardEntryRecurrenceRequest(
                    cardId = "card1", defaultLabel = "Netflix", defaultAmount = 55.9,
                    defaultCategoryId = "cat", dayOfMonth = 10, startDate = "2026-07-01",
                ),
            )
        }
    }

    @Test
    fun `updateSubscriptionAmount dispara o bump`() = runTest {
        api.onUpdateCardSubscriptionAmount = { id, _ ->
            CardEntryResponse(
                id = id, cardId = "card1", cardName = "Itaú", purchaseDate = "2026-07-10",
                label = "Netflix", totalAmount = 59.9, installmentsCount = 1,
                categoryId = "cat", categoryName = "Lazer",
                createdAt = "2026-07-01T00:00:00Z", updatedAt = "2026-07-10T00:00:00Z",
            )
        }
        assertBumps {
            CardsRepository(api, notifier).updateSubscriptionAmount(
                id = "ce1",
                request = UpdateCardSubscriptionAmountRequest(amount = 59.9, scope = "thisAndFollowing"),
            )
        }
    }

    @Test
    fun `deletes com scope disparam o bump e propagam o scope`() = runTest {
        assertBumps { CardsRepository(api, notifier).deleteEntry("x", scope = "this_and_following") }
        assertBumps { ExpensesRepository(api, notifier).delete("y", scope = "this_and_following") }
        assertEquals(listOf("this_and_following"), api.deleteCardEntryScopes)
        assertEquals(listOf("this_and_following"), api.deleteExpenseScopes)
    }

    @Test
    fun `write que falha NAO dispara o bump`() = runTest {
        // Delete não-2xx → repo converte em HttpException; nada mudou de fato.
        api.deleteResult = Response.error(500, okhttp3.ResponseBody.create(null, ""))
        val before = notifier.changes.value

        var threw = false
        try {
            ExpensesRepository(api, notifier).delete("x")
        } catch (e: HttpException) {
            threw = true
        }

        assertTrue(threw)
        assertEquals(before, notifier.changes.value)
    }

    @Test
    fun `delete de ajuste que falha lanca e NAO dispara o bump`() = runTest {
        // Endpoint retorna Response<Unit>: um 500 NÃO lança sozinho no Retrofit,
        // então o repo precisa checar isSuccessful (como os demais). Senão a
        // falha é engolida E o bus dispara indevidamente.
        api.deleteResult = Response.error(500, okhttp3.ResponseBody.create(null, ""))
        val before = notifier.changes.value

        var threw = false
        try {
            CycleAdjustmentsRepository(api, notifier).delete("x")
        } catch (e: HttpException) {
            threw = true
        }

        assertTrue(threw)
        assertEquals(before, notifier.changes.value)
    }
}
