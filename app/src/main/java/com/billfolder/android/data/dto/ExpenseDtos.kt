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
    // Provisionamento por baixa (ex: fisioterapia semanal). Numa despesa
    // "normal", occurrenceAmount/occurrencesTotal são null. Numa provisionada,
    // occurrenceAmount = valor de cada baixa, occurrencesTotal = quantas
    // baixas no ciclo, occurrencesPaid = quantas já foram dadas baixa, e
    // paidToDate = soma efetivamente paga. Está "em andamento" enquanto
    // occurrencesPaid < occurrencesTotal.
    @SerialName("occurrenceAmount")      val occurrenceAmount: Double? = null,
    @SerialName("occurrencesTotal")      val occurrencesTotal: Int? = null,
    @SerialName("occurrencesPaid")       val occurrencesPaid: Int = 0,
    @SerialName("paidToDate")            val paidToDate: Double = 0.0,
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

/**
 * POST /v1/expenses/{id}/pay-occurrence — "dar baixa" numa ocorrência
 * semanal de uma despesa provisionada. Todos os campos são opcionais:
 *  - paidDate: default hoje (backend)
 *  - amount: default occurrenceAmount da despesa
 *  - paidFromAccountId: registra de qual conta saiu (opcional)
 *
 * Retorna o ExpenseResponse atualizado (occurrencesPaid/paidToDate
 * incrementados). Só vale pra despesas provisionadas — o PATCH normal
 * com status=paid é REJEITADO (error "provisioned_expense", 400) nelas.
 */
@Serializable
data class PayOccurrenceRequest(
    @SerialName("paidDate")          val paidDate: String? = null,
    @SerialName("amount")            val amount: Double? = null,
    @SerialName("paidFromAccountId") val paidFromAccountId: String? = null,
)

/**
 * POST /v1/expenses/{id}/update-amount — reajusta o valor POR SESSÃO
 * (occurrenceAmount) de uma despesa provisionada. O total do mês
 * (expectedAmount) recalcula no backend = novo valor × nº de ocorrências.
 *
 * scope no body é camelCase ("this"/"thisAndFollowing") — igual ao reprice
 * de assinatura de cartão, diferente do snake_case do delete.
 */
@Serializable
data class RepriceProvisionedExpenseRequest(
    @SerialName("amount") val amount: Double,
    @SerialName("scope")  val scope: String,
)

/**
 * DTOs de "recorrência de despesa" (template que gera despesas
 * automaticamente a cada ciclo). Alinhado com
 * BillFolder.Application.Dtos.ExpenseRecurrences (.NET).
 *
 * frequency: enum string lowercase ("weekly"/"monthly") — mesma convenção
 * de serialização dos status de expense. Pra "weekly", weekday (0=domingo
 * .. 6=sábado) é usado e dueDay fica null; pra "monthly", é o contrário.
 */
@Serializable
data class CreateExpenseRecurrenceRequest(
    @SerialName("defaultLabel")      val defaultLabel: String,
    @SerialName("defaultAmount")     val defaultAmount: Double,
    @SerialName("defaultCategoryId") val defaultCategoryId: String,
    @SerialName("frequency")         val frequency: String,
    @SerialName("dueDay")            val dueDay: Int? = null,
    @SerialName("weekday")           val weekday: Int? = null,
    @SerialName("startDate")         val startDate: String,
    @SerialName("endDate")           val endDate: String? = null,
)

@Serializable
data class ExpenseRecurrenceResponse(
    @SerialName("id")                val id: String,
    @SerialName("defaultLabel")      val defaultLabel: String,
    @SerialName("defaultAmount")     val defaultAmount: Double,
    @SerialName("defaultCategoryId") val defaultCategoryId: String,
    @SerialName("defaultCategoryName") val defaultCategoryName: String? = null,
    @SerialName("frequency")         val frequency: String,
    @SerialName("dueDay")            val dueDay: Int? = null,
    @SerialName("weekday")           val weekday: Int? = null,
    @SerialName("startDate")         val startDate: String,
    @SerialName("endDate")           val endDate: String? = null,
    @SerialName("createdAt")         val createdAt: String,
    @SerialName("updatedAt")         val updatedAt: String,
)
