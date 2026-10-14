package com.billfolder.android.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.core.view.WindowCompat
import com.billfolder.android.R

/**
 * BillFolder Theme — DS v0.2.
 *
 * Decisões fechadas no DS:
 *  - Dark é default; Light segue a preferência do usuário (sua conta > aparência).
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

/**
 * Se o app está no tema escuro agora. O tema segue a preferência do usuário
 * (não só o sistema), então quem precisa saber — o logo, cores fixas da
 * marca — lê daqui em vez de isSystemInDarkTheme().
 */
val LocalBfDarkTheme = staticCompositionLocalOf { true }

@Composable
fun BillFolderTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    // enableEdgeToEdge() escolhe a cor dos ícones das barras pelo tema do
    // sistema; com o app em outro tema, eles sumiriam. Ajusta pelo tema do app.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalBfDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography  = BfTypography,
            shapes      = BfShapes,
            content     = content,
        )
    }
}

/** Wordmark do BillFolder na variante legível sobre o fundo do tema atual. */
@Composable
fun billFolderLogo(): Painter = painterResource(
    if (LocalBfDarkTheme.current) R.drawable.logo_billfolder else R.drawable.logo_billfolder_light,
)

/**
 * Valores positivos (entradas, rendimentos). O verde da marca tem contraste
 * baixo sobre o fundo claro, então no tema claro usa o primary.
 */
val BfPositiveAmount: Color
    @Composable get() = if (LocalBfDarkTheme.current) BfBrandBill else MaterialTheme.colorScheme.primary
