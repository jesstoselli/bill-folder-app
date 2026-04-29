package com.billfolder.android.data.repository

import com.billfolder.android.data.api.BillFolderApi
import com.billfolder.android.data.dto.CreateDailyExpenseRequest
import com.billfolder.android.data.dto.DailyExpenseResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyExpensesRepository @Inject constructor(
    private val api: BillFolderApi,
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
        api.createDailyExpense(request)
}
