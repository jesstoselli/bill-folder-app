package com.billfolder.android.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// BillFolder Design System v0.2 — paleta completa.
//
// Seed única: #86BC65 (cash green clássico, extraído do logo).
// Paletas tonais geradas pelo Material Theme Builder (valores aproximados —
// regenerar com a seed pra valores 100% precisos).
// ============================================================================

// ----------------------------------------------------------------------------
// DARK (default)
// ----------------------------------------------------------------------------
val DarkPrimary             = Color(0xFFBBE89F)
val DarkOnPrimary           = Color(0xFF21490D)
val DarkPrimaryContainer    = Color(0xFF386225)
val DarkOnPrimaryContainer  = Color(0xFFD7F8C0)

val DarkSecondary           = Color(0xFFBFD0AB)
val DarkOnSecondary         = Color(0xFF2A361D)
val DarkSecondaryContainer  = Color(0xFF404D32)
val DarkOnSecondaryContainer = Color(0xFFDBECC6)

val DarkTertiary            = Color(0xFFA0D0C9)
val DarkOnTertiary          = Color(0xFF003733)
val DarkTertiaryContainer   = Color(0xFF1F4E4A)
val DarkOnTertiaryContainer = Color(0xFFBCECE4)

val DarkError               = Color(0xFFE09999)
val DarkOnError             = Color(0xFF993D3D)
val DarkErrorContainer      = Color(0xFFBC6565)
val DarkOnErrorContainer    = Color(0xFFE1E3DB)

val DarkSurface                  = Color(0xFF0A0D0A)
val DarkOnSurface                = Color(0xFFE1E3DB)
val DarkSurfaceVariant           = Color(0xFF42493D)
val DarkOnSurfaceVariant         = Color(0xFFC2C9BB)
val DarkSurfaceContainer         = Color(0xFF1E1E1E)
val DarkSurfaceContainerHigh     = Color(0xFF2D2D2D)
val DarkSurfaceContainerHighest  = Color(0xFF272B25)
val DarkOutline                  = Color(0xFF8C9387)
val DarkOutlineVariant           = Color(0xFF42493D)

// ----------------------------------------------------------------------------
// LIGHT (pronto, mas não ativado por enquanto — DS define dark-first)
// ----------------------------------------------------------------------------
val LightPrimary             = Color(0xFF4F7B3D)
val LightOnPrimary           = Color(0xFFFFFFFF)
val LightPrimaryContainer    = Color(0xFFD7F8C0)
val LightOnPrimaryContainer  = Color(0xFF0C3000)

val LightSecondary           = Color(0xFF586549)
val LightOnSecondary         = Color(0xFFFFFFFF)
val LightSecondaryContainer  = Color(0xFFDBECC6)
val LightOnSecondaryContainer = Color(0xFF161E0B)

val LightTertiary            = Color(0xFF386663)
val LightOnTertiary          = Color(0xFFFFFFFF)
val LightTertiaryContainer   = Color(0xFFBCECE4)
val LightOnTertiaryContainer = Color(0xFF00201E)

val LightError               = Color(0xFFBA1A1A)
val LightOnError             = Color(0xFFFFFFFF)
val LightErrorContainer      = Color(0xFFFFDAD6)
val LightOnErrorContainer    = Color(0xFF410002)

val LightSurface                  = Color(0xFFF8FAF1)
val LightOnSurface                = Color(0xFF181D14)
val LightSurfaceVariant           = Color(0xFFDEE5D2)
val LightOnSurfaceVariant         = Color(0xFF42493D)
val LightSurfaceContainer         = Color(0xFFECEFE5)
val LightSurfaceContainerHigh     = Color(0xFFE6E9DF)
val LightSurfaceContainerHighest  = Color(0xFFE0E3DA)
val LightOutline                  = Color(0xFF72796D)
val LightOutlineVariant           = Color(0xFFC2C9BB)

// ----------------------------------------------------------------------------
// Tokens BillFolder fora do M3 — "brand moments"
// ----------------------------------------------------------------------------

/** A seed exata. Constante entre dark/light. Usar em logo SVG, hatching, FAB. */
val BfBrandBill = Color(0xFF86BC65)

// ----------------------------------------------------------------------------
// Paleta da pie chart "where is my money going?"
// 6 cores estáveis (não usam dynamic color) — mapeamento categoria→cor é fixo.
// ----------------------------------------------------------------------------
val BfChart1 = Color(0xFF86BC65) // groceries / food
val BfChart2 = Color(0xFF6FA854) // entertainment / fun
val BfChart3 = Color(0xFFC5E0A5) // self-care
val BfChart4 = Color(0xFF5285A3) // bills / services
val BfChart5 = Color(0xFFB4CAD6) // outros
val BfChart6 = Color(0xFF9C9C9C) // shopping
