package com.billfolder.android.ui.util

import com.billfolder.android.data.dto.CycleResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CycleNavigationTest {

    private fun cycle(id: String, startDate: String) = CycleResponse(
        id = id,
        startDate = startDate,
        endDate = startDate,
        label = id,
        isRecurrenceGenerated = false,
        isCurrent = false,
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z",
    )

    // Fora de ordem de propósito: a função deve ordenar por startDate.
    private val cycles = listOf(
        cycle("mar", "2026-03-01"),
        cycle("jan", "2026-01-01"),
        cycle("fev", "2026-02-01"),
    )

    @Test
    fun `no meio devolve os vizinhos corretos`() {
        assertEquals("jan", resolveAdjacentCycle(cycles, "fev", CycleDirection.PREVIOUS)?.id)
        assertEquals("mar", resolveAdjacentCycle(cycles, "fev", CycleDirection.NEXT)?.id)
    }

    @Test
    fun `primeiro ciclo nao tem anterior`() {
        assertNull(resolveAdjacentCycle(cycles, "jan", CycleDirection.PREVIOUS))
    }

    @Test
    fun `ultimo ciclo nao tem proximo`() {
        assertNull(resolveAdjacentCycle(cycles, "mar", CycleDirection.NEXT))
    }

    @Test
    fun `id ausente na lista devolve null`() {
        assertNull(resolveAdjacentCycle(cycles, "inexistente", CycleDirection.NEXT))
    }

    @Test
    fun `lista vazia devolve null`() {
        assertNull(resolveAdjacentCycle(emptyList(), "jan", CycleDirection.PREVIOUS))
    }
}
