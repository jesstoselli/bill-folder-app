package com.billfolder.android.data.repository

import com.billfolder.android.data.api.BillFolderApi
import com.billfolder.android.data.dto.CreateSavingsAccountRequest
import com.billfolder.android.data.dto.SavingsAccountResponse
import com.billfolder.android.data.dto.UpdateSavingsAccountRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositório das contas poupança.
 *
 * Fase A: só CRUD da conta. As transações (deposit/withdrawal/yield)
 * vêm na Fase B junto com a SavingsScreen de consumo.
 *
 * Convenção idêntica ao CardsRepository: DELETE retorna Response<Unit>
 * pra a gente conseguir converter non-2xx em HttpException explícito.
 */
@Singleton
class SavingsRepository @Inject constructor(
    private val api: BillFolderApi,
) {
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
}
