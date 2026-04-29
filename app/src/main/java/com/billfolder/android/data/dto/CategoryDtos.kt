package com.billfolder.android.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO da categoria. Categorias são SISTEMA (read-only, populadas via seed).
 * Não há endpoint de POST/PATCH/DELETE — UI consome a lista pra dropdowns
 * de transação.
 */
@Serializable
data class CategoryDto(
    @SerialName("id")           val id: String,
    @SerialName("key")          val key: String,
    @SerialName("namePt")       val namePt: String,
    @SerialName("isSystem")     val isSystem: Boolean,
    @SerialName("displayOrder") val displayOrder: Int,
)
