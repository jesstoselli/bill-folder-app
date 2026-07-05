package com.billfolder.android.ui.screens.cards

import com.billfolder.android.data.dto.CardEntryResponse
import com.billfolder.android.data.dto.CreditCardAccountResponse
import com.billfolder.android.data.dto.EntryInstallmentDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Bounds da navegação de faturas (CardsScreen). Com closingDay=20 e dueDay=27,
 * uma compra no dia 15 do mês M gera fatura vencendo em M-27 — o que dá
 * controle determinístico de qual fatura está sendo vista via
 * referencePurchaseDate.
 */
class CardsNavigationBoundsTest {

    private fun card(id: String, closingDay: Int = 20, dueDay: Int = 27) =
        CreditCardAccountResponse(
            id = id,
            name = "Cartão $id",
            closingDay = closingDay,
            dueDay = dueDay,
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z",
        )

    /** Cria uma entry no cartão com uma installment por dueDate informado. */
    private fun entry(cardId: String, vararg statementDueIsos: String): CardEntryResponse =
        CardEntryResponse(
            id = "entry-$cardId-${statementDueIsos.joinToString("_")}",
            cardId = cardId,
            cardName = "Cartão $cardId",
            purchaseDate = "2026-01-15",
            label = "compra",
            totalAmount = 100.0,
            installmentsCount = statementDueIsos.size,
            categoryId = "cat-1",
            categoryName = "Categoria",
            createdAt = "2026-01-15T00:00:00Z",
            updatedAt = "2026-01-15T00:00:00Z",
            installments = statementDueIsos.mapIndexed { i, due ->
                EntryInstallmentDto(
                    installmentId = "inst-$i-$due",
                    installmentNumber = i + 1,
                    amount = 10.0,
                    statementId = "stmt-$due",
                    statementDueDate = due,
                )
            },
        )

    private fun content(
        referencePurchaseIso: String,
        selectedCardId: String = "card-1",
        cards: List<CreditCardAccountResponse> = listOf(card("card-1")),
        entries: List<CardEntryResponse>,
    ) = CardsUiState.Content(
        cards = cards,
        allEntries = entries,
        selectedCardId = selectedCardId,
        referencePurchaseDate = LocalDate.parse(referencePurchaseIso),
    )

    @Test
    fun `range covers min and max statement due of the selected card`() {
        val state = content(
            referencePurchaseIso = "2026-06-15",
            entries = listOf(entry("card-1", "2026-05-27", "2026-07-27")),
        )

        val range = state.selectedCardStatementDueRange()

        assertEquals(LocalDate.parse("2026-05-27"), range?.start)
        assertEquals(LocalDate.parse("2026-07-27"), range?.endInclusive)
    }

    @Test
    fun `range ignores installments from other cards`() {
        val state = content(
            referencePurchaseIso = "2026-06-15",
            cards = listOf(card("card-1"), card("card-2")),
            entries = listOf(
                entry("card-1", "2026-06-27"),
                entry("card-2", "2026-01-27", "2026-12-27"),
            ),
        )

        val range = state.selectedCardStatementDueRange()

        assertEquals(LocalDate.parse("2026-06-27"), range?.start)
        assertEquals(LocalDate.parse("2026-06-27"), range?.endInclusive)
    }

    @Test
    fun `range is null when the selected card has no installments`() {
        val state = content(
            referencePurchaseIso = "2026-06-15",
            entries = listOf(entry("card-2", "2026-06-27")),
        )

        assertNull(state.selectedCardStatementDueRange())
    }

    @Test
    fun `in the middle of the range both directions are allowed`() {
        // dueDate atual = junho/27; range = maio/27..julho/27
        val state = content(
            referencePurchaseIso = "2026-06-15",
            entries = listOf(entry("card-1", "2026-05-27", "2026-07-27")),
        )

        assertTrue(state.canGoToPreviousStatement())
        assertTrue(state.canGoToNextStatement())
    }

    @Test
    fun `at the last statement next is blocked but previous is allowed`() {
        // dueDate atual = julho/27 == max
        val state = content(
            referencePurchaseIso = "2026-07-15",
            entries = listOf(entry("card-1", "2026-05-27", "2026-07-27")),
        )

        assertFalse(state.canGoToNextStatement())
        assertTrue(state.canGoToPreviousStatement())
    }

    @Test
    fun `at the first statement previous is blocked but next is allowed`() {
        // dueDate atual = maio/27 == min
        val state = content(
            referencePurchaseIso = "2026-05-15",
            entries = listOf(entry("card-1", "2026-05-27", "2026-07-27")),
        )

        assertFalse(state.canGoToPreviousStatement())
        assertTrue(state.canGoToNextStatement())
    }

    @Test
    fun `beyond the last statement only previous is allowed`() {
        // Caso do bug: âncora "hoje" caiu depois da última fatura com parcela.
        // dueDate atual = setembro/27 > max julho/27.
        val state = content(
            referencePurchaseIso = "2026-09-15",
            entries = listOf(entry("card-1", "2026-05-27", "2026-07-27")),
        )

        assertFalse(state.canGoToNextStatement())
        assertTrue(state.canGoToPreviousStatement())
    }

    @Test
    fun `no installments blocks both directions`() {
        val state = content(
            referencePurchaseIso = "2026-06-15",
            entries = emptyList(),
        )

        assertFalse(state.canGoToPreviousStatement())
        assertFalse(state.canGoToNextStatement())
    }
}
