package com.billfolder.android.ui.screens.home

import com.billfolder.android.data.dto.DailyExpenseResponse

/**
 * Avulsas ordenadas da mais recente pra mais antiga. `date` é ISO
 * "yyyy-MM-dd", então ordem lexicográfica descendente == cronológica
 * descendente. Usado na aba "Últimas" da Home.
 */
fun List<DailyExpenseResponse>.recentFirst(): List<DailyExpenseResponse> =
    sortedByDescending { it.date }
