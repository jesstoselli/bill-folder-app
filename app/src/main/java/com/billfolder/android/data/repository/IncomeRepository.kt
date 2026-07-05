package com.billfolder.android.data.repository

import com.billfolder.android.data.api.BillFolderApi
import com.billfolder.android.data.dto.CreateIncomeEntryRequest
import com.billfolder.android.data.dto.CreateIncomeSourceRequest
import com.billfolder.android.data.dto.IncomeEntryResponse
import com.billfolder.android.data.dto.IncomeSourceResponse
import com.billfolder.android.data.dto.UpdateIncomeEntryRequest
import com.billfolder.android.data.dto.UpdateIncomeSourceRequest
import com.billfolder.android.data.sync.DataChangeNotifier
import com.billfolder.android.data.sync.notifyingOnSuccess
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncomeRepository @Inject constructor(
    private val api: BillFolderApi,
    private val notifier: DataChangeNotifier,
) {
    // -------- Sources (recorrentes) --------
    suspend fun listSources(): List<IncomeSourceResponse> = api.getIncomeSources()

    suspend fun createSource(request: CreateIncomeSourceRequest): IncomeSourceResponse =
        notifier.notifyingOnSuccess { api.createIncomeSource(request) }

    suspend fun updateSource(
        id: String,
        request: UpdateIncomeSourceRequest,
    ): IncomeSourceResponse = notifier.notifyingOnSuccess { api.updateIncomeSource(id, request) }

    /**
     * Backend deleta a fonte mas preserva entries históricas — FK em
     * income_entries.source_id é ON DELETE SET NULL. Operação não-
     * destrutiva pro histórico financeiro do user. Sheet de confirmação
     * (Passo 4) registra esse comportamento na mensagem.
     */
    suspend fun deleteSource(id: String) = notifier.notifyingOnSuccess {
        val response = api.deleteIncomeSource(id)
        if (!response.isSuccessful) {
            throw retrofit2.HttpException(response)
        }
    }

    // -------- Entries (individuais, do ciclo) --------
    suspend fun listEntries(
        from: String? = null,
        to: String? = null,
        sourceId: String? = null,
    ): List<IncomeEntryResponse> = api.getIncomeEntries(from, to, sourceId)

    suspend fun createEntry(request: CreateIncomeEntryRequest): IncomeEntryResponse =
        notifier.notifyingOnSuccess { api.createIncomeEntry(request) }

    suspend fun updateEntry(id: String, request: UpdateIncomeEntryRequest): IncomeEntryResponse =
        notifier.notifyingOnSuccess { api.updateIncomeEntry(id, request) }

    /**
     * Helper: marca entry como recebida. Backend pode auto-preencher
     * actualDate=hoje e actualAmount=expectedAmount se vierem null.
     */
    suspend fun markReceived(
        id: String,
        actualDate: String? = null,
        actualAmount: Double? = null,
    ): IncomeEntryResponse = updateEntry(
        id = id,
        request = UpdateIncomeEntryRequest(
            status = "received",
            actualDate = actualDate,
            actualAmount = actualAmount,
        ),
    )

    /**
     * Backend retorna 204 em sucesso; convertemos non-2xx em HttpException
     * pra o caller propagar pra UI (mesmo padrão dos outros repos).
     */
    suspend fun deleteEntry(id: String) = notifier.notifyingOnSuccess {
        val response = api.deleteIncomeEntry(id)
        if (!response.isSuccessful) {
            throw retrofit2.HttpException(response)
        }
    }
}
