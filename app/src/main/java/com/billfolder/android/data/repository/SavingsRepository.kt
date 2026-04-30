package com.billfolder.android.data.repository

import com.billfolder.android.data.api.BillFolderApi
import com.billfolder.android.data.dto.CreateSavingsAccountRequest
import com.billfolder.android.data.dto.CreateSavingsTransactionRequest
import com.billfolder.android.data.dto.SavingsAccountResponse
import com.billfolder.android.data.dto.SavingsTransactionResponse
import com.billfolder.android.data.dto.UpdateSavingsAccountRequest
import com.billfolder.android.data.dto.UpdateSavingsTransactionRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositório de poupança — agrupa contas (SavingsAccount) e movimentações
 * (SavingsTransaction). Mesmo molde de IncomesRepository, que junta sources
 * e entries: enquanto o domínio for pequeno, mantém num único repo pra
 * evitar inflar a contagem de @Singleton; quando crescer, separa.
 *
 * Convenção idêntica aos demais repos do app: DELETE retorna Response<Unit>
 * pra a gente conseguir converter non-2xx em HttpException explícito.
 */
@Singleton
class SavingsRepository @Inject constructor(
    private val api: BillFolderApi,
) {

    // ------------------------------------------------------------------------
    // Accounts (Fase A)
    // ------------------------------------------------------------------------

    suspend fun listAccounts(): List<SavingsAccountResponse> = api.getSavingsAccounts()

    suspend fun createAccount(request: CreateSavingsAccountRequest): SavingsAccountResponse =
        api.createSavingsAccount(request)

    /**
     * PATCH parcial. Backend não permite mudar a checkingAccountId
     * (vínculo é fixo na criação) — sheet de edit deve manter o
     * dropdown de checking disabled.
     */
    suspend fun updateAccount(
        id: String,
        request: UpdateSavingsAccountRequest,
    ): SavingsAccountResponse = api.updateSavingsAccount(id, request)

    /**
     * Backend retorna 204 em sucesso. ON DELETE CASCADE no schema —
     * apagar a poupança remove todas as SavingsTransactions associadas.
     */
    suspend fun deleteAccount(id: String) {
        val response = api.deleteSavingsAccount(id)
        if (!response.isSuccessful) {
            throw retrofit2.HttpException(response)
        }
    }

    // ------------------------------------------------------------------------
    // Transactions (Fase B)
    //
    // Pra "transações dessa poupança no ciclo atual" passar
    // savingsAccountId=<id>&from=<cycle.start>&to=<cycle.end>. type fica
    // null (queremos todos os 5 tipos no read mesmo se a UI só permite
    // criar 3 deles).
    // ------------------------------------------------------------------------

    suspend fun listTransactions(
        savingsAccountId: String? = null,
        from: String? = null,
        to: String? = null,
        type: String? = null,
    ): List<SavingsTransactionResponse> = api.getSavingsTransactions(
        savingsAccountId = savingsAccountId,
        from = from,
        to = to,
        type = type,
    )

    suspend fun createTransaction(
        request: CreateSavingsTransactionRequest,
    ): SavingsTransactionResponse = api.createSavingsTransaction(request)

    suspend fun updateTransaction(
        id: String,
        request: UpdateSavingsTransactionRequest,
    ): SavingsTransactionResponse = api.updateSavingsTransaction(id, request)

    suspend fun deleteTransaction(id: String) {
        val response = api.deleteSavingsTransaction(id)
        if (!response.isSuccessful) {
            throw retrofit2.HttpException(response)
        }
    }
}
