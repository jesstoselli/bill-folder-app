package com.billfolder.android.ui.screens.expenses

import com.billfolder.android.data.dto.ExpenseResponse

/**
 * Helpers puros de "despesa provisionada" (fisioterapia semanal e afins).
 *
 * Uma despesa é provisionada quando `occurrencesTotal != null` — nesse caso
 * ela reserva o valor mensal cheio no orçamento e é quitada uma ocorrência
 * por vez ("dar baixa") via PayOccurrenceSheet, nunca pelo fluxo normal de
 * pagamento (que o backend rejeita com provisioned_expense).
 *
 * Extraído como função pura pra a lógica de branching do tap ser testável
 * sem tocar em Composable.
 */

/** true quando a despesa é provisionada (tem template de ocorrências). */
fun ExpenseResponse.isProvisioned(): Boolean = occurrencesTotal != null

/**
 * true quando é provisionada E ainda há ocorrências a dar baixa
 * (occurrencesPaid < occurrencesTotal). É o gatilho pra abrir o
 * PayOccurrenceSheet no tap. Uma provisionada já 100% quitada não tem
 * ação de tap.
 */
fun ExpenseResponse.isProvisionedInProgress(): Boolean {
    val total = occurrencesTotal ?: return false
    return occurrencesPaid < total
}

/**
 * Quanto ainda falta pagar numa provisionada = expectedAmount − paidToDate,
 * nunca negativo. Usado no subtítulo "faltam R$X" da row.
 */
fun ExpenseResponse.remainingProvisioned(): Double =
    (expectedAmount - paidToDate).coerceAtLeast(0.0)
