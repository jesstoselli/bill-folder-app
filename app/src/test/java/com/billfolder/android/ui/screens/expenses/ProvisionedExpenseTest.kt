package com.billfolder.android.ui.screens.expenses

import com.billfolder.android.data.dto.ExpenseResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testa a lógica pura de branching do tap na row de despesa: uma
 * provisionada em andamento abre o fluxo de baixa (PayOccurrenceSheet);
 * uma despesa normal segue o fluxo de pagamento cheio (PayExpenseSheet).
 */
class ProvisionedExpenseTest {

    private fun expense(
        occurrenceAmount: Double? = null,
        occurrencesTotal: Int? = null,
        occurrencesPaid: Int = 0,
        expectedAmount: Double = 400.0,
        paidToDate: Double = 0.0,
        status: String = "pending",
        actualAmount: Double? = null,
    ) = ExpenseResponse(
        id = "e1", dueDate = "2026-07-10", label = "x", expectedAmount = expectedAmount,
        actualAmount = actualAmount,
        status = status, categoryId = "cat", categoryName = "Cat",
        occurrenceAmount = occurrenceAmount, occurrencesTotal = occurrencesTotal,
        occurrencesPaid = occurrencesPaid, paidToDate = paidToDate,
        createdAt = "2026-07-01T00:00:00Z", updatedAt = "2026-07-01T00:00:00Z",
    )

    @Test
    fun `despesa normal nao e provisionada`() {
        val e = expense(occurrenceAmount = null, occurrencesTotal = null)
        assertFalse(e.isProvisioned())
        assertFalse(e.isProvisionedInProgress())
    }

    @Test
    fun `provisionada com ocorrencias restantes esta em andamento`() {
        val e = expense(occurrenceAmount = 100.0, occurrencesTotal = 4, occurrencesPaid = 2)
        assertTrue(e.isProvisioned())
        assertTrue(e.isProvisionedInProgress())
    }

    @Test
    fun `provisionada 100 por cento quitada nao esta em andamento`() {
        val e = expense(occurrenceAmount = 100.0, occurrencesTotal = 4, occurrencesPaid = 4)
        assertTrue(e.isProvisioned())
        assertFalse(e.isProvisionedInProgress())
    }

    @Test
    fun `provisionada sem baixas ainda esta em andamento`() {
        val e = expense(occurrenceAmount = 100.0, occurrencesTotal = 4, occurrencesPaid = 0)
        assertTrue(e.isProvisionedInProgress())
    }

    @Test
    fun `remainingProvisioned e expectedAmount menos paidToDate`() {
        val e = expense(
            occurrenceAmount = 100.0, occurrencesTotal = 4, occurrencesPaid = 2,
            expectedAmount = 400.0, paidToDate = 200.0,
        )
        assertEquals(200.0, e.remainingProvisioned(), 0.0001)
    }

    @Test
    fun `remainingProvisioned nunca e negativo`() {
        val e = expense(
            occurrenceAmount = 100.0, occurrencesTotal = 4, occurrencesPaid = 5,
            expectedAmount = 400.0, paidToDate = 500.0,
        )
        assertEquals(0.0, e.remainingProvisioned(), 0.0001)
    }

    // displayAmount() — valor principal da row.

    @Test
    fun `displayAmount de despesa normal pendente e o expectedAmount`() {
        val e = expense(expectedAmount = 250.0)
        assertEquals(250.0, e.displayAmount(), 0.0001)
    }

    @Test
    fun `displayAmount de despesa paga e o actualAmount`() {
        val e = expense(expectedAmount = 250.0, status = "paid", actualAmount = 230.0)
        assertEquals(230.0, e.displayAmount(), 0.0001)
    }

    @Test
    fun `displayAmount de paga sem actualAmount cai no expectedAmount`() {
        val e = expense(expectedAmount = 250.0, status = "paid", actualAmount = null)
        assertEquals(250.0, e.displayAmount(), 0.0001)
    }

    @Test
    fun `displayAmount de provisionada em andamento e o reservado que resta`() {
        // R$800 no mês, 2 de 4 baixas pagas (R$400) -> restam R$400 reservados.
        val e = expense(
            occurrenceAmount = 200.0, occurrencesTotal = 4, occurrencesPaid = 2,
            expectedAmount = 800.0, paidToDate = 400.0,
        )
        assertEquals(400.0, e.displayAmount(), 0.0001)
    }

    @Test
    fun `displayAmount de provisionada sem baixas e o mes cheio`() {
        val e = expense(
            occurrenceAmount = 200.0, occurrencesTotal = 4, occurrencesPaid = 0,
            expectedAmount = 800.0, paidToDate = 0.0,
        )
        assertEquals(800.0, e.displayAmount(), 0.0001)
    }

    @Test
    fun `displayAmount de provisionada quitada e o realizado`() {
        // 4/4 pagas, status=paid -> mostra o realizado (actualAmount = paidToDate).
        val e = expense(
            occurrenceAmount = 200.0, occurrencesTotal = 4, occurrencesPaid = 4,
            expectedAmount = 800.0, paidToDate = 800.0, status = "paid", actualAmount = 800.0,
        )
        assertEquals(800.0, e.displayAmount(), 0.0001)
    }
}
