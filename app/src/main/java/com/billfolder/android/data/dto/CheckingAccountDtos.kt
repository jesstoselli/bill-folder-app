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
