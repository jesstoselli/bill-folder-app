package com.billfolder.android.data.api

import com.billfolder.android.data.dto.AuthResponse
import com.billfolder.android.data.dto.CreateDailyExpenseRequest
import com.billfolder.android.data.dto.DailyExpenseResponse
import com.billfolder.android.data.dto.HomeResponse
import com.billfolder.android.data.dto.LoginRequest
import com.billfolder.android.data.dto.LogoutRequest
import com.billfolder.android.data.dto.RefreshTokenRequest
import com.billfolder.android.data.dto.SignupRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Cliente Retrofit pro BillFolder.Api.
 *
 * Convenções:
 * - Auth endpoints retornam Response<T> pra a gente conseguir ler 4xx body
 *   (mensagens de erro vêm em JSON e queremos surface pra UI).
 * - Demais endpoints retornam o tipo cru e deixam IOException/HttpException
 *   propagarem pro repositório.
 */
interface BillFolderApi {

    // ------------------------------------------------------------------------
    // Auth
    // ------------------------------------------------------------------------

    @POST("auth/signup")
    suspend fun signup(@Body request: SignupRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshTokenRequest): Response<AuthResponse>

    @POST("auth/logout")
    suspend fun logout(@Body request: LogoutRequest): Response<Unit>

    // ------------------------------------------------------------------------
    // Home (dashboard agregado)
    // ------------------------------------------------------------------------

    @GET("home")
    suspend fun getHome(): HomeResponse

    // ------------------------------------------------------------------------
    // Daily expenses (despesas avulsas do dia-a-dia)
    // ------------------------------------------------------------------------

    @POST("daily-expenses")
    suspend fun createDailyExpense(
        @Body request: CreateDailyExpenseRequest,
    ): DailyExpenseResponse
}
