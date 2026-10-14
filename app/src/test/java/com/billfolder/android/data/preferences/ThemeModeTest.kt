package com.billfolder.android.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {

    @Test
    fun `automatico segue o sistema e claro ou escuro fixam o tema`() {
        assertEquals(true, ThemeMode.SYSTEM.isDark(systemIsDark = true))
        assertEquals(false, ThemeMode.SYSTEM.isDark(systemIsDark = false))
        assertEquals(false, ThemeMode.LIGHT.isDark(systemIsDark = true))
        assertEquals(true, ThemeMode.DARK.isDark(systemIsDark = false))
    }
}
