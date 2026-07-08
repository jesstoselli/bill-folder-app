package com.billfolder.android.data.repository

import com.billfolder.android.data.api.BillFolderApi
import com.billfolder.android.data.dto.CardEntryRecurrenceResponse
import com.billfolder.android.data.dto.CreateCardEntryRecurrenceRequest
import com.billfolder.android.data.sync.DataChangeNotifier
import com.billfolder.android.data.sync.notifyingOnSuccess
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositório dos templates de assinatura de cartão (ex: Netflix, Spotify).
 * Por enquanto só precisamos do create pra feature de assinatura — GET/PATCH/
 * DELETE ficam pra quando houver tela de gerenciar assinaturas.
 *
 * Segue as convenções dos demais repos: ctor (api, notifier), write embrulhado
 * em notifyingOnSuccess pra o refresh cross-screen disparar. Espelha
 * ExpenseRecurrencesRepository.
 */
@Singleton
class CardEntryRecurrencesRepository @Inject constructor(
    private val api: BillFolderApi,
    private val notifier: DataChangeNotifier,
) {
    suspend fun create(request: CreateCardEntryRecurrenceRequest): CardEntryRecurrenceResponse =
        notifier.notifyingOnSuccess { api.createCardEntryRecurrence(request) }
}
