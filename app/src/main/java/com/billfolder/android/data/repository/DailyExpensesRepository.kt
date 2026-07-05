package com.billfolder.android.data.repository

import com.billfolder.android.data.api.BillFolderApi
import com.billfolder.android.data.dto.CreateDailyExpenseRequest
import com.billfolder.android.data.dto.DailyExpenseResponse
import com.billfolder.android.data.dto.UpdateDailyExpenseRequest
import com.billfolder.android.data.sync.DataChangeNotifier
import com.billfolder.android.data.sync.notifyingOnSuccess
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyExpensesRepository @Inject constructor(
    private val api: BillFolderApi,
    private val notifier: DataChangeNotifier,
) {
    /**
     * Lista despesas avulsas. `from` e `to` são strings ISO "yyyy-MM-dd"
     * (alinhado com DateOnly do backend). Repository não interpreta —
     * passa adiante e deixa a UI/ViewModel formar a janela de tempo.
     */
    suspend fun list(
        from: String? = null,
        to: String? = null,
        categoryId: String? = null,
    ): List<DailyExpenseResponse> = api.getDailyExpenses(from, to, categoryId)

    suspend fun create(request: CreateDailyExpenseRequest): DailyExpenseResponse =
        notifier.notifyingOnSuccess { api.createDailyExpense(request) }

    suspend fun update(
        id: String,
        request: UpdateDailyExpenseRequest,
    ): DailyExpenseResponse = notifier.notifyingOnSuccess { api.updateDailyExpense(id, request) }

    /**
     * Backend retorna 204 em sucesso; convertemos non-2xx em HttpException
     * pra o caller propagar pra UI (mesmo padrão do CardsRepository.deleteCard).
     */
    suspend fun delete(id: String) = notifier.notifyingOnSuccess {
        val response = api.deleteDailyExpense(id)
        if (!response.isSuccessful) {
            throw retrofit2.HttpException(response)
        }
    }
}
