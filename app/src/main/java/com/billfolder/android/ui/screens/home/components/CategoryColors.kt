package com.billfolder.android.ui.screens.home.components

import androidx.compose.ui.graphics.Color
import com.billfolder.android.ui.theme.BfChart1
import com.billfolder.android.ui.theme.BfChart2
import com.billfolder.android.ui.theme.BfChart3
import com.billfolder.android.ui.theme.BfChart4
import com.billfolder.android.ui.theme.BfChart5
import com.billfolder.android.ui.theme.BfChart6
import com.billfolder.android.ui.theme.BfChart7

/**
 * Paleta da pie chart por POSIÇÃO no ranking.
 *
 * O índice 0 é pintado com a cor de maior contraste (BfChart1, verde escuro);
 * conforme o slice fica menos relevante, a cor fica menos saturada. O slot
 * 6 é reservado pro bucket "outros", com cinza claro neutro pra deixar
 * claro que ali há agregação de várias categorias menores.
 *
 * Decisão: não há mapping categoria→cor. Cor depende do tamanho da fatia,
 * não da identidade. Isso dá controle visual previsível em qualquer dataset.
 */
private val ChartPalette = listOf(
    BfChart1, // 0 — verde escuro
    BfChart2, // 1 — verde principal
    BfChart3, // 2 — verde claro
    BfChart4, // 3 — azul escuro
    BfChart5, // 4 — azul claro
    BfChart6, // 5 — cinza médio
)

/** Cor do slice na posição indicada. */
fun colorForRank(index: Int): Color = ChartPalette[index.coerceIn(0, ChartPalette.lastIndex)]

/** Cor reservada do bucket "outros". */
val OthersChartColor: Color = BfChart7
