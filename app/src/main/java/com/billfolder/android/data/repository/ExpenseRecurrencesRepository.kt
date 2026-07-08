package com.billfolder.android.data.repository

import com.billfolder.android.data.api.BillFolderApi
import com.billfolder.android.data.dto.CreateExpenseRecurrenceRequest
import com.billfolder.android.data.dto.ExpenseRecurrenceResponse
import com.billfolder.android.data.sync.DataChangeNotifier
import com.billfolder.android.data.sync.notifyingOnSuccess
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositório dos templates de recorrência de despesa (ex: fisioterapia
 * semanal). Por enquanto só precisamos do create pra feature de baixa
 * semanal — GET/PATCH/DELETE ficam pra quando houver tela de gerenciar
 * recorrências.
 *
 * Segue as convenções dos demais repos: ctor (api, notifier), write
 * embrulhado em notifyingOnSuccess pra o refresh cross-screen disparar.
 */
@Singleton
class ExpenseRecurrencesRepository @Inject constructor(
    private val api: BillFolderApi,
    private val notifier: DataChangeNotifier,
) {
    suspend fun create(request: CreateExpenseRecurrenceRequest): ExpenseRecurrenceResponse =
        notifier.notifyingOnSuccess { api.createExpenseRecurrence(request) }
}
