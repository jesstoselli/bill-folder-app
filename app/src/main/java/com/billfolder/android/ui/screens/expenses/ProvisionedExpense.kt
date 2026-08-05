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

/** true quando a despesa veio de uma recorrência (semanal provisionada OU
 *  mensal comum) — tem template. Usado pra oferecer o escopo no delete. */
fun ExpenseResponse.isRecurring(): Boolean = templateId != null

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
 * nunca negativo. É o "reservado que resta" do mês.
 */
fun ExpenseResponse.remainingProvisioned(): Double =
    (expectedAmount - paidToDate).coerceAtLeast(0.0)

/**
 * Valor a exibir como número PRINCIPAL da row de despesa:
 *  - paga → o realizado (actualAmount, ou expectedAmount se não houver);
 *  - provisionada em andamento → o RESERVADO que ainda resta
 *    (expectedAmount − paidToDate), pra refletir o que falta provisionar no
 *    mês conforme as baixas vão sendo dadas (o total cheio do mês vira
 *    contexto no subtítulo "R$X no mês");
 *  - demais (normal pendente/atrasada) → o expectedAmount.
 */
fun ExpenseResponse.displayAmount(): Double = when {
    status.equals("paid", ignoreCase = true) -> actualAmount ?: expectedAmount
    isProvisionedInProgress() -> remainingProvisioned()
    else -> expectedAmount
}
