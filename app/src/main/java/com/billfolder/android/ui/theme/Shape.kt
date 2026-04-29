package com.billfolder.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Sistema de raios do BillFolder DS v0.2.
 *
 * Regra: tudo interativo (botão, FAB, input, chip) é pílula. Cards comuns
 * têm raio 16dp; só o hero card carrega raio 28dp pra ser visualmente
 * dominante na Home.
 */
val BfShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),    // badges retangulares pequenos
    small      = RoundedCornerShape(8.dp),    // raramente usado
    medium     = RoundedCornerShape(12.dp),   // tooltips, menus, list rows
    large      = RoundedCornerShape(16.dp),   // CARDS PADRÃO
    extraLarge = RoundedCornerShape(28.dp),   // SÓ HERO + bottom sheets / modais
)

/** Botões, FABs, TextFields, Chips. */
val PillShape = RoundedCornerShape(percent = 50)
