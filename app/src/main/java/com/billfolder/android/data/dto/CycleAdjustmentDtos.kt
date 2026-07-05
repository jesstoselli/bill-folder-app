package com.billfolder.android.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * "Ajustes do ciclo" — entradas/saídas avulsas que NÃO são despesa regular,
 * receita recorrente, compra no cartão nem despesa avulsa do dia-a-dia.
 *
 * Casos de uso:
 *  - Vendeu algo usado (bicicleta, câmera etc)
 *  - Amigo devolveu dinheiro emprestado
 *  - Saque da poupança pra usar no ciclo (linka pra SavingsTransaction)
 *  - Multa/estorno inesperado
 *  - Presente/mesada eventual
 *
 * Tipos: "inflow" (entra dinheiro) ou "outflow" (sai). Backend serializa
 * como string camelCase via JsonStringEnumConverter (ver Program.cs).
 */
@Serializable
data class CycleAdjustmentResponse(
    @SerialName("id")                          val id: String,
    @SerialName("type")                        val type: String,
    @SerialName("label")                       val label: String,
    @SerialName("amount")                      val amount: Double,
    @SerialName("date")                        val date: String,
    /** Se veio de um saque de poupança, aponta pra SavingsTransaction original. */
    @SerialName("sourceSavingsTransactionId")  val sourceSavingsTransactionId: String? = null,
    @SerialName("createdAt")                   val createdAt: String,
    @SerialName("updatedAt")                   val updatedAt: String,
)

@Serializable
data class CreateCycleAdjustmentRequest(
    @SerialName("type")                        val type: String,
    @SerialName("label")                       val label: String,
    @SerialName("amount")                      val amount: Double,
    @SerialName("date")                        val date: String,
    @SerialName("sourceSavingsTransactionId")  val sourceSavingsTransactionId: String? = null,
)

/**
 * PATCH parcial — só campos não-nulos são enviados. Backend usa null
 * pra "não mudar", não pra "limpar" (exceto sourceSavingsTransactionId
 * que também aceita null explícito pra desatrelar).
 */
@Serializable
data class UpdateCycleAdjustmentRequest(
    @SerialName("type")                        val type: String? = null,
    @SerialName("label")                       val label: String? = null,
    @SerialName("amount")                      val amount: Double? = null,
    @SerialName("date")                        val date: String? = null,
    @SerialName("sourceSavingsTransactionId")  val sourceSavingsTransactionId: String? = null,
)

/** Constantes pros valores válidos de type. Espelha CycleAdjustmentType do backend. */
object CycleAdjustmentTypes {
    const val INFLOW = "inflow"
    const val OUTFLOW = "outflow"
    val ALL = listOf(INFLOW, OUTFLOW)
}
