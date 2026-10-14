package com.billfolder.android.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Lembretes de vencimento deste aparelho. */
interface ReminderPreferences {
    val dueRemindersEnabled: Flow<Boolean>
    suspend fun setDueRemindersEnabled(enabled: Boolean)

    /** Último dia já conferido, com ou sem aviso — evita avisar duas vezes no mesmo dia. */
    val lastCheckedDate: Flow<LocalDate?>
    suspend fun setLastCheckedDate(date: LocalDate)
}

@Singleton
class DataStoreReminderPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) : ReminderPreferences {
    private val enabledKey = booleanPreferencesKey("due_reminders_enabled")
    private val lastCheckedKey = stringPreferencesKey("due_reminders_last_checked")

    override val dueRemindersEnabled: Flow<Boolean> =
        context.preferencesDataStore.data.map { it[enabledKey] ?: false }

    override suspend fun setDueRemindersEnabled(enabled: Boolean) {
        context.preferencesDataStore.edit { it[enabledKey] = enabled }
    }

    override val lastCheckedDate: Flow<LocalDate?> = context.preferencesDataStore.data.map { prefs ->
        prefs[lastCheckedKey]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    }

    override suspend fun setLastCheckedDate(date: LocalDate) {
        context.preferencesDataStore.edit { it[lastCheckedKey] = date.toString() }
    }
}
