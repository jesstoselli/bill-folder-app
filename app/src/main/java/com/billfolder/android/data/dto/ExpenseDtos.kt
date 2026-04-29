package com.billfolder.android.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs alinhados com BillFolder.Application.Dtos.Expenses (.NET).
 * Status: enum string camelCase ("pending", "paid", "overdue") — overdue
 * é computed (não é stored): backend deriva quando dueDate passou e status
 * stored é Pending. Filtro por status no GET respeita essa regra.
 */

@Serializable
data class ExpenseResponse(
    @SerialName("id")                    val id: String,
    @SerialName("dueDate")               val dueDate: String,
    @SerialName("label")                 val label: String,
    @SerialName("expectedAmount")        val expectedAmount: Double,
    @SerialName("actualAmount")          val actualAmount: Double? = null,
    @SerialName("status")                val status: String,
    @SerialName("paidDate")              val paidDate: String? = null,
    @SerialName("paidFromAccountId")     val paidFromAccountId: String? = null,
    @SerialName("paidFromAccountName")   val paidFromAccountName: String? = null,
    @SerialName("categoryId")            val categoryId: String,
    @SerialName("categoryName")          val categoryName: String,
    @SerialName("linkedCardStatementId") val linkedCardStatementId: String? = null,
    @SerialName("notes")                 val notes: String? = null,
    @SerialName("createdAt")             val createdAt: String,
    @SerialName("updatedAt")             val updatedAt: String,
)

@Serializable
data class CreateExpenseRequest(
    @SerialName("dueDate")        val dueDate: String,
    @SerialName("label")          val label: String,
    @SerialName("expectedAmount") val expectedAmount: Double,
    @SerialName("categoryId")     val categoryId: String,
    @SerialName("notes")          val notes: String? = null,
)

/**
 * PATCH parcial. null nos campos = não muda. Pra marcar como pago, basta
 * mandar `status=paid` (+ opcionalmente paidFromAccountId pra registrar
 * de qual conta saiu); backend auto-preenche paidDate=hoje e
 * actualAmount=expectedAmount se vierem null.
 */
@Serializable
data class UpdateExpenseRequest(
    @SerialName("dueDate")           val dueDate: String? = null,
    @SerialName("label")             val label: String? = null,
    @SerialName("expectedAmount")    val expectedAmount: Double? = null,
    @SerialName("actualAmount")      val actualAmount: Double? = null,
    @SerialName("status")            val status: String? = null,
    @SerialName("paidDate")          val paidDate: String? = null,
    @SerialName("paidFromAccountId") val paidFromAccountId: String? = null,
    @SerialName("categoryId")        val categoryId: String? = null,
    @SerialName("notes")             val notes: String? = null,
)
