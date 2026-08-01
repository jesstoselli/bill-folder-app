package com.billfolder.android.data.repository

import com.billfolder.android.data.api.BillFolderApi
import com.billfolder.android.data.dto.CardStatementResponse
import com.billfolder.android.data.dto.PayCardStatementRequest
import com.billfolder.android.data.sync.DataChangeNotifier
import com.billfolder.android.data.sync.notifyingOnSuccess
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardStatementsRepository @Inject constructor(
    private val api: BillFolderApi,
    private val notifier: DataChangeNotifier,
) {
    suspend fun list(cardId: String? = null): List<CardStatementResponse> =
        api.getCardStatements(cardId)

    suspend fun pay(
        id: String,
        request: PayCardStatementRequest,
    ): CardStatementResponse = notifier.notifyingOnSuccess { api.payCardStatement(id, request) }
}
