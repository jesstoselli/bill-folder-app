package com.billfolder.android.ui.screens.home

import com.billfolder.android.data.dto.HomeUpcomingExpenseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Helpers puros da row "próximos" da Home pra despesa provisionada: mostrar o
 * reservado que resta (expectedAmount − paidToDate) enquanto em andamento, em
 * vez do total cheio do mês. Espelha o displayAmount() da tela de Despesas,
 * mas sobre o DTO enxuto da Home (só occurrencesTotal/Paid + paidToDate).
 */
class HomeUpcomingExpenseTest {

    private fun upcoming(
        expectedAmount: Double = 400.0,
        occurrencesTotal: Int? = null,
        occurrencesPaid: Int = 0,
        paidToDate: Double = 0.0,
    ) = HomeUpcomingExpenseDto(
        id = "e1", label = "terapia", dueDate = "2026-07-10",
        expectedAmount = expectedAmount, status = "pending", categoryName = "Saúde",
        occurrencesTotal = occurrencesTotal, occurrencesPaid = occurrencesPaid,
        paidToDate = paidToDate,
    )

    @Test
    fun `despesa normal nao e provisionada em andamento`() {
        assertFalse(upcoming(occurrencesTotal = null).isProvisionedInProgress())
    }

    @Test
    fun `provisionada com ocorrencias restantes esta em andamento`() {
        assertTrue(
            upcoming(occurrencesTotal = 4, occurrencesPaid = 2).isProvisionedInProgress(),
        )
    }

    @Test
    fun `provisionada quitada nao esta em andamento`() {
        assertFalse(
            upcoming(occurrencesTotal = 4, occurrencesPaid = 4).isProvisionedInProgress(),
        )
    }

    @Test
    fun `displayAmount de despesa normal e o expectedAmount`() {
        assertEquals(400.0, upcoming(expectedAmount = 400.0).displayAmount(), 0.0001)
    }

    @Test
    fun `displayAmount de provisionada em andamento e o reservado que resta`() {
        // R$800 no mês, 2 de 4 baixas (R$400 pagos) -> restam R$400 reservados.
        val e = upcoming(
            expectedAmount = 800.0, occurrencesTotal = 4, occurrencesPaid = 2,
            paidToDate = 400.0,
        )
        assertEquals(400.0, e.displayAmount(), 0.0001)
    }

    @Test
    fun `displayAmount de provisionada sem baixas e o mes cheio`() {
        val e = upcoming(
            expectedAmount = 800.0, occurrencesTotal = 4, occurrencesPaid = 0,
            paidToDate = 0.0,
        )
        assertEquals(800.0, e.displayAmount(), 0.0001)
    }
}
