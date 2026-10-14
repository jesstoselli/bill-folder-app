package com.billfolder.android.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Preferência de aparência deste aparelho. */
interface ThemePreferences {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}

/**
 * Fica num DataStore próprio, separado do de tokens: é uma escolha do
 * aparelho, não da sessão, e sobrevive ao logout.
 */
private val Context.preferencesDataStore by preferencesDataStore(name = "billfolder_preferences")

@Singleton
class DataStoreThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) : ThemePreferences {
    private val themeKey = stringPreferencesKey("theme_mode")

    override val themeMode: Flow<ThemeMode> = context.preferencesDataStore.data.map { prefs ->
        prefs[themeKey]?.let { saved -> ThemeMode.entries.firstOrNull { it.name == saved } }
            ?: ThemeMode.SYSTEM
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.preferencesDataStore.edit { it[themeKey] = mode.name }
    }
}
