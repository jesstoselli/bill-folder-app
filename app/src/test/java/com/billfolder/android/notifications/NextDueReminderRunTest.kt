package com.billfolder.android.notifications

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class NextDueReminderRunTest {

    private val today = LocalDate.of(2026, 10, 5)

    @Test
    fun `antes das 9h roda as 9h de hoje`() {
        val now = today.atTime(7, 30)

        assertEquals(today.atTime(9, 0), nextDueReminderRun(now, lastChecked = today.minusDays(1)))
    }

    @Test
    fun `depois das 9h sem ter conferido hoje roda agora`() {
        val now = today.atTime(14, 0)

        assertEquals(now, nextDueReminderRun(now, lastChecked = null))
    }

    @Test
    fun `depois de conferir hoje roda as 9h de amanha`() {
        val now: LocalDateTime = today.atTime(9, 2)

        assertEquals(today.plusDays(1).atTime(9, 0), nextDueReminderRun(now, lastChecked = today))
    }
}
