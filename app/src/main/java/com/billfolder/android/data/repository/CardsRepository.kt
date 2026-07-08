package com.billfolder.android.data.repository

import com.billfolder.android.data.api.BillFolderApi
import com.billfolder.android.data.dto.CardEntryResponse
import com.billfolder.android.data.dto.CreateCardEntryRequest
import com.billfolder.android.data.dto.CreateCreditCardAccountRequest
import com.billfolder.android.data.dto.CreditCardAccountResponse
import com.billfolder.android.data.dto.UpdateCardEntryRequest
import com.billfolder.android.data.dto.UpdateCardSubscriptionAmountRequest
import com.billfolder.android.data.dto.UpdateCreditCardAccountRequest
import com.billfolder.android.data.sync.DataChangeNotifier
import com.billfolder.android.data.sync.notifyingOnSuccess
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardsRepository @Inject constructor(
    private val api: BillFolderApi,
    private val notifier: DataChangeNotifier,
) {
    // ---- Credit cards (entidades durávies) ----
    suspend fun listCards(): List<CreditCardAccountResponse> = api.getCreditCards()


    suspend fun createCard(request: CreateCreditCardAccountRequest): CreditCardAccountResponse =
        notifier.notifyingOnSuccess { api.createCreditCard(request) }

    /**
     * PATCH parcial. Backend não recalcula statements/installments —
     * mudar closingDay/dueDay afeta só lançamentos futuros. Sheet de
     * edit avisa o user disso.
     */
    suspend fun updateCard(
        id: String,
        request: UpdateCreditCardAccountRequest,
    ): CreditCardAccountResponse = notifier.notifyingOnSuccess { api.updateCreditCard(id, request) }

    /**
     * Deleta o cartão. Backend retorna 204 em sucesso; convertemos
     * non-2xx em HttpException pra o caller propagar pra UI.
     */
    suspend fun deleteCard(id: String) = notifier.notifyingOnSuccess {
        val response = api.deleteCreditCard(id)
        if (!response.isSuccessful) {
            throw retrofit2.HttpException(response)
        }
    }

    // ---- Card entries (compras) ----
    suspend fun listEntries(cardId: String? = null): List<CardEntryResponse> =
        api.getCardEntries(cardId)

    suspend fun createEntry(request: CreateCardEntryRequest): CardEntryResponse =
        notifier.notifyingOnSuccess { api.createCardEntry(request) }

    /**
     * PATCH limitado a label/categoria/notes — backend não aceita mudar
     * valor/parcelas/data (recálculo de installments fica pra endpoint
     * dedicado no futuro).
     */
    suspend fun updateEntry(id: String, request: UpdateCardEntryRequest): CardEntryResponse =
        notifier.notifyingOnSuccess { api.updateCardEntry(id, request) }

    /**
     * "Reprecificar" uma assinatura (POST /card-entries/{id}/update-amount).
     * scope no body é camelCase ("this"/"thisAndFollowing"). Retorna o card
     * entry atualizado; notifica o bus em sucesso como todo write.
     */
    suspend fun updateSubscriptionAmount(
        id: String,
        request: UpdateCardSubscriptionAmountRequest,
    ): CardEntryResponse =
        notifier.notifyingOnSuccess { api.updateCardSubscriptionAmount(id, request) }

    /**
     * Backend retorna 204 em sucesso. Importante: deletar uma entry
     * parcelada remove TODAS as installments associadas e recalcula
     * statements futuros — backend lida com a cascata.
     *
     * scope (query, snake_case): "this" (default) ou "this_and_following" —
     * numa entry gerada por assinatura, decide se apaga só esta ou as futuras.
     */
    suspend fun deleteEntry(id: String, scope: String? = null) = notifier.notifyingOnSuccess {
        val response = api.deleteCardEntry(id, scope)
        if (!response.isSuccessful) {
            throw retrofit2.HttpException(response)
        }
    }
}
