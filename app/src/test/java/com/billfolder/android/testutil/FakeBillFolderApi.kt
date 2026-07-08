package com.billfolder.android.testutil

import com.billfolder.android.data.api.BillFolderApi
import com.billfolder.android.data.dto.AuthResponse
import com.billfolder.android.data.dto.CardEntryRecurrenceResponse
import com.billfolder.android.data.dto.CardEntryResponse
import com.billfolder.android.data.dto.CategoryDto
import com.billfolder.android.data.dto.CheckingAccountResponse
import com.billfolder.android.data.dto.CreateCardEntryRecurrenceRequest
import com.billfolder.android.data.dto.CreateCardEntryRequest
import com.billfolder.android.data.dto.CreateCheckingAccountRequest
import com.billfolder.android.data.dto.CreateCreditCardAccountRequest
import com.billfolder.android.data.dto.CreateCycleAdjustmentRequest
import com.billfolder.android.data.dto.CreateCycleRequest
import com.billfolder.android.data.dto.CreateDailyExpenseRequest
import com.billfolder.android.data.dto.CreateExpenseRequest
import com.billfolder.android.data.dto.CreateExpenseRecurrenceRequest
import com.billfolder.android.data.dto.CreateIncomeEntryRequest
import com.billfolder.android.data.dto.CreateIncomeSourceRequest
import com.billfolder.android.data.dto.CreateSavingsAccountRequest
import com.billfolder.android.data.dto.CreateSavingsTransactionRequest
import com.billfolder.android.data.dto.CreditCardAccountResponse
import com.billfolder.android.data.dto.CycleAdjustmentResponse
import com.billfolder.android.data.dto.CycleResponse
import com.billfolder.android.data.dto.DailyExpenseResponse
import com.billfolder.android.data.dto.ExpenseRecurrenceResponse
import com.billfolder.android.data.dto.ExpenseResponse
import com.billfolder.android.data.dto.PayOccurrenceRequest
import com.billfolder.android.data.dto.ForgotPasswordRequest
import com.billfolder.android.data.dto.ForgotPasswordResponse
import com.billfolder.android.data.dto.HomeResponse
import com.billfolder.android.data.dto.IncomeEntryResponse
import com.billfolder.android.data.dto.IncomeSourceResponse
import com.billfolder.android.data.dto.LoginRequest
import com.billfolder.android.data.dto.LogoutRequest
import com.billfolder.android.data.dto.RefreshTokenRequest
import com.billfolder.android.data.dto.RepriceProvisionedExpenseRequest
import com.billfolder.android.data.dto.ResetPasswordRequest
import com.billfolder.android.data.dto.SavingsAccountResponse
import com.billfolder.android.data.dto.SavingsTransactionResponse
import com.billfolder.android.data.dto.SignupRequest
import com.billfolder.android.data.dto.UpdateCardEntryRequest
import com.billfolder.android.data.dto.UpdateCardSubscriptionAmountRequest
import com.billfolder.android.data.dto.UpdateCheckingAccountRequest
import com.billfolder.android.data.dto.UpdateCreditCardAccountRequest
import com.billfolder.android.data.dto.UpdateCycleAdjustmentRequest
import com.billfolder.android.data.dto.UpdateDailyExpenseRequest
import com.billfolder.android.data.dto.UpdateExpenseRequest
import com.billfolder.android.data.dto.UpdateIncomeEntryRequest
import com.billfolder.android.data.dto.UpdateIncomeSourceRequest
import com.billfolder.android.data.dto.UpdateSavingsAccountRequest
import com.billfolder.android.data.dto.UpdateSavingsTransactionRequest
import retrofit2.Response

/**
 * Fake do BillFolderApi pra unit tests de repositories e ViewModels — sem
 * Retrofit/rede. Uso: setar os `var`s de leitura com o que a API deve
 * devolver, setar as lambdas `on*` pros writes que o teste exercita (as não
 * setadas lançam, o que torna óbvio um caminho não configurado), e inspecionar
 * as listas `*Calls` pra assertar que um write aconteceu.
 *
 * GETs que filtram por from/to/status/etc IGNORAM os filtros de propósito —
 * o teste configura a lista exatamente como quer que ela volte. A lógica de
 * janela por ciclo é do backend; aqui testamos o comportamento client-side.
 */
class FakeBillFolderApi : BillFolderApi {

