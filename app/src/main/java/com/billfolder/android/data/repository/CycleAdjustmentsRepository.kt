package com.billfolder.android.data.repository

import com.billfolder.android.data.api.BillFolderApi
import com.billfolder.android.data.dto.CreateCycleAdjustmentRequest
import com.billfolder.android.data.dto.CycleAdjustmentResponse
import com.billfolder.android.data.dto.UpdateCycleAdjustmentRequest
import com.billfolder.android.data.sync.DataChangeNotifier
import com.billfolder.android.data.sync.notifyingOnSuccess
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositório de "ajustes do ciclo" — entradas/saídas avulsas do ciclo
 * atual que não são despesa/receita/cartão/daily. Ex: venda de item usado,
 * saque da poupança, dívida esquecida, presente eventual.
 *
 * Backend: /v1/cycle-adjustments (CRUD completo).
 */
@Singleton
class CycleAdjustmentsRepository @Inject constructor(
    private val api: BillFolderApi,
    private val notifier: DataChangeNotifier,
) {
    /** Lista ajustes filtrados por data (janela do ciclo) e tipo opcional. */
    suspend fun list(
        from: String? = null,
        to: String? = null,
        type: String? = null,
    ): List<CycleAdjustmentResponse> = api.getCycleAdjustments(from, to, type)

    suspend fun create(request: CreateCycleAdjustmentRequest): CycleAdjustmentResponse =
        notifier.notifyingOnSuccess { api.createCycleAdjustment(request) }

    suspend fun update(id: String, request: UpdateCycleAdjustmentRequest): CycleAdjustmentResponse =
        notifier.notifyingOnSuccess { api.updateCycleAdjustment(id, request) }

    suspend fun delete(id: String) = notifier.notifyingOnSuccess {
        val response = api.deleteCycleAdjustment(id)
        if (!response.isSuccessful) {
            throw retrofit2.HttpException(response)
        }
    }
}
