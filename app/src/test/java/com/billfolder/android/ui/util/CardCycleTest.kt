package com.billfolder.android.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Caracterização de computeStatementForPurchase — o coração da lógica de
 * faturas do cartão (fechamento → vencimento). Cobre os 4 ramos (compra
 * antes/depois do fechamento, vencimento no mesmo mês / mês seguinte),
 * clamp de dia inexistente e viradas de ano.
 */
class CardCycleTest {

    private fun stmt(purchase: String, closingDay: Int, dueDay: Int): StatementPeriod =
        computeStatementForPurchase(LocalDate.parse(purchase), closingDay, dueDay)

    @Test
    fun `compra antes do fechamento, vencimento no mesmo mes`() {
        // fecha dia 17, vence dia 25 (due > closing → mesmo mês do fechamento)
        val s = stmt("2026-06-10", closingDay = 17, dueDay = 25)
        assertEquals(LocalDate.parse("2026-05-18"), s.periodStart)
        assertEquals(LocalDate.parse("2026-06-17"), s.periodEnd)
        assertEquals(LocalDate.parse("2026-06-25"), s.dueDate)
    }

    @Test
    fun `compra depois do fechamento cai na fatura do mes seguinte`() {
        val s = stmt("2026-06-20", closingDay = 17, dueDay = 25)
        assertEquals(LocalDate.parse("2026-06-18"), s.periodStart)
        assertEquals(LocalDate.parse("2026-07-17"), s.periodEnd)
        assertEquals(LocalDate.parse("2026-07-25"), s.dueDate)
    }

    @Test
    fun `vencimento menor que fechamento vence no mes seguinte`() {
        // fecha dia 25, vence dia 10 (due <= closing → mês seguinte ao fechamento)
        val s = stmt("2026-06-10", closingDay = 25, dueDay = 10)
        assertEquals(LocalDate.parse("2026-05-26"), s.periodStart)
        assertEquals(LocalDate.parse("2026-06-25"), s.periodEnd)
        assertEquals(LocalDate.parse("2026-07-10"), s.dueDate)
    }

    @Test
    fun `fechamento dia 31 em fevereiro clampeia pro ultimo dia do mes`() {
        val s = stmt("2026-02-15", closingDay = 31, dueDay = 10)
        assertEquals(LocalDate.parse("2026-02-01"), s.periodStart)
        assertEquals(LocalDate.parse("2026-02-28"), s.periodEnd)
        assertEquals(LocalDate.parse("2026-03-10"), s.dueDate)
    }

    @Test
    fun `virada de ano quando a compra empurra o fechamento pra janeiro`() {
        val s = stmt("2026-12-20", closingDay = 17, dueDay = 25)
        assertEquals(LocalDate.parse("2026-12-18"), s.periodStart)
        assertEquals(LocalDate.parse("2027-01-17"), s.periodEnd)
        assertEquals(LocalDate.parse("2027-01-25"), s.dueDate)
    }

    @Test
    fun `ptBrMonthYearOf formata mes por extenso em minusculo`() {
        assertEquals("julho/2026", ptBrMonthYearOf(LocalDate.parse("2026-07-25")))
        assertEquals("janeiro/2027", ptBrMonthYearOf(LocalDate.parse("2027-01-01")))
    }
}
