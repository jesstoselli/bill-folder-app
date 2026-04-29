package com.billfolder.android.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateDailyExpenseRequest(
    @SerialName("date")       val date: String,        // "yyyy-MM-dd"
    @SerialName("label")      val label: String,
    @SerialName("amount")     val amount: Double,
    @SerialName("categoryId") val categoryId: String,  // UUID v7
    @SerialName("accountId")  val accountId: String,
    @SerialName("notes")      val notes: String? = null,
)

@Serializable
data class DailyExpenseResponse(
    @SerialName("id")           val id: String,
    @SerialName("date")         val date: String,
    @SerialName("label")        val label: String,
    @SerialName("amount")       val amount: Double,
    @SerialName("categoryId")   val categoryId: String,
    @SerialName("categoryName") val categoryName: String,
    @SerialName("accountId")    val accountId: String,
    @SerialName("accountName")  val accountName: String,
    @SerialName("notes")        val notes: String? = null,
    @SerialName("createdAt")    val createdAt: String,
    @SerialName("updatedAt")    val updatedAt: String,
)
