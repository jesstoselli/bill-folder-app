package com.billfolder.android.data.repository

import com.billfolder.android.data.api.BillFolderApi
import com.billfolder.android.data.dto.CardEntryResponse
import com.billfolder.android.data.dto.CreateCardEntryRequest
import com.billfolder.android.data.dto.CreateCreditCardAccountRequest
import com.billfolder.android.data.dto.CreditCardAccountResponse
import com.billfolder.android.data.dto.UpdateCardEntryRequest
import com.billfolder.android.data.dto.UpdateCreditCardAccountRequest
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
     * PATCH parcial. Backend não recalcula statements/installments —
     * mudar closingDay/dueDay afeta só lançamentos futuros. Sheet de
     * edit avisa o user disso.
     */
    suspend fun updateCard(
        id: String,
        request: UpdateCreditCardAccountRequest,
    ): CreditCardAccountResponse = api.updateCreditCard(id, request)

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

    /**
     * PATCH limitado a label/categoria/notes — backend não aceita mudar
     * valor/parcelas/data (recálculo de installments fica pra endpoint
     * dedicado no futuro).
     */
    suspend fun updateEntry(id: String, request: UpdateCardEntryRequest): CardEntryResponse =
        api.updateCardEntry(id, request)

    /**
     * Backend retorna 204 em sucesso. Importante: deletar uma entry
     * parcelada remove TODAS as installments associadas e recalcula
     * statements futuros — backend lida com a cascata.
     */
    suspend fun deleteEntry(id: String) {
        val response = api.deleteCardEntry(id)
        if (!response.isSuccessful) {
            throw retrofit2.HttpException(response)
        }
    }
}
