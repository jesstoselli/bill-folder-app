package com.billfolder.android.ui.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formatadores compartilhados pela UI.
 *
 * Decisão consciente: o backend manda decimal como número JSON, e parseamos
 * como Double. Pra UI isso é suficiente — Double tem 15-17 dígitos significativos,
 * sobra muito pra valores na faixa de "salário+contas". Se um dia precisarmos
 * de exatidão ao centavo (relatório fiscal etc), mudamos pra BigDecimal no
 * DTO via @Serializable(BigDecimalSerializer::class).
 */

private val BR_LOCALE = Locale.Builder().setLanguage("pt").setRegion("BR").build()
private val BR_CURRENCY by lazy { NumberFormat.getCurrencyInstance(BR_LOCALE) }
private val SHORT_DATE = DateTimeFormatter.ofPattern("dd 'de' MMM", BR_LOCALE)
private val CYCLE_DATE = DateTimeFormatter.ofPattern("dd MMM", BR_LOCALE)

/** "1234.56" → "R$ 1.234,56" */
fun formatBrl(value: Double): String = BR_CURRENCY.format(value)

/** "2026-04-29" → "29 de abr" */
fun formatShortDate(isoDate: String): String =
    runCatching { LocalDate.parse(isoDate).format(SHORT_DATE) }.getOrDefault(isoDate)

/** ("2026-04-25", "2026-05-25") → "25 abr — 25 mai" */
fun formatCycleRange(startIso: String, endIso: String): String {
    val start = runCatching { LocalDate.parse(startIso).format(CYCLE_DATE) }.getOrDefault(startIso)
    val end = runCatching { LocalDate.parse(endIso).format(CYCLE_DATE) }.getOrDefault(endIso)
    return "$start — $end"
}
