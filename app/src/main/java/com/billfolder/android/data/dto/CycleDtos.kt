package com.billfolder.android.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs alinhados com BillFolder.Application.Dtos.Cycles (.NET).
 * Datas (DateOnly) chegam como string ISO "yyyy-MM-dd".
 */
@Serializable
data class CycleResponse(
    @SerialName("id")                     val id: String,
    @SerialName("startDate")              val startDate: String,
    @SerialName("endDate")                val endDate: String,
    @SerialName("label")                  val label: String,
    @SerialName("isRecurrenceGenerated")  val isRecurrenceGenerated: Boolean,
    @SerialName("isCurrent")              val isCurrent: Boolean,
    @SerialName("createdAt")              val createdAt: String,
    @SerialName("updatedAt")              val updatedAt: String,
)

/**
 * POST /v1/cycles. Validação backend (FluentValidation):
 *  - startDate, endDate obrigatórios e startDate < endDate
 *  - label: obrigatório, max 100 chars
 *
 * Conflito 409 "duplicate_start_date" se já existir um ciclo do user
 * com a mesma startDate — o caller (sheet) trata e mostra mensagem em PT.
 */
@Serializable
data class CreateCycleRequest(
    @SerialName("startDate") val startDate: String,
    @SerialName("endDate")   val endDate: String,
    @SerialName("label")     val label: String,
)
