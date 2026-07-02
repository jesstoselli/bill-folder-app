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

/**
 * POST /v1/auth/forgot-password. Backend responde 200 SEMPRE
 * (proteção contra enumeration de usuários — não vaza se o email
 * está cadastrado). Body devCode só vem preenchido quando o backend
 * está em modo dev sem provedor de email — em prod é null.
 */
@Serializable
data class ForgotPasswordRequest(
    @SerialName("email") val email: String,
)

@Serializable
data class ForgotPasswordResponse(
    @SerialName("devCode") val devCode: String? = null,
)

/**
 * POST /v1/auth/reset-password. Backend responde 204 em sucesso ou
 * 400 { error, message } — códigos possíveis do backend:
 *  - validation_error: senha curta, email inválido, código não é 6 dígitos
 *  - invalid_reset_code: código não bate / expirou / já foi usado
 *
 * Após reset OK, todos os refresh tokens ativos do user são revogados
 * — sessions em outros devices caem no próximo refresh.
 */
@Serializable
data class ResetPasswordRequest(
    @SerialName("email")       val email: String,
    @SerialName("code")        val code: String,
    @SerialName("newPassword") val newPassword: String,
)