    // ---- Reads configuráveis (GET) ----
    var onGetHome: (String?) -> HomeResponse = { notConfigured("getHome") }
    var categories: List<CategoryDto> = emptyList()
    var checkingAccounts: List<CheckingAccountResponse> = emptyList()
    var onGetCurrentCycle: () -> CycleResponse = { notConfigured("getCurrentCycle") }
    var cycles: List<CycleResponse> = emptyList()
    var cycleAdjustments: List<CycleAdjustmentResponse> = emptyList()
    var dailyExpenses: List<DailyExpenseResponse> = emptyList()
    var expenses: List<ExpenseResponse> = emptyList()
    var incomeSources: List<IncomeSourceResponse> = emptyList()
    var incomeEntries: List<IncomeEntryResponse> = emptyList()
    var creditCards: List<CreditCardAccountResponse> = emptyList()
    var cardEntries: List<CardEntryResponse> = emptyList()
    var savingsAccounts: List<SavingsAccountResponse> = emptyList()
    var savingsTransactions: List<SavingsTransactionResponse> = emptyList()

    // ---- Writes: lambdas de resposta (default lança) + registro de chamadas ----
    var onCreateCheckingAccount: (CreateCheckingAccountRequest) -> CheckingAccountResponse = { notConfigured("createCheckingAccount") }
    var onUpdateCheckingAccount: (String, UpdateCheckingAccountRequest) -> CheckingAccountResponse = { _, _ -> notConfigured("updateCheckingAccount") }
    var onCreateCycle: (CreateCycleRequest) -> CycleResponse = { notConfigured("createCycle") }
    var onCreateCycleAdjustment: (CreateCycleAdjustmentRequest) -> CycleAdjustmentResponse = { notConfigured("createCycleAdjustment") }
    var onUpdateCycleAdjustment: (String, UpdateCycleAdjustmentRequest) -> CycleAdjustmentResponse = { _, _ -> notConfigured("updateCycleAdjustment") }
    var onCreateDailyExpense: (CreateDailyExpenseRequest) -> DailyExpenseResponse = { notConfigured("createDailyExpense") }
    var onUpdateDailyExpense: (String, UpdateDailyExpenseRequest) -> DailyExpenseResponse = { _, _ -> notConfigured("updateDailyExpense") }
    var onCreateExpense: (CreateExpenseRequest) -> ExpenseResponse = { notConfigured("createExpense") }
    var onUpdateExpense: (String, UpdateExpenseRequest) -> ExpenseResponse = { _, _ -> notConfigured("updateExpense") }
    var onPayOccurrence: (String, PayOccurrenceRequest) -> ExpenseResponse = { _, _ -> notConfigured("payOccurrence") }
    var onRepriceProvisionedExpense: (String, RepriceProvisionedExpenseRequest) -> ExpenseResponse = { _, _ -> notConfigured("repriceProvisionedExpense") }
    var onCreateExpenseRecurrence: (CreateExpenseRecurrenceRequest) -> ExpenseRecurrenceResponse = { notConfigured("createExpenseRecurrence") }
    var onCreateIncomeSource: (CreateIncomeSourceRequest) -> IncomeSourceResponse = { notConfigured("createIncomeSource") }
    var onUpdateIncomeSource: (String, UpdateIncomeSourceRequest) -> IncomeSourceResponse = { _, _ -> notConfigured("updateIncomeSource") }
    var onCreateIncomeEntry: (CreateIncomeEntryRequest) -> IncomeEntryResponse = { notConfigured("createIncomeEntry") }
    var onUpdateIncomeEntry: (String, UpdateIncomeEntryRequest) -> IncomeEntryResponse = { _, _ -> notConfigured("updateIncomeEntry") }
    var onCreateCreditCard: (CreateCreditCardAccountRequest) -> CreditCardAccountResponse = { notConfigured("createCreditCard") }
    var onUpdateCreditCard: (String, UpdateCreditCardAccountRequest) -> CreditCardAccountResponse = { _, _ -> notConfigured("updateCreditCard") }
    var onCreateCardEntry: (CreateCardEntryRequest) -> CardEntryResponse = { notConfigured("createCardEntry") }
    var onUpdateCardEntry: (String, UpdateCardEntryRequest) -> CardEntryResponse = { _, _ -> notConfigured("updateCardEntry") }
    var onCreateCardEntryRecurrence: (CreateCardEntryRecurrenceRequest) -> CardEntryRecurrenceResponse = { notConfigured("createCardEntryRecurrence") }
    var onUpdateCardSubscriptionAmount: (String, UpdateCardSubscriptionAmountRequest) -> CardEntryResponse = { _, _ -> notConfigured("updateCardSubscriptionAmount") }
    var onCreateSavingsAccount: (CreateSavingsAccountRequest) -> SavingsAccountResponse = { notConfigured("createSavingsAccount") }
    var onUpdateSavingsAccount: (String, UpdateSavingsAccountRequest) -> SavingsAccountResponse = { _, _ -> notConfigured("updateSavingsAccount") }
    var onCreateSavingsTransaction: (CreateSavingsTransactionRequest) -> SavingsTransactionResponse = { notConfigured("createSavingsTransaction") }
    var onUpdateSavingsTransaction: (String, UpdateSavingsTransactionRequest) -> SavingsTransactionResponse = { _, _ -> notConfigured("updateSavingsTransaction") }

