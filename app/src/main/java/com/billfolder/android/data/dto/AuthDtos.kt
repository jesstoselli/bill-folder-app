package com.billfolder.android.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs alinhados com BillFolder.Application.Dtos.Auth (.NET).
 * O backend serializa em camelCase, então @SerialName fica explícito
 * pra evitar surpresa caso a config global do kotlinx-serialization mude.
 */

@Serializable
data class SignupRequest(
    @SerialName("email")       val email: String,
    @SerialName("password")    val password: String,
    @SerialName("displayName") val displayName: String,
)

@Serializable
data class LoginRequest(
    @SerialName("email")    val email: String,
    @SerialName("password") val password: String,
)

@Serializable
data class RefreshTokenRequest(
    @SerialName("refreshToken") val refreshToken: String,
)

@Serializable
data class LogoutRequest(
    @SerialName("refreshToken") val refreshToken: String,
)

@Serializable
data class AuthResponse(
    @SerialName("accessToken")            val accessToken: String,
    @SerialName("refreshToken")           val refreshToken: String,
    @SerialName("accessTokenExpiresAt")   val accessTokenExpiresAt: String,
    @SerialName("refreshTokenExpiresAt")  val refreshTokenExpiresAt: String,
    @SerialName("user")                   val user: UserDto,
)

@Serializable
data class UserDto(
    @SerialName("id")          val id: String,
    @SerialName("email")       val email: String,
    @SerialName("displayName") val displayName: String,
)

/** Erros do backend usam shape { error, message }. */
@Serializable
data class ApiErrorBody(
    @SerialName("error")   val error: String? = null,
    @SerialName("message") val message: String? = null,
)
