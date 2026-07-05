package com.billfolder.android.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Os formatters usam java.text/java.time com locale pt-BR. O caractere de
 * espaço entre símbolo e número varia entre versões de JVM (espaço normal,
 * NBSP, narrow NBSP), então normalizamos todo whitespace antes de comparar
 * pra não deixar o teste frágil — o que importa é agrupamento (.) e decimal (,).
 */
class FormattersTest {

    private fun String.stripSpaces() = replace(Regex("\\s+"), "").replace(" ", "").replace(" ", "")

    @Test
    fun `formatBrl usa ponto de milhar e virgula decimal`() {
        assertEquals("R$1.234,56", formatBrl(1234.56).stripSpaces())
    }

    @Test
    fun `formatBrl lida com valor negativo`() {
        val out = formatBrl(-300.0).stripSpaces()
        assertTrue("esperava sinal negativo em '$out'", out.contains("-"))
        assertTrue("esperava 300,00 em '$out'", out.contains("300,00"))
    }

    @Test
    fun `formatBrl zera com duas casas`() {
        assertEquals("R$0,00", formatBrl(0.0).stripSpaces())
    }

    @Test
    fun `formatShortDate formata data ISO valida`() {
        val out = formatShortDate("2026-04-29")
        assertTrue("esperava o dia 29 em '$out'", out.startsWith("29"))
    }

    @Test
    fun `formatShortDate cai no input quando a data e invalida`() {
        assertEquals("não-é-data", formatShortDate("não-é-data"))
    }

    @Test
    fun `formatCycleRange junta inicio e fim com travessao`() {
        val out = formatCycleRange("2026-04-25", "2026-05-25")
        assertTrue("esperava separador — em '$out'", out.contains("—"))
        assertTrue("esperava os dias 25 em '$out'", out.contains("25"))
    }

    @Test
    fun `formatCycleRange cai no input quando as datas sao invalidas`() {
        val out = formatCycleRange("aaa", "bbb")
        assertTrue(out.contains("aaa"))
        assertTrue(out.contains("bbb"))
    }
}
