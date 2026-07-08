package com.billfolder.android.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs do domínio de cartões (BillFolder.Application.Dtos.CreditCards
 * + .Cards). Conceito:
 *
 * - CreditCardAccount = config do cartão (banco, bandeira, fechamento, vencimento).
 *   Tipo "Itaú Personnalité" — entidade durável, raramente muda.
 * - CardEntry = compra individual no cartão. Pode ser parcelada
 *   (installments). Backend distribui as parcelas entre as faturas
 *   (statements) automaticamente baseado em closingDay/dueDay.
 * - Statement (CardStatement) = fatura mensal. Não temos um DTO separado
 *   aqui — usamos HomeCardStatementDto na Home.
 */

@Serializable
data class CreditCardAccountResponse(
    @SerialName("id")          val id: String,
    @SerialName("name")        val name: String,
    @SerialName("issuerBank")  val issuerBank: String? = null,
    @SerialName("brand")       val brand: String? = null,
    @SerialName("closingDay")  val closingDay: Int,
    @SerialName("dueDay")      val dueDay: Int,
    @SerialName("createdAt")   val createdAt: String,
    @SerialName("updatedAt")   val updatedAt: String,
)

@Serializable
data class CreateCreditCardAccountRequest(
    @SerialName("name")       val name: String,
    @SerialName("issuerBank") val issuerBank: String? = null,
    @SerialName("brand")      val brand: String? = null,
    @SerialName("closingDay") val closingDay: Int,
    @SerialName("dueDay")     val dueDay: Int,
)

/**
 * PATCH parcial — null = não muda. Backend não recalcula statements/
 * installments existentes em mudanças de closingDay/dueDay; só novos
 * lançamentos usam os dias atualizados. Vale avisar o user na sheet
 * de edit.
 *
 * Espelha BillFolder.Application.Dtos.CreditCards.UpdateCreditCardAccountRequest.
 */
@Serializable
data class UpdateCreditCardAccountRequest(
    @SerialName("name")       val name: String? = null,
    @SerialName("issuerBank") val issuerBank: String? = null,
    @SerialName("brand")      val brand: String? = null,
    @SerialName("closingDay") val closingDay: Int? = null,
    @SerialName("dueDay")     val dueDay: Int? = null,
)

/**
 * Card entry (compra). InstallmentsCount é o nº total de parcelas
 * (1 = à vista). O backend gera automaticamente as N installments
 * de Amount/N cada, distribuindo entre statements consecutivos
 * baseado em closingDay/purchaseDate.
 */
@Serializable
data class CardEntryResponse(
    @SerialName("id")                val id: String,
    @SerialName("cardId")            val cardId: String,
    @SerialName("cardName")          val cardName: String,
    @SerialName("purchaseDate")      val purchaseDate: String,
    @SerialName("label")             val label: String,
    @SerialName("totalAmount")       val totalAmount: Double,
    @SerialName("installmentsCount") val installmentsCount: Int,
    @SerialName("categoryId")        val categoryId: String,
    @SerialName("categoryName")      val categoryName: String,
    @SerialName("notes")             val notes: String? = null,
    @SerialName("createdAt")         val createdAt: String,
    @SerialName("updatedAt")         val updatedAt: String,
    @SerialName("installments")      val installments: List<EntryInstallmentDto> = emptyList(),
)

@Serializable
data class EntryInstallmentDto(
    @SerialName("installmentId")     val installmentId: String,
    @SerialName("installmentNumber") val installmentNumber: Int,
    @SerialName("amount")            val amount: Double,
    @SerialName("statementId")       val statementId: String,
    @SerialName("statementDueDate")  val statementDueDate: String,
)

@Serializable
data class CreateCardEntryRequest(
    @SerialName("cardId")             val cardId: String,
    @SerialName("purchaseDate")       val purchaseDate: String,
    @SerialName("label")              val label: String,
    @SerialName("totalAmount")        val totalAmount: Double,
    @SerialName("installmentsCount")  val installmentsCount: Int,
    @SerialName("categoryId")         val categoryId: String,
    @SerialName("notes")              val notes: String? = null,
)

/**
 * Atualização limitada — backend (UpdateCardEntryRequest .NET) só aceita
 * label, categoria e notes. Mudar valor/parcelas/data exigiria recalcular
 * todas as installments e movê-las entre statements/faturas, operação
 * complexa que merece endpoint dedicado no futuro.
 *
 * Na UI isso significa: em modo edit, os campos cardId/purchaseDate/
 * totalAmount/installmentsCount ficam disabled — só label, categoria
 * e notes podem mudar.
 */
@Serializable
data class UpdateCardEntryRequest(
    @SerialName("label")      val label: String? = null,
    @SerialName("categoryId") val categoryId: String? = null,
    @SerialName("notes")      val notes: String? = null,
)

/**
 * "Assinatura" de cartão: template que gera um card entry recorrente a cada
 * ciclo (ex: Netflix, Spotify). Alinhado com
 * BillFolder.Application.Dtos.Recurrences.CreateCardEntryRecurrenceRequest (.NET).
 *
 * dayOfMonth (short no backend) = dia do vencimento no mês (1..31). Datas em
 * ISO yyyy-MM-dd (DateOnly no backend). defaultAmount é decimal no backend —
 * mapeado pra Double aqui como nas demais DTOs de valor.
 */
@Serializable
data class CreateCardEntryRecurrenceRequest(
    @SerialName("cardId")            val cardId: String,
    @SerialName("defaultLabel")      val defaultLabel: String,
    @SerialName("defaultAmount")     val defaultAmount: Double,
    @SerialName("defaultCategoryId") val defaultCategoryId: String,
    @SerialName("dayOfMonth")        val dayOfMonth: Int,
    @SerialName("startDate")         val startDate: String,
    @SerialName("endDate")           val endDate: String? = null,
)

@Serializable
data class CardEntryRecurrenceResponse(
    @SerialName("id")                  val id: String,
    @SerialName("cardId")              val cardId: String,
    @SerialName("cardName")            val cardName: String,
    @SerialName("defaultLabel")        val defaultLabel: String,
    @SerialName("defaultAmount")       val defaultAmount: Double,
    @SerialName("defaultCategoryId")   val defaultCategoryId: String,
    @SerialName("defaultCategoryName") val defaultCategoryName: String,
    @SerialName("dayOfMonth")          val dayOfMonth: Int,
    @SerialName("startDate")           val startDate: String,
    @SerialName("endDate")             val endDate: String? = null,
    @SerialName("isActive")            val isActive: Boolean,
    @SerialName("createdAt")           val createdAt: String,
    @SerialName("updatedAt")           val updatedAt: String,
)

/**
 * POST /card-entries/{id}/update-amount — "reprecificar" uma assinatura.
 * scope é enum JSON camelCase (JsonStringEnumConverter no backend): "this" ou
 * "thisAndFollowing". ATENÇÃO: difere do scope do DELETE, que é query param
 * snake_case ("this"/"this_and_following"). Modelado como String pura — o call
 * site manda o literal exato.
 */
@Serializable
data class UpdateCardSubscriptionAmountRequest(
    @SerialName("amount") val amount: Double,
    @SerialName("scope")  val scope: String,
)
