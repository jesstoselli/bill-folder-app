package com.billfolder.android.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate

/**
 * Roda a conferência diária. Pega as dependências por EntryPoint em vez de
 * hilt-work: um worker só não justifica a fábrica customizada.
 */
class DueBillsWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun dueBillsReminder(): DueBillsReminder
    }

    override suspend fun doWork(): Result {
        val reminder = EntryPointAccessors
            .fromApplication(applicationContext, Dependencies::class.java)
            .dueBillsReminder()
        return try {
            reminder.run(LocalDate.now())
            Result.success()
        } catch (e: IOException) {
            Result.retry()
        } catch (e: HttpException) {
            // 5xx passa; 4xx (sessão expirada, por ex.) não melhora tentando de novo.
            if (e.code() >= 500) Result.retry() else Result.failure()
        }
    }
}
