package com.billfolder.android.notifications

import com.billfolder.android.ui.util.formatBrl

/** Uma conta que vence no dia e ainda não foi paga. */
data class DueBill(val label: String, val amount: Double)

/** Texto do aviso do dia: [lines] e [summary] aparecem quando o aviso é expandido. */
data class DueBillsMessage(
    val title: String,
    val body: String,
    val lines: List<String> = emptyList(),
    val summary: String? = null,
)

private const val NAMES_IN_BODY = 3

/**
 * Um aviso por dia, nunca um por conta: com uma conta só, o nome dela vai no
 * título; com várias, o título conta quantas são e o corpo lista os nomes.
 */
fun dueBillsMessage(bills: List<DueBill>): DueBillsMessage? {
    if (bills.isEmpty()) return null

    if (bills.size == 1) {
        val bill = bills.single()
        return DueBillsMessage(
            title = "💸 ${bill.label} vence hoje",
            body = "${formatBrl(bill.amount)} — bora pagar pra não esquecer 😉",
        )
    }

    val total = bills.sumOf { it.amount }
    return DueBillsMessage(
        title = "📅 Você tem ${bills.size} contas a vencer hoje",
        body = "${joinNames(bills.map { it.label })} · ${formatBrl(total)} no total 💸",
        lines = bills.map { "• ${it.label} — ${formatBrl(it.amount)}" },
        summary = "${formatBrl(total)} no total",
    )
}

/** "Luz, Internet e Aluguel"; passando de três, "Luz, Internet, Aluguel e mais 2". */
internal fun joinNames(names: List<String>): String = when {
    names.size <= 1 -> names.joinToString()
    names.size <= NAMES_IN_BODY -> names.dropLast(1).joinToString(", ") + " e " + names.last()
    else -> names.take(NAMES_IN_BODY).joinToString(", ") + " e mais ${names.size - NAMES_IN_BODY}"
}
