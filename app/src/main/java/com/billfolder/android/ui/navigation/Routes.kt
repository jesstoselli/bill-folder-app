package com.billfolder.android.ui.navigation

/**
 * Rotas do NavHost. Strings simples — quando crescer, migrar pra
 * type-safe Navigation 2.8 (Serializable destinations).
 */
object Routes {
    const val LOGIN           = "login"
    const val SIGNUP          = "signup"
    const val HOME            = "home"
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

    const val MANAGE_CARDS    = "manage-cards"
    const val MANAGE_SAVINGS  = "manage-savings"
}
