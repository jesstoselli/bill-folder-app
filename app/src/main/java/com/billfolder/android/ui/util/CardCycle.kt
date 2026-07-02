package com.billfolder.android.ui.util

import java.time.LocalDate

/**
 * Porta client-side do BillFolder.Application.UseCases.Cards.CardCycleCalculator.
 * Mesma lógica, mas em Kotlin, pra evitar round-trip só pra calcular o período
 * da fatura de uma compra.
 *
 * Usado pela CardsScreen pra filtrar entries pela fatura que vence dentro do
 * ciclo BillFolder atual, em vez de filtrar pela purchaseDate diretamente
 * (que ignora o closingDay do cartão).
 *
 * Duas semânticas de "compras no cartão nesse mês":
 *   - Filtrar por purchaseDate no ciclo → compras feitas no mês (irrelevante
 *     pro user; se comprei dia 18 num cartão que fecha dia 17, essa compra
 *     não afeta a fatura desse mês).
 *   - Filtrar por dueDate no ciclo → compras que compõem a fatura vencendo
 *     nesse mês (o que o user REALMENTE quer ver).
 *
 * Este helper implementa a 2ª semântica.
 */

/**
 * Período da fatura (dueDate) que vai receber uma compra feita em
 * <code>purchaseDate</code>. Retorna a tripla (periodStart, periodEnd,
 * dueDate).
 *
 * Regras:
 *  - Se purchaseDate.day &lt;= closingDay: fatura fecha no MESMO mês da compra.
 *  - Se purchaseDate.day &gt; closingDay: fatura fecha no MÊS SEGUINTE.
 *
 *  - dueDay &gt; closingDay: vence no MESMO mês do fechamento (fechou dia 5, vence dia 15).
 *  - dueDay &lt;= closingDay: vence no MÊS SEGUINTE ao fechamento (fechou dia 25, vence dia 10).
 *
 * Clampeamento: se closingDay/dueDay excedem os dias do mês (ex: 31 em fev),
 * usamos o último dia real do mês (28/29).
 */
data class StatementPeriod(
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val dueDate: LocalDate,
)

fun computeStatementForPurchase(
    purchaseDate: LocalDate,
    closingDay: Int,
    dueDay: Int,
): StatementPeriod {
    val (closeYear, closeMonth) = if (purchaseDate.dayOfMonth <= closingDay) {
        purchaseDate.year to purchaseDate.monthValue
    } else {
        addMonths(purchaseDate.year, purchaseDate.monthValue, 1)
    }

    val periodEnd = clampDay(closeYear, closeMonth, closingDay)
    val periodStart = computePeriodStart(periodEnd, closingDay)
    val dueDate = computeDueDate(periodEnd, closingDay, dueDay)

    return StatementPeriod(periodStart, periodEnd, dueDate)
}

// --- helpers privados ---

private fun computePeriodStart(periodEnd: LocalDate, closingDay: Int): LocalDate {
    val (prevYear, prevMonth) = addMonths(periodEnd.year, periodEnd.monthValue, -1)
    val previousClose = clampDay(prevYear, prevMonth, closingDay)
    return previousClose.plusDays(1)
}

private fun computeDueDate(
    periodEnd: LocalDate,
    closingDay: Int,
    dueDay: Int,
): LocalDate {
    return if (dueDay > closingDay) {
        // fechou dia X, vence dia Y do mesmo mês
        clampDay(periodEnd.year, periodEnd.monthValue, dueDay)
    } else {
        // fechou dia X, vence dia Y do mês seguinte
        val (nextYear, nextMonth) = addMonths(periodEnd.year, periodEnd.monthValue, 1)
        clampDay(nextYear, nextMonth, dueDay)
    }
}

private fun clampDay(year: Int, month: Int, day: Int): LocalDate {
    val maxDay = LocalDate.of(year, month, 1).lengthOfMonth()
    val actualDay = minOf(day, maxDay)
    return LocalDate.of(year, month, actualDay)
}

private fun addMonths(year: Int, month: Int, delta: Int): Pair<Int, Int> {
    val totalMonths = year * 12 + (month - 1) + delta
    val newYear = totalMonths / 12
    val newMonth = (totalMonths % 12) + 1
    return newYear to newMonth
}
