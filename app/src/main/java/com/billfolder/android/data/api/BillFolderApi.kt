package com.billfolder.android.data.api

import com.billfolder.android.data.dto.AuthResponse
import com.billfolder.android.data.dto.CardEntryResponse
import com.billfolder.android.data.dto.CategoryDto
import com.billfolder.android.data.dto.CheckingAccountResponse
import com.billfolder.android.data.dto.CreateCardEntryRequest
import com.billfolder.android.data.dto.CreateCreditCardAccountRequest
import com.billfolder.android.data.dto.CreateDailyExpenseRequest
import com.billfolder.android.data.dto.CreateExpenseRequest
import com.billfolder.android.data.dto.CreateIncomeEntryRequest
import com.billfolder.android.data.dto.CreateIncomeSourceRequest
import com.billfolder.android.data.dto.CreditCardAccountResponse
import com.billfolder.android.data.dto.CycleResponse
import com.billfolder.android.data.dto.DailyExpenseResponse
import com.billfolder.android.data.dto.ExpenseResponse
import com.billfolder.android.data.dto.HomeResponse
import com.billfolder.android.data.dto.IncomeEntryResponse
import com.billfolder.android.data.dto.IncomeSourceResponse
import com.billfolder.android.data.dto.LoginRequest
import com.billfolder.android.data.dto.LogoutRequest
import com.billfolder.android.data.dto.RefreshTokenRequest
import com.billfolder.android.data.dto.SignupRequest
import com.billfolder.android.data.dto.UpdateDailyExpenseRequest
import com.billfolder.android.data.dto.UpdateExpenseRequest
import com.billfolder.android.data.dto.UpdateIncomeEntryRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

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
    // Reference data — populam dropdowns de formulários
    // ------------------------------------------------------------------------

    /** Categorias do sistema (seed do backend, read-only). */
    @GET("categories")
    suspend fun getCategories(): List<CategoryDto>

    /** Contas correntes do usuário. */
    @GET("checking-accounts")
    suspend fun getCheckingAccounts(): List<CheckingAccountResponse>

    // ------------------------------------------------------------------------
    // Cycles
    // ------------------------------------------------------------------------

    /**
     * Retorna o ciclo aberto que cobre a data atual. 404 se o usuário ainda
     * não criou nenhum ciclo — repository converte isso pra fluxo de UI.
     */
    @GET("cycles/current")
    suspend fun getCurrentCycle(): CycleResponse

    // ------------------------------------------------------------------------
    // Daily expenses (despesas avulsas do dia-a-dia)
    // ------------------------------------------------------------------------

    /**
     * Lista despesas avulsas. Filtros opcionais por data e categoria;
     * pra "ver as do ciclo atual", manda from=cycle.startDate&to=cycle.endDate.
     */
    @GET("daily-expenses")
    suspend fun getDailyExpenses(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("categoryId") categoryId: String? = null,
    ): List<DailyExpenseResponse>

    @POST("daily-expenses")
    suspend fun createDailyExpense(
        @Body request: CreateDailyExpenseRequest,
    ): DailyExpenseResponse

    @PATCH("daily-expenses/{id}")
    suspend fun updateDailyExpense(
        @Path("id") id: String,
        @Body request: UpdateDailyExpenseRequest,
    ): DailyExpenseResponse

    @DELETE("daily-expenses/{id}")
    suspend fun deleteDailyExpense(@Path("id") id: String): Response<Unit>

    // ------------------------------------------------------------------------
    // Expenses (despesas com vencimento — luz, aluguel, etc)
    // ------------------------------------------------------------------------

    @GET("expenses")
    suspend fun getExpenses(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("status") status: String? = null,
        @Query("categoryId") categoryId: String? = null,
    ): List<ExpenseResponse>

    @POST("expenses")
    suspend fun createExpense(@Body request: CreateExpenseRequest): ExpenseResponse

    @PATCH("expenses/{id}")
    suspend fun updateExpense(
        @Path("id") id: String,
        @Body request: UpdateExpenseRequest,
    ): ExpenseResponse

    // ------------------------------------------------------------------------
    // Incomes — fontes recorrentes + entries individuais
    // ------------------------------------------------------------------------

    @GET("income-sources")
    suspend fun getIncomeSources(): List<IncomeSourceResponse>

    @POST("income-sources")
    suspend fun createIncomeSource(
        @Body request: CreateIncomeSourceRequest,
    ): IncomeSourceResponse

    @GET("income-entries")
    suspend fun getIncomeEntries(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("sourceId") sourceId: String? = null,
    ): List<IncomeEntryResponse>

    @POST("income-entries")
    suspend fun createIncomeEntry(
        @Body request: CreateIncomeEntryRequest,
    ): IncomeEntryResponse

    @PATCH("income-entries/{id}")
    suspend fun updateIncomeEntry(
        @Path("id") id: String,
        @Body request: UpdateIncomeEntryRequest,
    ): IncomeEntryResponse

    // ------------------------------------------------------------------------
    // Credit Cards (config dos cartões)
    // ------------------------------------------------------------------------

    @GET("credit-card-accounts")
    suspend fun getCreditCards(): List<CreditCardAccountResponse>

    @POST("credit-card-accounts")
    suspend fun createCreditCard(
        @Body request: CreateCreditCardAccountRequest,
    ): CreditCardAccountResponse

    @DELETE("credit-card-accounts/{id}")
    suspend fun deleteCreditCard(@Path("id") id: String): retrofit2.Response<Unit>

    // ------------------------------------------------------------------------
    // Card entries (compras com parcelamento opcional)
    // ------------------------------------------------------------------------

    @GET("card-entries")
    suspend fun getCardEntries(
        @Query("cardId") cardId: String? = null,
    ): List<CardEntryResponse>

    @POST("card-entries")
    suspend fun createCardEntry(
        @Body request: CreateCardEntryRequest,
    ): CardEntryResponse
}
