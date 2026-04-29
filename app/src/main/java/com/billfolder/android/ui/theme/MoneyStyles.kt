package com.billfolder.android.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Estilos custom específicos pra valores monetários.
 *
 * Por que existem em vez de só usar typography.displaySmall:
 *  - `fontFeatureSettings = "tnum"` ativa tabular numerals — todos os
 *    dígitos 0-9 ocupam a MESMA largura, fazendo "R$ 1.234,00" alinhar
 *    verticalmente em listas. Sem isso, "1" é mais estreito que "0",
 *    valores ficam tortos.
 *  - Pesos calibrados pra hierarquia: parte inteira em SemiBold, centavos
 *    em Normal (renderizadas opcionalmente em tamanho menor pelo
 *    componente que consome — não da pra fazer aqui porque é um TextStyle
 *    único, não um span builder).
 */

val MoneyDisplay = TextStyle(
    fontFamily = Barlow,
    fontWeight = FontWeight.SemiBold,
    fontSize   = 36.sp,
    lineHeight = 44.sp,
    fontFeatureSettings = "tnum",
)

val MoneyDisplayLarge = TextStyle(
    fontFamily = Barlow,
    fontWeight = FontWeight.SemiBold,
    fontSize   = 44.sp,
    lineHeight = 52.sp,
    letterSpacing = (-0.5).sp,
    fontFeatureSettings = "tnum",
)

val MoneyRow = TextStyle(
    fontFamily = Barlow,
    fontWeight = FontWeight.SemiBold,
    fontSize   = 16.sp,
    lineHeight = 24.sp,
    fontFeatureSettings = "tnum",
)

val MoneyCents = TextStyle(
    fontFamily = Barlow,
    fontWeight = FontWeight.Normal,
    fontSize   = 14.sp,
    lineHeight = 20.sp,
    fontFeatureSettings = "tnum",
)
