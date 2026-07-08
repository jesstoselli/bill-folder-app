package com.billfolder.android.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs do domínio de poupança (BillFolder.Application.Dtos.Savings).
 *
 * Modelo:
 * - SavingsAccount: conta poupança, sempre vinculada 1:1 a uma
 *   CheckingAccount (backend valida "checking_already_has_savings"
 *   se a checking já tiver uma poupança associada). Identificada por
 *   bankName/branch/accountNumber + initialBalance (saldo de abertura,
 *   que serve de baseline pro cálculo de saldo atual quando somarmos
 *   transactions na Fase B).
 *
 * Fase A (atual): só CRUD da conta. SavingsTransaction* virão na Fase B
 * junto com a SavingsScreen de consumo (carousel + lista por ciclo).
 *
 * Espelha BillFolder.Application.Dtos.Savings.SavingsAccountDtos.
 */

@Serializable
data class SavingsAccountResponse(
    @SerialName("id")                val id: String,
    @SerialName("checkingAccountId") val checkingAccountId: String,
    @SerialName("bankName")          val bankName: String,
    @SerialName("branch")            val branch: String,
    @SerialName("accountNumber")     val accountNumber: String,
    @SerialName("initialBalance")    val initialBalance: Double,
    // Saldo corrente = inicial + Σ transações com sinal (calculado no backend).
    @SerialName("currentBalance")    val currentBalance: Double = 0.0,
    @SerialName("createdAt")         val createdAt: String,
    @SerialName("updatedAt")         val updatedAt: String,
)

@Serializable
data class CreateSavingsAccountRequest(
    @SerialName("checkingAccountId") val checkingAccountId: String,
    @SerialName("bankName")          val bankName: String,
    @SerialName("branch")            val branch: String,
    @SerialName("accountNumber")     val accountNumber: String,
    @SerialName("initialBalance")    val initialBalance: Double,
)

/**
 * PATCH parcial — null = não muda. Backend não permite mudar a
 * checkingAccountId associada (vínculo é fixo na criação); só
 * bankName/branch/accountNumber/initialBalance são editáveis.
 *
 * Espelha BillFolder.Application.Dtos.Savings.UpdateSavingsAccountRequest.
 */
@Serializable
data class UpdateSavingsAccountRequest(
    @SerialName("bankName")       val bankName: String? = null,
    @SerialName("branch")         val branch: String? = null,
    @SerialName("accountNumber")  val accountNumber: String? = null,
    @SerialName("initialBalance") val initialBalance: Double? = null,
)
