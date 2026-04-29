package com.billfolder.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// TODO: trocar BillFolderFontFamily pra Barlow Semi Condensed via
//   androidx.compose.ui.text.googlefonts.GoogleFont (Downloadable Fonts).
//   Requer popular res/values/font_certs.xml com os certificados do
//   provider com.google.android.gms.fonts. Por ora usamos a default
//   (Roboto Flex no Android 13+) com weights/sizes customizados — funciona
//   e mantém a hierarquia tipográfica intencional.
private val BillFolderFontFamily = FontFamily.Default

/**
 * Escala tipográfica do BillFolder. Optei por:
 * - displayLarge: número-herói da home ("R$ 1.234,56" sobra do mês)
 * - headlineSmall: títulos de tela
 * - titleMedium: cabeçalhos de seção / cards
 * - bodyLarge: texto principal
 * - bodyMedium: texto secundário / listas densas
 * - labelLarge: botões e CTAs
 * - labelSmall: chips de status (paid/pending/overdue)
 */
val BillFolderTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = BillFolderFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize   = 44.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = BillFolderFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = BillFolderFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize   = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = BillFolderFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = BillFolderFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = BillFolderFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = BillFolderFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize   = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
    ),
)
