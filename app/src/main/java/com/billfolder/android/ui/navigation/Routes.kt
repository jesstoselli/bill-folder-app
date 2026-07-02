package com.billfolder.android.ui.navigation

/**
 * Rotas do NavHost. Strings simples — quando crescer, migrar pra
 * type-safe Navigation 2.8 (Serializable destinations).
 */
object Routes {
    const val LOGIN           = "login"
    const val SIGNUP          = "signup"
    const val HOME            = "home"

    /**
     * Reset de senha — 2 telas em cadeia:
     *   forgot-password → user digita email, dispara envio do código
     *   reset-password?email=... → user digita código + nova senha
     * O email é passado como arg pro passo 2 não pedir de novo.
     */
    const val FORGOT_PASSWORD          = "forgot-password"
    const val RESET_PASSWORD_PATTERN   = "reset-password?email={email}"
    const val RESET_PASSWORD_ARG_EMAIL = "email"
    fun resetPasswordFor(email: String) = "reset-password?email=$email"

    const val DAILY_EXPENSES  = "daily-expenses"
    const val EXPENSES        = "expenses"
    const val INCOME          = "income"

    /**
     * Cards (consumo) aceita um cardId opcional como query arg pra
     * abrir já com aquele cartão selecionado no carousel. Sem o arg
     * ("cards"), cai no comportamento default (primeiro cartão).
     */
    const val CARDS_PATTERN   = "cards?cardId={cardId}"
    const val CARDS           = "cards"
    const val CARDS_ARG_ID    = "cardId"
    fun cardsWithSelected(cardId: String) = "cards?cardId=$cardId"

    /**
     * Savings (consumo) — mesmo molde do Cards. savingsAccountId opcional
     * como query arg pra abrir já com aquela poupança selecionada no
     * carousel; sem o arg ("savings"), cai no comportamento default
     * (primeira poupança).
     */
    const val SAVINGS_PATTERN = "savings?savingsAccountId={savingsAccountId}"
    const val SAVINGS         = "savings"
    const val SAVINGS_ARG_ID  = "savingsAccountId"
    fun savingsWithSelected(savingsAccountId: String) =
        "savings?savingsAccountId=$savingsAccountId"

    const val MANAGE_CARDS    = "manage-cards"
    const val MANAGE_SAVINGS  = "manage-savings"
    const val MANAGE_BANKS = "manage-banks"
}