    // ---- Registro de deletes (ids) e resultado configurável ----
    var deleteResult: Response<Unit> = Response.success(Unit)
    val deletedCheckingAccountIds = mutableListOf<String>()
    val deletedCycleAdjustmentIds = mutableListOf<String>()
    val deletedDailyExpenseIds = mutableListOf<String>()
    val deletedExpenseIds = mutableListOf<String>()
    val deleteExpenseScopes = mutableListOf<String?>()
    val deleteCardEntryScopes = mutableListOf<String?>()
    val deletedIncomeSourceIds = mutableListOf<String>()
    val deletedIncomeEntryIds = mutableListOf<String>()
    val deletedCreditCardIds = mutableListOf<String>()
    val deletedCardEntryIds = mutableListOf<String>()
    val deletedSavingsAccountIds = mutableListOf<String>()
    val deletedSavingsTransactionIds = mutableListOf<String>()

    // ---- Registro de chamadas de write (pra asserção) ----
    val createExpenseCalls = mutableListOf<CreateExpenseRequest>()
    val payOccurrenceCalls = mutableListOf<Pair<String, PayOccurrenceRequest>>()
    val repriceProvisionedExpenseCalls = mutableListOf<Pair<String, RepriceProvisionedExpenseRequest>>()
    val createExpenseRecurrenceCalls = mutableListOf<CreateExpenseRecurrenceRequest>()
    val createCardEntryRecurrenceCalls = mutableListOf<CreateCardEntryRecurrenceRequest>()
    val updateCardSubscriptionAmountCalls = mutableListOf<Pair<String, UpdateCardSubscriptionAmountRequest>>()
    val createDailyExpenseCalls = mutableListOf<CreateDailyExpenseRequest>()
    val createIncomeEntryCalls = mutableListOf<CreateIncomeEntryRequest>()
    val createCardEntryCalls = mutableListOf<CreateCardEntryRequest>()
    val createSavingsTransactionCalls = mutableListOf<CreateSavingsTransactionRequest>()
    val createCycleAdjustmentCalls = mutableListOf<CreateCycleAdjustmentRequest>()

    private fun notConfigured(name: String): Nothing =
        throw NotImplementedError("FakeBillFolderApi.$name não configurado neste teste")

    // ========================================================================
    // Auth
    // ========================================================================
    override suspend fun signup(request: SignupRequest): Response<AuthResponse> = notConfigured("signup")
    override suspend fun login(request: LoginRequest): Response<AuthResponse> = notConfigured("login")
    override suspend fun refresh(request: RefreshTokenRequest): Response<AuthResponse> = notConfigured("refresh")
    override suspend fun logout(request: LogoutRequest): Response<Unit> = Response.success(Unit)
    override suspend fun forgotPassword(request: ForgotPasswordRequest): Response<ForgotPasswordResponse> = notConfigured("forgotPassword")
    override suspend fun resetPassword(request: ResetPasswordRequest): Response<Unit> = notConfigured("resetPassword")

    // ========================================================================
    // Home
    // ========================================================================
    override suspend fun getHome(cycleId: String?): HomeResponse = onGetHome(cycleId)

    // ========================================================================
    // Reference data / Checking accounts
    // ========================================================================
    override suspend fun getCategories(): List<CategoryDto> = categories
    override suspend fun getCheckingAccounts(): List<CheckingAccountResponse> = checkingAccounts
    override suspend fun createCheckingAccount(request: CreateCheckingAccountRequest): CheckingAccountResponse =
        onCreateCheckingAccount(request)
    override suspend fun updateCheckingAccount(id: String, request: UpdateCheckingAccountRequest): CheckingAccountResponse =
        onUpdateCheckingAccount(id, request)
    override suspend fun deleteCheckingAccount(id: String): Response<Unit> {
        deletedCheckingAccountIds += id
        if (deleteResult.isSuccessful) checkingAccounts = checkingAccounts.filterNot { it.id == id }
        return deleteResult
    }

