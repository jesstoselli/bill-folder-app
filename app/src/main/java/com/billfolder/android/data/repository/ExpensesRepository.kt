package com.billfolder.android.data.repository

import com.billfolder.android.data.api.BillFolderApi
import com.billfolder.android.data.dto.CreateExpenseRequest
import com.billfolder.android.data.dto.ExpenseResponse
import com.billfolder.android.data.dto.UpdateExpenseRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpensesRepository @Inject constructor(
    private val api: BillFolderApi,
) {
    suspend fun list(
        from: String? = null,
        to: String? = null,
        status: String? = null,
        categoryId: String? = null,
    ): List<ExpenseResponse> = api.getExpenses(from, to, status, categoryId)

    suspend fun create(request: CreateExpenseRequest): ExpenseResponse =
        api.createExpense(request)

    suspend fun update(id: String, request: UpdateExpenseRequest): ExpenseResponse =
        api.updateExpense(id, request)

    /**
     * Conveniência: marca como paid mandando o mínimo necessário. Backend
     * auto-preenche paidDate (=hoje) e actualAmount (=expectedAmount) se
     * vierem null. paidFromAccountId é opcional — se vier, registra de
     * qual conta saiu o dinheiro.
     */
    suspend fun markPaid(
        id: String,
        paidDate: String? = null,
        actualAmount: Double? = null,
        paidFromAccountId: String? = null,
    ): ExpenseResponse = update(
        id = id,
        request = UpdateExpenseRequest(
            status = "paid",
            paidDate = paidDate,
            actualAmount = actualAmount,
            paidFromAccountId = paidFromAccountId,
        ),
    )
}
