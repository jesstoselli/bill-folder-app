package com.billfolder.android.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

/**
 * Textura "cash hatching" — linhas finas a 45° que evocam o hatching das
 * notas no logo BillFolder. Usar como detalhe sutil de marca.
 *
 * Specs DS v0.2:
 *  - Linhas a 45°
 *  - Cor: primary com alpha 6-8% (dark) ou 4-5% (light)
 *  - Espaçamento: 8dp
 *  - Espessura: 1dp
 *
 * Onde aplicar:
 *  ✅ Hero card "Available Amount" (parâmetro opcional)
 *  ✅ Splash screen
 *  ✅ Tela de login/signup atrás do wordmark
 *  ❌ NÃO em todos os cards — vira ruído
 *  ❌ NÃO em listas longas — cansa
 *
 * Implementação:
 *  Loop varre desde -diagonal até +diagonal pra cobrir o canvas inteiro
 *  em qualquer aspect ratio. Cada linha vai de (x, 0) a (x+height, height)
 *  — ou seja, uma diagonal a 45°.
 */
fun Modifier.cashHatching(
    color: Color,
    alpha: Float = 0.06f,
    spacing: Dp = 8.dp,
    strokeWidth: Dp = 1.dp,
): Modifier = this.drawBehind {
    val spacingPx = spacing.toPx()
    val strokeWidthPx = strokeWidth.toPx()
    val hatchColor = color.copy(alpha = alpha)

    val diagonal = sqrt(size.width * size.width + size.height * size.height)
    var x = -diagonal
    while (x < diagonal) {
        drawLine(
            color = hatchColor,
            start = Offset(x, 0f),
            end = Offset(x + size.height, size.height),
            strokeWidth = strokeWidthPx,
        )
        x += spacingPx
    }
}
