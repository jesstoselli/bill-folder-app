package com.billfolder.android.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs alinhados com BillFolder.Application.Dtos.Home (.NET).
 * Decimais chegam como número JSON — usamos Double aqui pra simplicidade do MVP.
 * Datas (DateOnly/DateTime) chegam como string ISO; deixamos como String e
 * parseamos só onde precisar (camada de UI).
 * Enums chegam camelCase string ("pending", "paid", "overdue", "open", "closed").
 */

@Serializable
data class HomeResponse(
    @SerialName("cycle")             val cycle: HomeCycleDto,
    @SerialName("balance")           val balance: HomeBalanceDto,
    @SerialName("incomeBreakdown")   val incomeBreakdown: HomeIncomeBreakdownDto,
    @SerialName("expenseBreakdown")  val expenseBreakdown: HomeExpenseBreakdownDto,
    @SerialName("upcomingExpenses")  val upcomingExpenses: List<HomeUpcomingExpenseDto>,
    @SerialName("cardStatementsInCycle") val cardStatementsInCycle: List<HomeCardStatementDto>,
    // Default empty pra retrocompat: se um build antigo do app pegar uma
    // resposta nova do backend, ainda parseia. Inverso (backend antigo + app
    // novo) também funciona via ignoreUnknownKeys + valor default.
    @SerialName("categoryBreakdown") val categoryBreakdown: List<HomeCategoryBreakdownDto> = emptyList(),
)

@Serializable
data class HomeCycleDto(
    @SerialName("id")        val id: String,
    @SerialName("startDate") val startDate: String,
    @SerialName("endDate")   val endDate: String,
    @SerialName("label")     val label: String,
)

@Serializable
data class HomeBalanceDto(
    @SerialName("checkingAccountsTotal")  val checkingAccountsTotal: Double,
    @SerialName("expectedIncome")         val expectedIncome: Double,
    @SerialName("receivedIncome")         val receivedIncome: Double,
    @SerialName("expectedExpenses")       val expectedExpenses: Double,
    @SerialName("paidExpenses")           val paidExpenses: Double,
    @SerialName("expectedCardStatements") val expectedCardStatements: Double,
    @SerialName("dailyExpensesSpent")     val dailyExpensesSpent: Double,
    @SerialName("remaining")              val remaining: Double,
)

@Serializable
data class HomeIncomeBreakdownDto(
    @SerialName("expected")    val expected: Int,
    @SerialName("received")    val received: Int,
    @SerialName("late")        val late: Int,
    @SerialName("notOccurred") val notOccurred: Int,
)

@Serializable
data class HomeExpenseBreakdownDto(
    @SerialName("pending") val pending: Int,
    @SerialName("overdue") val overdue: Int,
    @SerialName("paid")    val paid: Int,
)

@Serializable
data class HomeUpcomingExpenseDto(
    @SerialName("id")             val id: String,
    @SerialName("label")          val label: String,
    @SerialName("dueDate")        val dueDate: String,
    @SerialName("expectedAmount") val expectedAmount: Double,
    @SerialName("status")         val status: String,
    @SerialName("categoryName")   val categoryName: String,
)

@Serializable
data class HomeCardStatementDto(
    @SerialName("id")          val id: String,
    @SerialName("cardId")      val cardId: String,
    @SerialName("cardName")    val cardName: String,
    @SerialName("dueDate")     val dueDate: String,
    @SerialName("totalAmount") val totalAmount: Double,
    @SerialName("status")      val status: String,
)

/**
 * Soma agregada por categoria do que passa pelo ciclo. Backend retorna
 * já ordenado por valor descendente.
 */
@Serializable
data class HomeCategoryBreakdownDto(
    @SerialName("categoryId")   val categoryId: String,
    @SerialName("categoryKey")  val categoryKey: String,
    @SerialName("categoryName") val categoryName: String,
    @SerialName("amount")       val amount: Double,
)
