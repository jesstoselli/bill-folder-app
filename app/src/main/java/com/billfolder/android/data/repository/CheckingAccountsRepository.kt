package com.billfolder.android.data.repository

import com.billfolder.android.data.api.BillFolderApi
import com.billfolder.android.data.dto.CheckingAccountResponse
import com.billfolder.android.data.dto.CreateCheckingAccountRequest
import com.billfolder.android.data.dto.UpdateCheckingAccountRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositório de contas correntes (CheckingAccount).
 *
 * Convive com o ReferenceDataRepository, que expõe getCheckingAccounts()
 * também. A diferença: ReferenceDataRepository é read-only agrupado com
 * categorias, pra alimentar dropdowns de formulários (AddDailyExpense,
 * PayExpense etc). Esse repo dedicado carrega o CRUD da ManageBanksScreen.
 *
 * Convenção idêntica ao CardsRepository/SavingsRepository: DELETE retorna
 * Response<Unit> pra converter non-2xx em HttpException explícito.
 *
 * Sobre isPrimary: backend garante invariante "no máximo 1 primary por
 * user". Marcar uma conta como primary desmarca as outras automaticamente
 * — nada que o caller precise coordenar.
 */
@Singleton
class CheckingAccountsRepository @Inject constructor(
    private val api: BillFolderApi,
) {
    suspend fun listAccounts(): List<CheckingAccountResponse> = api.getCheckingAccounts()

    suspend fun createAccount(
        request: CreateCheckingAccountRequest,
    ): CheckingAccountResponse = api.createCheckingAccount(request)

    suspend fun updateAccount(
        id: String,
        request: UpdateCheckingAccountRequest,
    ): CheckingAccountResponse = api.updateCheckingAccount(id, request)

    suspend fun deleteAccount(id: String) {
        val response = api.deleteCheckingAccount(id)
        if (!response.isSuccessful) {
            throw retrofit2.HttpException(response)
        }
    }
}
