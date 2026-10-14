package com.billfolder.android.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Hora do aviso diário. */
internal const val DUE_REMINDER_HOUR = 9

/** Agenda (ou desliga) a conferência diária do que vence. */
interface DueBillsScheduler {
    /** [lastChecked] é o último dia já conferido; decide se hoje ainda precisa rodar. */
    fun schedule(lastChecked: LocalDate?)
    fun cancel()
}

/**
 * Próxima conferência: às 9h de hoje se ainda não deu 9h; agora mesmo se já
 * passou das 9h e hoje não foi conferido (o aparelho estava sem rede, o
 * lembrete acabou de ser ligado); senão, às 9h de amanhã.
 */
internal fun nextDueReminderRun(now: LocalDateTime, lastChecked: LocalDate?): LocalDateTime {
    val todayAtHour = now.toLocalDate().atTime(DUE_REMINDER_HOUR, 0)
    return when {
        now.isBefore(todayAtHour) -> todayAtHour
        lastChecked != now.toLocalDate() -> now
        else -> todayAtHour.plusDays(1)
    }
}

/**
 * Trabalho periódico de um dia. O WorkManager deixa o horário escorregar um
 * pouco a cada execução, então cada agendamento (abrir o app, ligar o
 * lembrete, terminar uma conferência) realinha a próxima pras 9h.
 */
@Singleton
class WorkManagerDueBillsScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : DueBillsScheduler {

    override fun schedule(lastChecked: LocalDate?) {
        val next = nextDueReminderRun(LocalDateTime.now(), lastChecked)
        val request = PeriodicWorkRequestBuilder<DueBillsWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .setNextScheduleTimeOverride(
                next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    private companion object {
        const val WORK_NAME = "due-bills-reminder"
    }
}
