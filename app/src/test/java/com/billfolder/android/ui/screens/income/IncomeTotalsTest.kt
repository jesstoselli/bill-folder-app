package com.billfolder.android.ui.screens.income

import com.billfolder.android.data.dto.IncomeEntryResponse
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * O total "esperado no mês" deve refletir o valor EFETIVO de cada entry:
 * uma entry já recebida vale o actualAmount (o que realmente entrou), não
 * o expectedAmount original. Reproduz o bug do mês de férias: Stone
 * esperada 7679.10 mas recebida 3979.10 contava cheia no total.
 */
class IncomeTotalsTest {

    private fun entry(
        expected: Double,
        status: String,
        actual: Double? = null,
    ) = IncomeEntryResponse(
        id = "e-$expected-$status",
        expectedAmount = expected,
        actualAmount = actual,
        expectedDate = "2026-08-05",
        status = status,
        createdAt = "2026-08-01T00:00:00Z",
        updatedAt = "2026-08-01T00:00:00Z",
    )

    @Test
    fun `expected entry counts expected amount`() {
        assertEquals(2000.0, incomeExpectedTotal(listOf(entry(2000.0, "expected"))), 0.001)
    }

    @Test
    fun `received entry counts actual not expected`() {
        assertEquals(
            3979.10,
            incomeExpectedTotal(listOf(entry(7679.10, "received", actual = 3979.10))),
            0.001,
        )
    }

    @Test
    fun `received without actual falls back to expected`() {
        assertEquals(
            2000.0,
            incomeExpectedTotal(listOf(entry(2000.0, "received", actual = null))),
            0.001,
        )
    }

    @Test
    fun `notOccurred entry is excluded`() {
        assertEquals(0.0, incomeExpectedTotal(listOf(entry(2000.0, "notOccurred"))), 0.001)
    }

    @Test
    fun `mixed cycle totals effective amounts not stale expected`() {
        val entries = listOf(
            entry(2000.0, "received", actual = 2000.0),
            entry(7679.10, "received", actual = 3979.10),
            entry(2406.08, "expected"),
        )
        // 8385.18, e NÃO 12085.18.
        assertEquals(8385.18, incomeExpectedTotal(entries), 0.001)
        assertEquals(5979.10, incomeReceivedTotal(entries), 0.001)
    }
}
