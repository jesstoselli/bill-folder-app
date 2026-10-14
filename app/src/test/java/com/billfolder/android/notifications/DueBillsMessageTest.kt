package com.billfolder.android.notifications

import com.billfolder.android.ui.util.formatBrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DueBillsMessageTest {

    @Test
    fun `nada vencendo nao gera aviso`() {
        assertNull(dueBillsMessage(emptyList()))
    }

    @Test
    fun `uma conta so leva o nome dela no titulo`() {
        val message = dueBillsMessage(listOf(DueBill("Luz", 180.0)))!!

        assertEquals("💸 Luz vence hoje", message.title)
        assertEquals("${formatBrl(180.0)} — bora pagar pra não esquecer 😉", message.body)
        assertEquals(emptyList<String>(), message.lines)
    }

    @Test
    fun `varias contas viram um aviso so com a contagem e o total`() {
        val message = dueBillsMessage(
            listOf(DueBill("Aluguel", 1500.0), DueBill("Luz", 180.0), DueBill("Internet", 120.0)),
        )!!

        assertEquals("📅 Você tem 3 contas a vencer hoje", message.title)
        assertEquals("Aluguel, Luz e Internet · ${formatBrl(1800.0)} no total 💸", message.body)
        assertEquals(
            listOf(
                "• Aluguel — ${formatBrl(1500.0)}",
                "• Luz — ${formatBrl(180.0)}",
                "• Internet — ${formatBrl(120.0)}",
            ),
            message.lines,
        )
        assertEquals("${formatBrl(1800.0)} no total", message.summary)
    }

    @Test
    fun `nomes passando de tres resumem o resto`() {
        assertEquals("A e B", joinNames(listOf("A", "B")))
        assertEquals("A, B, C e mais 2", joinNames(listOf("A", "B", "C", "D", "E")))
    }
}
