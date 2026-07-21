package com.billfolder.android.ui.screens.home

import com.billfolder.android.data.dto.HomeUpcomingExpenseDto

/**
 * Helpers puros da row "próximos" da Home pra despesa provisionada (fisio/
 * terapia semanal). Espelham o comportamento da tela de Despesas
 * (ExpenseResponse.displayAmount), mas sobre o DTO enxuto da Home — que só
 * carrega occurrencesTotal/Paid + paidToDate. Extraídos como funções puras
 * pra ficarem testáveis fora de Composable.
 */

/**
 * true quando a despesa é provisionada E ainda há ocorrências a dar baixa
 * (occurrencesPaid < occurrencesTotal). Enquanto em andamento, a row mostra o
 * reservado que resta em vez do total cheio do mês.
 */
fun HomeUpcomingExpenseDto.isProvisionedInProgress(): Boolean {
    val total = occurrencesTotal ?: return false
    return occurrencesPaid < total
}

/**
 * Valor a exibir como número principal da row da Home:
 *  - provisionada em andamento → o RESERVADO que resta
 *    (expectedAmount − paidToDate, nunca negativo);
 *  - demais → o expectedAmount.
 *
 * (Na Home "próximos" só entram despesas não pagas, então não há ramo de
 * "paga" aqui — diferente do displayAmount da tela de Despesas.)
 */
fun HomeUpcomingExpenseDto.displayAmount(): Double =
    if (isProvisionedInProgress()) {
        (expectedAmount - paidToDate).coerceAtLeast(0.0)
    } else {
        expectedAmount
    }
