package com.billfolder.android.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs alinhados com BillFolder.Application.Dtos.Incomes (.NET).
 *
 * Modelo conceitual:
 *  - IncomeSource = fonte recorrente (salário CLT todo dia 5, aluguel
 *    de imóvel todo dia 10). Tem schedule definido por expectedDay e
 *    janela startDate..endDate.
 *  - IncomeEntry = ocorrência individual num ciclo. Pode ser:
 *      a) "filha" de uma source (sourceId preenchido) — gerada por
 *         recorrência ou manualmente vinculada
 *      b) one-off (sourceId null) — recebimento avulso
 *
 * Status enum: "expected" | "received" | "late" | "notOccurred".
 */

@Serializable
data class IncomeSourceResponse(
    @SerialName("id")            val id: String,
    @SerialName("origin")        val origin: String,
    @SerialName("originType")    val originType: String, // work/rent/investment/freelance/gift/other
    @SerialName("defaultAmount") val defaultAmount: Double,
    @SerialName("expectedDay")   val expectedDay: Int,
    @SerialName("startDate")     val startDate: String,
    @SerialName("endDate")       val endDate: String? = null,
    @SerialName("isActive")      val isActive: Boolean,
    @SerialName("createdAt")     val createdAt: String,
    @SerialName("updatedAt")     val updatedAt: String,
)

@Serializable
data class CreateIncomeSourceRequest(
    @SerialName("origin")        val origin: String,
    @SerialName("originType")    val originType: String,
    @SerialName("defaultAmount") val defaultAmount: Double,
    @SerialName("expectedDay")   val expectedDay: Int,
    @SerialName("startDate")     val startDate: String,
    @SerialName("endDate")       val endDate: String? = null,
)

@Serializable
data class IncomeEntryResponse(
    @SerialName("id")             val id: String,
    @SerialName("sourceId")       val sourceId: String? = null,
    @SerialName("sourceOrigin")   val sourceOrigin: String? = null,
    @SerialName("expectedAmount") val expectedAmount: Double,
    @SerialName("actualAmount")   val actualAmount: Double? = null,
    @SerialName("expectedDate")   val expectedDate: String,
    @SerialName("actualDate")     val actualDate: String? = null,
    @SerialName("status")         val status: String,
    @SerialName("notes")          val notes: String? = null,
    @SerialName("createdAt")      val createdAt: String,
    @SerialName("updatedAt")      val updatedAt: String,
)

@Serializable
data class CreateIncomeEntryRequest(
    @SerialName("sourceId")       val sourceId: String? = null,
    @SerialName("expectedAmount") val expectedAmount: Double,
    @SerialName("expectedDate")   val expectedDate: String,
    @SerialName("notes")          val notes: String? = null,
)

/**
 * PATCH parcial. Pra "confirmar recebimento", manda status="received"
 * + actualDate + actualAmount; backend pode auto-preencher se vierem null.
 */
@Serializable
data class UpdateIncomeEntryRequest(
    @SerialName("sourceId")       val sourceId: String? = null,
    @SerialName("expectedAmount") val expectedAmount: Double? = null,
    @SerialName("actualAmount")   val actualAmount: Double? = null,
    @SerialName("expectedDate")   val expectedDate: String? = null,
    @SerialName("actualDate")     val actualDate: String? = null,
    @SerialName("status")         val status: String? = null,
    @SerialName("notes")          val notes: String? = null,
)
