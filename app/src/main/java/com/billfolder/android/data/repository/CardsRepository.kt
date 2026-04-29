package com.billfolder.android.data.repository

import com.billfolder.android.data.api.BillFolderApi
import com.billfolder.android.data.dto.CardEntryResponse
import com.billfolder.android.data.dto.CreateCardEntryRequest
import com.billfolder.android.data.dto.CreateCreditCardAccountRequest
import com.billfolder.android.data.dto.CreditCardAccountResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardsRepository @Inject constructor(
    private val api: BillFolderApi,
) {
    // ---- Credit cards (entidades durávies) ----
    suspend fun listCards(): List<CreditCardAccountResponse> = api.getCreditCards()


    suspend fun createCard(request: CreateCreditCardAccountRequest): CreditCardAccountResponse =
        api.createCreditCard(request)

    /**
     * Deleta o cartão. Backend retorna 204 em sucesso; convertemos
     * non-2xx em HttpException pra o caller propagar pra UI.
     */
    suspend fun deleteCard(id: String) {
        val response = api.deleteCreditCard(id)
        if (!response.isSuccessful) {
            throw retrofit2.HttpException(response)
        }
    }

    // ---- Card entries (compras) ----
    suspend fun listEntries(cardId: String? = null): List<CardEntryResponse> =
        api.getCardEntries(cardId)

    suspend fun createEntry(request: CreateCardEntryRequest): CardEntryResponse =
        api.createCardEntry(request)
}
