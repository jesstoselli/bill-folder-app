package com.billfolder.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * BillFolder Theme — DS v0.2.
 *
 * Decisões fechadas no DS:
 *  - Dark é default; Light pronto mas não ativo (futura preferência do usuário).
 *  - Dynamic color (Material You) DESLIGADO — a identidade verde é parte da marca.
 *  - Logo + hatching usam BfBrandBill direto (constante entre dark/light).
 */
private val DarkColors = darkColorScheme(
    primary             = DarkPrimary,
    onPrimary           = DarkOnPrimary,
    primaryContainer    = DarkPrimaryContainer,
    onPrimaryContainer  = DarkOnPrimaryContainer,

    secondary           = DarkSecondary,
    onSecondary         = DarkOnSecondary,
    secondaryContainer  = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,

    tertiary            = DarkTertiary,
    onTertiary          = DarkOnTertiary,
    tertiaryContainer   = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,

    error               = DarkError,
    onError             = DarkOnError,
    errorContainer      = DarkErrorContainer,
    onErrorContainer    = DarkOnErrorContainer,

    surface                  = DarkSurface,
    onSurface                = DarkOnSurface,
    surfaceVariant           = DarkSurfaceVariant,
    onSurfaceVariant         = DarkOnSurfaceVariant,
    surfaceContainer         = DarkSurfaceContainer,
    surfaceContainerHigh     = DarkSurfaceContainerHigh,
    surfaceContainerHighest  = DarkSurfaceContainerHighest,
    background               = DarkSurface,
    onBackground             = DarkOnSurface,

    outline         = DarkOutline,
    outlineVariant  = DarkOutlineVariant,
)

private val LightColors = lightColorScheme(
    primary             = LightPrimary,
    onPrimary           = LightOnPrimary,
    primaryContainer    = LightPrimaryContainer,
    onPrimaryContainer  = LightOnPrimaryContainer,

    secondary           = LightSecondary,
    onSecondary         = LightOnSecondary,
    secondaryContainer  = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,

    tertiary            = LightTertiary,
    onTertiary          = LightOnTertiary,
    tertiaryContainer   = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,

    error               = LightError,
    onError             = LightOnError,
    errorContainer      = LightErrorContainer,
    onErrorContainer    = LightOnErrorContainer,

    surface                  = LightSurface,
    onSurface                = LightOnSurface,
    surfaceVariant           = LightSurfaceVariant,
    onSurfaceVariant         = LightOnSurfaceVariant,
    surfaceContainer         = LightSurfaceContainer,
    surfaceContainerHigh     = LightSurfaceContainerHigh,
    surfaceContainerHighest  = LightSurfaceContainerHighest,
    background               = LightSurface,
    onBackground             = LightOnSurface,

    outline         = LightOutline,
    outlineVariant  = LightOutlineVariant,
)

@Composable
fun BillFolderTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography  = BfTypography,
        shapes      = BfShapes,
        content     = content,
    )
}
