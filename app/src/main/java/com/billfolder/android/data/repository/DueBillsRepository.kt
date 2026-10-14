package com.billfolder.android.data.repository

import com.billfolder.android.data.api.BillFolderApi
import com.billfolder.android.notifications.DueBill
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * O que vence num dia e ainda não foi pago: despesas e faturas de cartão.
 * Faturas não viram despesa no backend, por isso são duas consultas.
 */
@Singleton
class DueBillsRepository @Inject constructor(
    private val api: BillFolderApi,
) {
    suspend fun dueOn(date: LocalDate): List<DueBill> = coroutineScope {
        val day = date.toString()
        val expenses = async { api.getExpenses(from = day, to = day) }
        val statements = async { api.getCardStatements(from = day, to = day) }

        val expenseBills = expenses.await()
            .filter { it.dueDate == day && !it.status.equals("paid", ignoreCase = true) }
            .map { expense ->
                // Provisionada: o que resta reservar, como na Home.
                val amount = if (expense.occurrencesTotal != null) {
                    (expense.expectedAmount - expense.paidToDate).coerceAtLeast(0.0)
                } else {
                    expense.expectedAmount
                }
                DueBill(label = expense.label, amount = amount)
            }
        val statementBills = statements.await()
            .filter {
                it.dueDate == day &&
                    !it.status.equals("paid", ignoreCase = true) &&
                    it.totalAmount > 0.0
            }
            .map { DueBill(label = "Fatura ${it.cardName}", amount = it.totalAmount) }

        (expenseBills + statementBills).sortedByDescending { it.amount }
    }
}
