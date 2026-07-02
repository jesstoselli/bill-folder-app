package com.billfolder.android.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CheckingAccountResponse(
    @SerialName("id")             val id: String,
    @SerialName("bankName")       val bankName: String,
    @SerialName("branch")         val branch: String? = null,
    @SerialName("accountNumber")  val accountNumber: String? = null,
    @SerialName("initialBalance") val initialBalance: Double,
    @SerialName("isPrimary")      val isPrimary: Boolean,
    @SerialName("createdAt")      val createdAt: String,
    @SerialName("updatedAt")      val updatedAt: String,
)

/**
 * POST /v1/checking-accounts. Backend valida: bankName/branch/accountNumber
 * obrigatórios (max 100/20/30), initialBalance >= 0.
 *
 * Sobre isPrimary: backend garante invariante "no máximo 1 primary por user".
 * Se marcarmos essa conta como primary, o service desmarca automaticamente
 * as outras. Não precisa validação client-side.
 */
@Serializable
data class CreateCheckingAccountRequest(
    @SerialName("bankName")       val bankName: String,
    @SerialName("branch")         val branch: String,
    @SerialName("accountNumber")  val accountNumber: String,
    @SerialName("initialBalance") val initialBalance: Double,
    @SerialName("isPrimary")      val isPrimary: Boolean,
)

/**
 * PATCH parcial — null = não muda. Mesma invariante do isPrimary: se
 * setamos true, backend desmarca as outras; se setamos false E esta era
 * a primary, ela vira não-primary (nenhuma primary).
 */
@Serializable
data class UpdateCheckingAccountRequest(
    @SerialName("bankName")       val bankName: String? = null,
    @SerialName("branch")         val branch: String? = null,
    @SerialName("accountNumber")  val accountNumber: String? = null,
    @SerialName("initialBalance") val initialBalance: Double? = null,
    @SerialName("isPrimary")      val isPrimary: Boolean? = null,
)
