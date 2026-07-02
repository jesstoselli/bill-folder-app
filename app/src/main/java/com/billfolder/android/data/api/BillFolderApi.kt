package com.billfolder.android.data.api

import com.billfolder.android.data.dto.AuthResponse
import com.billfolder.android.data.dto.ForgotPasswordRequest
import com.billfolder.android.data.dto.ForgotPasswordResponse
import com.billfolder.android.data.dto.CardEntryResponse
import com.billfolder.android.data.dto.CategoryDto
import com.billfolder.android.data.dto.CheckingAccountResponse
import com.billfolder.android.data.dto.CreateCardEntryRequest
import com.billfolder.android.data.dto.CreateCreditCardAccountRequest
import com.billfolder.android.data.dto.CreateDailyExpenseRequest
import com.billfolder.android.data.dto.CreateExpenseRequest
import com.billfolder.android.data.dto.CreateCheckingAccountRequest
import com.billfolder.android.data.dto.CreateCycleRequest
import com.billfolder.android.data.dto.CreateIncomeEntryRequest
import com.billfolder.android.data.dto.CreateIncomeSourceRequest
import com.billfolder.android.data.dto.CreateSavingsAccountRequest
import com.billfolder.android.data.dto.CreateSavingsTransactionRequest
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
import com.billfolder.android.data.dto.ResetPasswordRequest
import com.billfolder.android.data.dto.SavingsAccountResponse
import com.billfolder.android.data.dto.SavingsTransactionResponse
import com.billfolder.android.data.dto.SignupRequest
import com.billfolder.android.data.dto.UpdateCardEntryRequest
import com.billfolder.android.data.dto.UpdateCheckingAccountRequest
import com.billfolder.android.data.dto.UpdateCreditCardAccountRequest
import com.billfolder.android.data.dto.UpdateDailyExpenseRequest
import com.billfolder.android.data.dto.UpdateExpenseRequest
import com.billfolder.android.data.dto.UpdateIncomeEntryRequest
import com.billfolder.android.data.dto.UpdateIncomeSourceRequest
import com.billfolder.android.data.dto.UpdateSavingsAccountRequest
import com.billfolder.android.data.dto.UpdateSavingsTransactionRequest
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

    /**
     * Inicia fluxo de reset de senha. Backend sempre retorna 200
     * (proteção anti-enumeration). devCode no body só vem em dev
     * sem provider de email; em prod é null.
     */
    @POST("auth/forgot-password")
    suspend fun forgotPassword(
        @Body request: ForgotPasswordRequest,
    ): Response<ForgotPasswordResponse>

    /**
     * Conclui reset. 204 em sucesso, 400 { error, message } em caso
     * de código inválido/expirado ou validation error.
     */
    @POST("auth/reset-password")
    suspend fun resetPassword(
        @Body request: ResetPasswordRequest,
    ): Response<Unit>

    // ------------------------------------------------------------------------
    // Home (dashboard agregado)
    // ------------------------------------------------------------------------

    /**
     * Snapshot do dashboard. Sem cycleId, o backend usa o ciclo atual.
     * Com cycleId, o backend usa aquele ciclo específico — usado pela
     * navegação prev/next de ciclos no CycleNavigator da Home.
     */
    @GET("home")
    suspend fun getHome(@Query("cycleId") cycleId: String? = null): HomeResponse

    // ------------------------------------------------------------------------
    // Reference data — populam dropdowns de formulários
    // ------------------------------------------------------------------------

    /** Categorias do sistema (seed do backend, read-only). */
    @GET("categories")
    suspend fun getCategories(): List<CategoryDto>

    /** Contas correntes do usuário. */
    @GET("checking-accounts")
    suspend fun getCheckingAccounts(): List<CheckingAccountResponse>

    @POST("checking-accounts")
    suspend fun createCheckingAccount(
        @Body request: CreateCheckingAccountRequest,
    ): CheckingAccountResponse

    /**
     * PATCH parcial. Se isPrimary=true, backend desmarca as outras
     * automaticamente (invariante "no máximo 1 primary por user").
     */
    @PATCH("checking-accounts/{id}")
    suspend fun updateCheckingAccount(
        @Path("id") id: String,
        @Body request: UpdateCheckingAccountRequest,
    ): CheckingAccountResponse

    /**
     * 204 em sucesso. CASCADE no schema: apagar a conta remove savings
     * vinculada + income entries que apontavam pra ela (se houver).
     */
    @DELETE("checking-accounts/{id}")
    suspend fun deleteCheckingAccount(@Path("id") id: String): Response<Unit>

    // ------------------------------------------------------------------------
    // Cycles
    // ------------------------------------------------------------------------

    /**
     * Retorna o ciclo aberto que cobre a data atual. 404 se o usuário ainda
     * não criou nenhum ciclo — repository converte isso pra fluxo de UI.
     */
    @GET("cycles/current")
    suspend fun getCurrentCycle(): CycleResponse

    /**
     * Lista ciclos do usuário. Sem from/to, retorna todos. Backend ordena
     * por startDate asc. Usado pela navegação prev/next de ciclos —
     * carregamos a lista completa e escolhemos o adjacente client-side.
     */
    @GET("cycles")
    suspend fun listCycles(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
    ): List<CycleResponse>

    /**
     * Cria um novo ciclo. 409 "duplicate_start_date" se já houver um ciclo
     * do user começando na mesma data (cycles não podem se sobrepor por
     * startDate). Sheet de criar ciclo intercepta e mostra mensagem em PT.
     */
    @POST("cycles")
    suspend fun createCycle(@Body request: CreateCycleRequest): CycleResponse

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

    @DELETE("expenses/{id}")
    suspend fun deleteExpense(@Path("id") id: String): Response<Unit>

    // ------------------------------------------------------------------------
    // Incomes — fontes recorrentes + entries individuais
    // ------------------------------------------------------------------------

    @GET("income-sources")
    suspend fun getIncomeSources(): List<IncomeSourceResponse>

    @POST("income-sources")
    suspend fun createIncomeSource(
        @Body request: CreateIncomeSourceRequest,
    ): IncomeSourceResponse

    @PATCH("income-sources/{id}")
    suspend fun updateIncomeSource(
        @Path("id") id: String,
        @Body request: UpdateIncomeSourceRequest,
    ): IncomeSourceResponse

    @DELETE("income-sources/{id}")
    suspend fun deleteIncomeSource(@Path("id") id: String): Response<Unit>

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

    @DELETE("income-entries/{id}")
    suspend fun deleteIncomeEntry(@Path("id") id: String): Response<Unit>

    // ------------------------------------------------------------------------
    // Credit Cards (config dos cartões)
    // ------------------------------------------------------------------------

    @GET("credit-card-accounts")
    suspend fun getCreditCards(): List<CreditCardAccountResponse>

    @POST("credit-card-accounts")
    suspend fun createCreditCard(
        @Body request: CreateCreditCardAccountRequest,
    ): CreditCardAccountResponse

    @PATCH("credit-card-accounts/{id}")
    suspend fun updateCreditCard(
        @Path("id") id: String,
        @Body request: UpdateCreditCardAccountRequest,
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

    /**
     * PATCH limitado: backend só aceita label/categoryId/notes. Outros campos
     * (date, amount, parcelas) precisariam recalcular installments — fica
     * pra endpoint dedicado no futuro.
     */
    @PATCH("card-entries/{id}")
    suspend fun updateCardEntry(
        @Path("id") id: String,
        @Body request: UpdateCardEntryRequest,
    ): CardEntryResponse

    @DELETE("card-entries/{id}")
    suspend fun deleteCardEntry(@Path("id") id: String): Response<Unit>

    // ------------------------------------------------------------------------
    // Savings accounts (CRUD da conta poupança)
    //
    // Vínculo 1:1 com checking — backend retorna 409 "checking_already_has_savings"
    // se a checking selecionada já tiver uma poupança. PATCH só aceita
    // bankName/branch/accountNumber/initialBalance; checkingAccountId é
    // imutável.
    // ------------------------------------------------------------------------

    @GET("savings-accounts")
    suspend fun getSavingsAccounts(): List<SavingsAccountResponse>

    @POST("savings-accounts")
    suspend fun createSavingsAccount(
        @Body request: CreateSavingsAccountRequest,
    ): SavingsAccountResponse

    @PATCH("savings-accounts/{id}")
    suspend fun updateSavingsAccount(
        @Path("id") id: String,
        @Body request: UpdateSavingsAccountRequest,
    ): SavingsAccountResponse

    @DELETE("savings-accounts/{id}")
    suspend fun deleteSavingsAccount(@Path("id") id: String): Response<Unit>

    // ------------------------------------------------------------------------
    // Savings transactions (movimentações da poupança)
    //
    // Atenção: route group separado de /savings-accounts (não nested). O
    // backend filtra por savingsAccountId via query param.
    //
    // Filtros: from/to em ISO yyyy-MM-dd; type aceita os 5 valores camelCase
    // (deposit/withdrawal/yield/transferOut/transferIn) — backend valida
    // case-insensitive e retorna 400 'invalid_type' se não bater.
    //
    // Pra "transações dessa poupança no ciclo atual", manda:
    //   savingsAccountId=<id>&from=<cycle.start>&to=<cycle.end>
    //
    // backend serializa enum como camelCase via JsonStringEnumConverter.
    // ------------------------------------------------------------------------

    @GET("savings-transactions")
    suspend fun getSavingsTransactions(
        @Query("savingsAccountId") savingsAccountId: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("type") type: String? = null,
    ): List<SavingsTransactionResponse>

    @POST("savings-transactions")
    suspend fun createSavingsTransaction(
        @Body request: CreateSavingsTransactionRequest,
    ): SavingsTransactionResponse

    @PATCH("savings-transactions/{id}")
    suspend fun updateSavingsTransaction(
        @Path("id") id: String,
        @Body request: UpdateSavingsTransactionRequest,
    ): SavingsTransactionResponse

    @DELETE("savings-transactions/{id}")
    suspend fun deleteSavingsTransaction(@Path("id") id: String): Response<Unit>
}
