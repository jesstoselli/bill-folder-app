package com.billfolder.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Tema do app. Forçamos dark mode; não respeitamos o sistema, não usamos
 * dynamic color (Material You). A identidade visual é fixa.
 */
private val BillFolderColorScheme = darkColorScheme(
    primary       = BillGreen,
    onPrimary     = SurfaceBlack,
    primaryContainer   = BillGreenDim,
    onPrimaryContainer = TextPrimary,

    secondary     = BillAmber,
    onSecondary   = SurfaceBlack,

    error         = BillRed,
    onError       = TextPrimary,

    background    = SurfaceBlack,
    onBackground  = TextPrimary,

    surface           = SurfaceElevated,
    onSurface         = TextPrimary,
    surfaceVariant    = SurfaceCard,
    onSurfaceVariant  = TextSecondary,
    surfaceContainerHigh = SurfaceHigh,

    outline         = OutlineSubtle,
    outlineVariant  = OutlineSubtle,
)

@Composable
fun BillFolderTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = BillFolderColorScheme,
        typography  = BillFolderTypography,
        content     = content,
    )
}
