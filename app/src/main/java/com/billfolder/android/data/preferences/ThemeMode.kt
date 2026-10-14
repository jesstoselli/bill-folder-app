package com.billfolder.android.data.preferences

/**
 * Aparência escolhida pelo usuário. SYSTEM (padrão) segue o tema do aparelho;
 * LIGHT e DARK fixam o tema do app independente do sistema.
 */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Resolve a preferência para "o app está escuro agora?". */
fun ThemeMode.isDark(systemIsDark: Boolean): Boolean = when (this) {
    ThemeMode.SYSTEM -> systemIsDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}
