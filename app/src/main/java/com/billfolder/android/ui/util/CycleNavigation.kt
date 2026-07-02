package com.billfolder.android.ui.util

import com.billfolder.android.data.dto.CycleResponse

/**
 * Utilitários puros pra navegação prev/next entre ciclos. Compartilhado
 * entre HomeViewModel, ExpensesViewModel, IncomeViewModel, CardsViewModel,
 * DailyExpensesViewModel e SavingsViewModel — todas essas telas têm o
 * mesmo CycleNavigator no topo com setinhas ⬅️ ➡️.
 *
 * A lista de ciclos vinda do backend sai ordenada por startDate asc, mas
 * ordenamos de novo aqui pra ser defensivo — outros callers podem trazer
 * a lista em outra ordem.
 */

enum class CycleDirection { PREVIOUS, NEXT }

/**
 * Retorna o ciclo adjacente (anterior ou próximo) na lista, ou null se
 * o ciclo atual já é o extremo (primeiro/último).
 *
 * Semântica de "adjacente": ordena a lista por startDate ISO ascending
 * (strings ISO comparam lexicograficamente), acha o índice do currentId
 * e devolve o vizinho.
 *
 * currentId ausente na lista → null (ciclo inválido, não navega).
 */
fun resolveAdjacentCycle(
    cycles: List<CycleResponse>,
    currentId: String,
    direction: CycleDirection,
): CycleResponse? {
    if (cycles.isEmpty()) return null
    val sorted = cycles.sortedBy { it.startDate }
    val index = sorted.indexOfFirst { it.id == currentId }
    if (index < 0) return null

    val target = when (direction) {
        CycleDirection.PREVIOUS -> index - 1
        CycleDirection.NEXT     -> index + 1
    }
    return sorted.getOrNull(target)
}
