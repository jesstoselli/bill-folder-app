package com.billfolder.android.ui.screens.cards

import com.billfolder.android.data.dto.CardEntryResponse

/**
 * Helper puro de "assinatura de cartão" (Netflix, Spotify e afins).
 *
 * Uma compra é uma assinatura quando foi gerada por um template de
 * recorrência — ou seja, quando `templateId != null`. Nesse caso, deletar
 * ou reprecificar precisa perguntar o escopo ("só esta" vs "esta e as
 * próximas") via RecurrenceScopeDialog, análogo à despesa provisionada
 * (ver ExpenseResponse.isProvisioned()). Uma compra avulsa segue o fluxo
 * normal (delete direto, sem reprice).
 *
 * Extraído como função pura pra a lógica de branching ser testável sem
 * tocar em Composable — espelha ProvisionedExpense.kt.
 */
fun CardEntryResponse.isSubscription(): Boolean = templateId != null
