package com.billfolder.android.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** DTOs alinhados com BillFolder.Application.Dtos.Cards (.NET). */
@Serializable
data class CardStatementResponse(
    @SerialName("id")                  val id: String,
    @SerialName("cardId")              val cardId: String,
    @SerialName("cardName")            val cardName: String,
    @SerialName("periodStart")         val periodStart: String,
    @SerialName("periodEnd")           val periodEnd: String,
    @SerialName("dueDate")             val dueDate: String,
    @SerialName("status")              val status: String,
    @SerialName("paidDate")            val paidDate: String? = null,
    @SerialName("actualAmount")        val actualAmount: Double? = null,
    @SerialName("paidFromAccountId")   val paidFromAccountId: String? = null,
    @SerialName("paidFromAccountName") val paidFromAccountName: String? = null,
    @SerialName("totalAmount")         val totalAmount: Double,
    @SerialName("installmentsCount")   val installmentsCount: Int,
    @SerialName("linkedExpenseId")     val linkedExpenseId: String? = null,
    @SerialName("createdAt")           val createdAt: String,
    @SerialName("updatedAt")           val updatedAt: String,
)

@Serializable
data class PayCardStatementRequest(
    @SerialName("paidDate")          val paidDate: String,
    @SerialName("actualAmount")      val actualAmount: Double,
    @SerialName("paidFromAccountId") val paidFromAccountId: String? = null,
)
