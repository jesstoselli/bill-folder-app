package com.billfolder.android.data.repository

import com.billfolder.android.data.api.BillFolderApi
import com.billfolder.android.data.dto.CreateExpenseRequest
import com.billfolder.android.data.dto.ExpenseResponse
import com.billfolder.android.data.dto.PayOccurrenceRequest
import com.billfolder.android.data.dto.RepriceProvisionedExpenseRequest
import com.billfolder.android.data.dto.UpdateExpenseRequest
import com.billfolder.android.data.sync.DataChangeNotifier
import com.billfolder.android.data.sync.notifyingOnSuccess
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpensesRepository @Inject constructor(
    private val api: BillFolderApi,
    private val notifier: DataChangeNotifier,
) {
    suspend fun list(
        from: String? = null,
        to: String? = null,
        status: String? = null,
        categoryId: String? = null,
    ): List<ExpenseResponse> = api.getExpenses(from, to, status, categoryId)

    suspend fun create(request: CreateExpenseRequest): ExpenseResponse =
        notifier.notifyingOnSuccess { api.createExpense(request) }

    suspend fun update(id: String, request: UpdateExpenseRequest): ExpenseResponse =
        notifier.notifyingOnSuccess { api.updateExpense(id, request) }

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

    /**
     * "Dar baixa" numa ocorrência semanal de uma despesa provisionada
     * (POST /expenses/{id}/pay-occurrence). Retorna o ExpenseResponse
     * atualizado (occurrencesPaid/paidToDate incrementados). Notifica o
     * bus em sucesso, como todo write.
     */
    suspend fun payOccurrence(
        id: String,
        request: PayOccurrenceRequest,
    ): ExpenseResponse = notifier.notifyingOnSuccess { api.payOccurrence(id, request) }

    /**
     * Reajusta o valor por sessão de uma despesa provisionada
     * (POST /expenses/{id}/update-amount). scope no body é camelCase
     * ("this"/"thisAndFollowing"). Retorna o ExpenseResponse atualizado;
     * notifica o bus em sucesso como todo write.
     */
    suspend fun repriceProvisioned(
        id: String,
        request: RepriceProvisionedExpenseRequest,
    ): ExpenseResponse = notifier.notifyingOnSuccess { api.repriceProvisionedExpense(id, request) }

    /**
     * Backend retorna 204 em sucesso; convertemos non-2xx em HttpException
     * pra o caller propagar pra UI (mesmo padrão dos outros repos).
     *
     * scope (query, snake_case): "this" (default) ou "this_and_following" —
     * numa despesa gerada por recorrência, decide se apaga só esta ou as futuras.
     */
    suspend fun delete(id: String, scope: String? = null) = notifier.notifyingOnSuccess {
        val response = api.deleteExpense(id, scope)
        if (!response.isSuccessful) {
            throw retrofit2.HttpException(response)
        }
    }
}
