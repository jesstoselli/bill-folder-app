package com.billfolder.android.testutil

import com.billfolder.android.data.preferences.ReminderPreferences
import com.billfolder.android.notifications.DueBillsScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate

class FakeReminderPreferences(
    enabled: Boolean = false,
    lastChecked: LocalDate? = null,
) : ReminderPreferences {
    override val dueRemindersEnabled = MutableStateFlow(enabled)
    override val lastCheckedDate = MutableStateFlow(lastChecked)

    override suspend fun setDueRemindersEnabled(enabled: Boolean) {
        dueRemindersEnabled.value = enabled
    }

    override suspend fun setLastCheckedDate(date: LocalDate) {
        lastCheckedDate.value = date
    }
}

class FakeDueBillsScheduler : DueBillsScheduler {
    /** Cada agendamento, com o último dia conferido que recebeu. */
    val scheduled = mutableListOf<LocalDate?>()
    var cancelled = 0

    override fun schedule(lastChecked: LocalDate?) {
        scheduled += lastChecked
    }

    override fun cancel() {
        cancelled++
    }
}
