package com.billfolder.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.billfolder.android.R

/**
 * Barlow Semi Condensed via Google Fonts Downloadable.
 *
 * O provider faz fetch da fonte em runtime via Google Play Services
 * (assinado pelos certs em res/values/font_certs.xml). Vantagem:
 * sem ~150KB de TTF no APK e a fonte é cacheada por todos os apps
 * que usam o mesmo Provider.
 *
 * Por que semi-condensed pra app financeiro:
 *  - Cabe mais "R$ 5.730,00" em telas estreitas.
 *  - Tem `tnum` (tabular nums) — dígitos alinham em listas (ver MoneyStyles.kt).
 */
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val barlowSemiCondensed = GoogleFont("Barlow Semi Condensed")

val Barlow = FontFamily(
    Font(googleFont = barlowSemiCondensed, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = barlowSemiCondensed, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = barlowSemiCondensed, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = barlowSemiCondensed, fontProvider = provider, weight = FontWeight.Bold),
)

/**
 * Type scale do DS v0.2. Convenções:
 *  - Headers de seção em lowercase ("next due", "overdue").
 *  - Valores monetários usam estilos custom em MoneyStyles.kt.
 */
val BfTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Barlow,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 44.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Barlow,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 40.sp,
        lineHeight = 48.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = Barlow,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 36.sp,
        lineHeight = 44.sp,
    ),

    headlineLarge = TextStyle(
        fontFamily = Barlow,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Barlow,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Barlow,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 24.sp,
        lineHeight = 32.sp,
    ),

    titleLarge = TextStyle(
        fontFamily = Barlow,
        fontWeight = FontWeight.Medium,
        fontSize   = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Barlow,
        fontWeight = FontWeight.Medium,
        fontSize   = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Barlow,
        fontWeight = FontWeight.Medium,
        fontSize   = 14.sp,
        lineHeight = 20.sp,
    ),

    bodyLarge = TextStyle(
        fontFamily = Barlow,
        fontWeight = FontWeight.Normal,
        fontSize   = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Barlow,
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Barlow,
        fontWeight = FontWeight.Normal,
        fontSize   = 12.sp,
        lineHeight = 16.sp,
    ),

    labelLarge = TextStyle(
        fontFamily = Barlow,
        fontWeight = FontWeight.Medium,
        fontSize   = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Barlow,
        fontWeight = FontWeight.Medium,
        fontSize   = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Barlow,
        fontWeight = FontWeight.Medium,
        fontSize   = 11.sp,
        lineHeight = 16.sp,
    ),
)
