package com.billfolder.android.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs do domínio de transações de poupança (BillFolder.Application.Dtos.Savings.SavingsTransactionDtos).
 *
 * Modelo:
 * - SavingsTransaction representa uma movimentação numa SavingsAccount.
 *   Existe sempre vinculada a uma poupança (savingsAccountId obrigatório).
 *   Soma com sinal varia por tipo:
 *      Deposit, Yield, TransferIn  → entrada (+amount no saldo)
 *      Withdrawal, TransferOut     → saída (-amount no saldo)
 *
 *   O backend retorna `amount` sempre como valor absoluto positivo; é a
 *   UI que aplica sinal/cor com base no `type`. (Convenção pra evitar
 *   double-negative: "withdrawal de -100" vs "withdrawal de 100".)
 *
 * - linkedTransactionId é usado pra par TransferOut/TransferIn (mesmo
 *   movimento entre 2 poupanças do mesmo usuário). Fase B atual não
 *   suporta criar transferências pela UI — chega só Deposit/Withdrawal/
 *   Yield. Refinamento de transferência fica pra depois.
 *
 * Backend serializa o enum SavingsTransactionType como camelCase via
 * JsonStringEnumConverter(JsonNamingPolicy.CamelCase): "deposit",
 * "withdrawal", "yield", "transferOut", "transferIn". Mantemos como
 * String aqui (mesma convenção dos outros enums do app — originType,
 * status etc) e expomos as constantes em SavingsTransactionTypes.
 *
 * Espelha BillFolder.Application.Dtos.Savings.SavingsTransactionDtos.
 */

@Serializable
data class SavingsTransactionResponse(
    @SerialName("id")                  val id: String,
    @SerialName("savingsAccountId")    val savingsAccountId: String,
    @SerialName("type")                val type: String, // ver SavingsTransactionTypes
    @SerialName("amount")              val amount: Double,
    @SerialName("date")                val date: String,  // ISO yyyy-MM-dd
    @SerialName("label")               val label: String? = null,
    @SerialName("linkedTransactionId") val linkedTransactionId: String? = null,
    @SerialName("createdAt")           val createdAt: String,
    @SerialName("updatedAt")           val updatedAt: String,
)

@Serializable
data class CreateSavingsTransactionRequest(
    @SerialName("savingsAccountId")    val savingsAccountId: String,
    @SerialName("type")                val type: String,
    @SerialName("amount")              val amount: Double,
    @SerialName("date")                val date: String,
    @SerialName("label")               val label: String? = null,
    @SerialName("linkedTransactionId") val linkedTransactionId: String? = null,
)

/**
 * PATCH parcial — null = não muda. Backend permite editar tipo, valor,
 * data e label. linkedTransactionId só importa em TransferOut/TransferIn,
 * que ainda não estão na UI.
 *
 * Espelha BillFolder.Application.Dtos.Savings.UpdateSavingsTransactionRequest.
 */
@Serializable
data class UpdateSavingsTransactionRequest(
    @SerialName("type")                val type: String? = null,
    @SerialName("amount")              val amount: Double? = null,
    @SerialName("date")                val date: String? = null,
    @SerialName("label")               val label: String? = null,
    @SerialName("linkedTransactionId") val linkedTransactionId: String? = null,
)

/**
 * Constantes pros valores serializados pelo backend (camelCase via
 * JsonNamingPolicy.CamelCase). Usar essas constantes em VMs/UI ao
 * invés de literais soltas — evita typo e dá grep-ability.
 *
 * Fase B atual exibe e cria apenas DEPOSIT/WITHDRAWAL/YIELD pela UI;
 * TRANSFER_OUT/TRANSFER_IN são reconhecidos no read (rendem corretamente
 * na lista) mas a sheet de create/edit não oferece a opção. Quando a
 * feature de transferência entrar, dá pra reusar essas mesmas constantes.
 */
object SavingsTransactionTypes {
    const val DEPOSIT      = "deposit"
    const val WITHDRAWAL   = "withdrawal"
    const val YIELD        = "yield"
    const val TRANSFER_OUT = "transferOut"
    const val TRANSFER_IN  = "transferIn"

    /** Tipos que a sheet de create/edit oferece em Fase B (sem transferência). */
    val CREATABLE_BY_USER: List<String> = listOf(DEPOSIT, WITHDRAWAL, YIELD)

    /** True se o tipo soma positivo no saldo (entrada). */
    fun isInflow(type: String): Boolean =
        type == DEPOSIT || type == YIELD || type == TRANSFER_IN
}
