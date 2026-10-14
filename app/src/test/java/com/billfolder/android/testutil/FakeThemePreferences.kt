package com.billfolder.android.testutil

import com.billfolder.android.data.preferences.ThemeMode
import com.billfolder.android.data.preferences.ThemePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeThemePreferences(initial: ThemeMode = ThemeMode.SYSTEM) : ThemePreferences {
    private val mode = MutableStateFlow(initial)
    override val themeMode: StateFlow<ThemeMode> = mode
    override suspend fun setThemeMode(mode: ThemeMode) {
        this.mode.value = mode
    }
}