    // ========================================================================
    // Cycles
    // ========================================================================
    override suspend fun getCurrentCycle(): CycleResponse = onGetCurrentCycle()
    override suspend fun listCycles(from: String?, to: String?): List<CycleResponse> = cycles
    override suspend fun createCycle(request: CreateCycleRequest): CycleResponse {
        return onCreateCycle(request)
    }

    // ========================================================================
    // Cycle adjustments
    // ========================================================================
    override suspend fun getCycleAdjustments(from: String?, to: String?, type: String?): List<CycleAdjustmentResponse> =
        cycleAdjustments
    override suspend fun createCycleAdjustment(request: CreateCycleAdjustmentRequest): CycleAdjustmentResponse {
        createCycleAdjustmentCalls += request
        return onCreateCycleAdjustment(request)
    }
    override suspend fun updateCycleAdjustment(id: String, request: UpdateCycleAdjustmentRequest): CycleAdjustmentResponse =
        onUpdateCycleAdjustment(id, request)
    override suspend fun deleteCycleAdjustment(id: String): Response<Unit> {
        deletedCycleAdjustmentIds += id
        if (deleteResult.isSuccessful) cycleAdjustments = cycleAdjustments.filterNot { it.id == id }
        return deleteResult
    }

    // ========================================================================
    // Daily expenses
    // ========================================================================
    override suspend fun getDailyExpenses(from: String?, to: String?, categoryId: String?): List<DailyExpenseResponse> =
        dailyExpenses
    override suspend fun createDailyExpense(request: CreateDailyExpenseRequest): DailyExpenseResponse {
        createDailyExpenseCalls += request
        return onCreateDailyExpense(request)
    }
    override suspend fun updateDailyExpense(id: String, request: UpdateDailyExpenseRequest): DailyExpenseResponse =
        onUpdateDailyExpense(id, request)
    override suspend fun deleteDailyExpense(id: String): Response<Unit> {
        deletedDailyExpenseIds += id
        if (deleteResult.isSuccessful) dailyExpenses = dailyExpenses.filterNot { it.id == id }
        return deleteResult
    }

    // ========================================================================
    // Expenses
    // ========================================================================
    override suspend fun getExpenses(from: String?, to: String?, status: String?, categoryId: String?): List<ExpenseResponse> =
        expenses
    override suspend fun createExpense(request: CreateExpenseRequest): ExpenseResponse {
        createExpenseCalls += request
        return onCreateExpense(request)
    }
    override suspend fun updateExpense(id: String, request: UpdateExpenseRequest): ExpenseResponse =
        onUpdateExpense(id, request)
    override suspend fun payOccurrence(id: String, request: PayOccurrenceRequest): ExpenseResponse {
        payOccurrenceCalls += id to request
        return onPayOccurrence(id, request)
    }
    override suspend fun repriceProvisionedExpense(id: String, request: RepriceProvisionedExpenseRequest): ExpenseResponse {
        repriceProvisionedExpenseCalls += id to request
        return onRepriceProvisionedExpense(id, request)
    }
    override suspend fun deleteExpense(id: String, scope: String?): Response<Unit> {
        deletedExpenseIds += id
        deleteExpenseScopes += scope
        if (deleteResult.isSuccessful) expenses = expenses.filterNot { it.id == id }
        return deleteResult
    }
    override suspend fun createExpenseRecurrence(request: CreateExpenseRecurrenceRequest): ExpenseRecurrenceResponse {
        createExpenseRecurrenceCalls += request
        return onCreateExpenseRecurrence(request)
    }
    override suspend fun createCardEntryRecurrence(request: CreateCardEntryRecurrenceRequest): CardEntryRecurrenceResponse {
        createCardEntryRecurrenceCalls += request
        return onCreateCardEntryRecurrence(request)
    }

