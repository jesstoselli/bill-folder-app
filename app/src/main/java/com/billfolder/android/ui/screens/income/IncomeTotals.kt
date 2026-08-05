package com.billfolder.android.ui.screens.income

import com.billfolder.android.data.dto.IncomeEntryResponse

/**
 * Totais da tela "recebimentos". Puros (sem Compose) pra serem testáveis —
 * a matemática de dinheiro não pode viver só dentro do composable.
 *
 * Regra central: uma entry JÁ RECEBIDA vale o valor que REALMENTE entrou
 * (actualAmount, com fallback pro expectedAmount se nulo), não o esperado
 * original. Sem isso, uma renda reduzida no mês (ex: férias) continuava
 * contando cheia no total esperado — mesmo a linha da lista já mostrando o
 * valor reduzido. Espelha IncomeEntryRow.
 */

/** Valor efetivo da entry: recebida → actualAmount ?: expectedAmount; senão → expectedAmount. */
fun IncomeEntryResponse.effectiveAmount(): Double =
    if (status.equals("received", ignoreCase = true)) {
        actualAmount ?: expectedAmount
    } else {
        expectedAmount
    }

/** Total esperado no ciclo pelo valor efetivo de cada entry; exclui notOccurred. */
fun incomeExpectedTotal(entries: List<IncomeEntryResponse>): Double =
    entries
        .filterNot { it.status.equals("notOccurred", ignoreCase = true) }
        .sumOf { it.effectiveAmount() }

/** Total efetivamente recebido no ciclo (só entries received). */
fun incomeReceivedTotal(entries: List<IncomeEntryResponse>): Double =
    entries
        .filter { it.status.equals("received", ignoreCase = true) }
        .sumOf { it.actualAmount ?: it.expectedAmount }
