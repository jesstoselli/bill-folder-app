package com.billfolder.android.ui.screens.home

import com.billfolder.android.data.dto.DailyExpenseResponse
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeRecentDailyTest {

    private fun daily(id: String, date: String) = DailyExpenseResponse(
        id = id, date = date, label = "x", amount = 10.0,
        categoryId = "cat", categoryName = "Cat", accountId = "acc", accountName = "Conta",
        createdAt = "2026-06-01T00:00:00Z", updatedAt = "2026-06-01T00:00:00Z",
    )

    @Test
    fun `recentFirst ordena por data descendente`() {
        val list = listOf(
            daily("a", "2026-06-05"),
            daily("b", "2026-06-20"),
            daily("c", "2026-06-10"),
        )
        val ids = list.recentFirst().map { it.id }
        assertEquals(listOf("b", "c", "a"), ids)
    }

    @Test
    fun `recentFirst em lista vazia retorna vazia`() {
        assertEquals(emptyList<DailyExpenseResponse>(), emptyList<DailyExpenseResponse>().recentFirst())
    }
}