    // ========================================================================
    // Income
    // ========================================================================
    override suspend fun getIncomeSources(): List<IncomeSourceResponse> = incomeSources
    override suspend fun createIncomeSource(request: CreateIncomeSourceRequest): IncomeSourceResponse =
        onCreateIncomeSource(request)
    override suspend fun updateIncomeSource(id: String, request: UpdateIncomeSourceRequest): IncomeSourceResponse =
        onUpdateIncomeSource(id, request)
    override suspend fun deleteIncomeSource(id: String): Response<Unit> {
        deletedIncomeSourceIds += id
        if (deleteResult.isSuccessful) incomeSources = incomeSources.filterNot { it.id == id }
        return deleteResult
    }
    override suspend fun getIncomeEntries(from: String?, to: String?, sourceId: String?): List<IncomeEntryResponse> =
        incomeEntries
    override suspend fun createIncomeEntry(request: CreateIncomeEntryRequest): IncomeEntryResponse {
        createIncomeEntryCalls += request
        return onCreateIncomeEntry(request)
    }
    override suspend fun updateIncomeEntry(id: String, request: UpdateIncomeEntryRequest): IncomeEntryResponse =
        onUpdateIncomeEntry(id, request)
    override suspend fun deleteIncomeEntry(id: String): Response<Unit> {
        deletedIncomeEntryIds += id
        if (deleteResult.isSuccessful) incomeEntries = incomeEntries.filterNot { it.id == id }
        return deleteResult
    }

    // ========================================================================
    // Credit cards
    // ========================================================================
    override suspend fun getCreditCards(): List<CreditCardAccountResponse> = creditCards
    override suspend fun createCreditCard(request: CreateCreditCardAccountRequest): CreditCardAccountResponse =
        onCreateCreditCard(request)
    override suspend fun updateCreditCard(id: String, request: UpdateCreditCardAccountRequest): CreditCardAccountResponse =
        onUpdateCreditCard(id, request)
    override suspend fun deleteCreditCard(id: String): Response<Unit> {
        deletedCreditCardIds += id
        if (deleteResult.isSuccessful) creditCards = creditCards.filterNot { it.id == id }
        return deleteResult
    }

    // ========================================================================
    // Card entries
    // ========================================================================
    override suspend fun getCardEntries(cardId: String?): List<CardEntryResponse> = cardEntries
    override suspend fun createCardEntry(request: CreateCardEntryRequest): CardEntryResponse {
        createCardEntryCalls += request
        return onCreateCardEntry(request)
    }
    override suspend fun updateCardEntry(id: String, request: UpdateCardEntryRequest): CardEntryResponse =
        onUpdateCardEntry(id, request)
    override suspend fun updateCardSubscriptionAmount(id: String, request: UpdateCardSubscriptionAmountRequest): CardEntryResponse {
        updateCardSubscriptionAmountCalls += id to request
        return onUpdateCardSubscriptionAmount(id, request)
    }
    override suspend fun deleteCardEntry(id: String, scope: String?): Response<Unit> {
        deletedCardEntryIds += id
        deleteCardEntryScopes += scope
        if (deleteResult.isSuccessful) cardEntries = cardEntries.filterNot { it.id == id }
        return deleteResult
    }

    // ========================================================================
    // Savings accounts
    // ========================================================================
    override suspend fun getSavingsAccounts(): List<SavingsAccountResponse> = savingsAccounts
    override suspend fun createSavingsAccount(request: CreateSavingsAccountRequest): SavingsAccountResponse =
        onCreateSavingsAccount(request)
    override suspend fun updateSavingsAccount(id: String, request: UpdateSavingsAccountRequest): SavingsAccountResponse =
        onUpdateSavingsAccount(id, request)
    override suspend fun deleteSavingsAccount(id: String): Response<Unit> {
        deletedSavingsAccountIds += id
        if (deleteResult.isSuccessful) savingsAccounts = savingsAccounts.filterNot { it.id == id }
        return deleteResult
    }

    // ========================================================================
    // Savings transactions
    // ========================================================================
    override suspend fun getSavingsTransactions(savingsAccountId: String?, from: String?, to: String?, type: String?): List<SavingsTransactionResponse> =
        savingsTransactions
    override suspend fun createSavingsTransaction(request: CreateSavingsTransactionRequest): SavingsTransactionResponse {
        createSavingsTransactionCalls += request
        return onCreateSavingsTransaction(request)
    }
    override suspend fun updateSavingsTransaction(id: String, request: UpdateSavingsTransactionRequest): SavingsTransactionResponse =
        onUpdateSavingsTransaction(id, request)
    override suspend fun deleteSavingsTransaction(id: String): Response<Unit> {
        deletedSavingsTransactionIds += id
        if (deleteResult.isSuccessful) savingsTransactions = savingsTransactions.filterNot { it.id == id }
        return deleteResult
    }
}
