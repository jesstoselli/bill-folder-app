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
