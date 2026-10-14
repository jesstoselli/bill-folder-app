package com.billfolder.android.notifications

import com.billfolder.android.data.auth.TokenStorage
import com.billfolder.android.data.preferences.ReminderPreferences
import com.billfolder.android.data.repository.DueBillsRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A conferência do dia: busca o que vence hoje e mostra um aviso só. Erros de
 * rede propagam pro worker tentar de novo; o dia só é marcado como conferido
 * depois que a busca deu certo.
 */
@Singleton
class DueBillsReminder @Inject constructor(
    private val repository: DueBillsRepository,
    private val preferences: ReminderPreferences,
    private val tokens: TokenStorage,
    private val notifier: DueBillsNotifier,
    private val scheduler: DueBillsScheduler,
) {
    suspend fun run(today: LocalDate) {
        if (!preferences.dueRemindersEnabled.first()) return
        if (preferences.lastCheckedDate.first() == today) return
        if (tokens.getAccessToken() == null) return

        dueBillsMessage(repository.dueOn(today))?.let(notifier::show)
        preferences.setLastCheckedDate(today)
        scheduler.schedule(lastChecked = today)
    }
}
