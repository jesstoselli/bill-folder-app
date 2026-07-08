package com.billfolder.android.ui.screens.cards

import com.billfolder.android.data.dto.CardEntryResponse
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testa a lógica pura de "essa compra é uma assinatura?". Uma compra gerada
 * por template de recorrência (templateId != null) precisa perguntar o escopo
 * ao deletar/reprecificar; uma compra avulsa (templateId == null) segue o
 * fluxo normal. Espelha ProvisionedExpenseTest.
 */
class CardSubscriptionTest {

    private fun entry(templateId: String? = null) = CardEntryResponse(
        id = "e1", cardId = "c1", cardName = "Cartão", purchaseDate = "2026-06-10",
        label = "Netflix", totalAmount = 39.9, installmentsCount = 1,
        categoryId = "cat", categoryName = "Cat", templateId = templateId,
        createdAt = "2026-06-10T00:00:00Z", updatedAt = "2026-06-10T00:00:00Z",
    )

    @Test
    fun `compra avulsa nao e assinatura`() {
        assertFalse(entry(templateId = null).isSubscription())
    }

    @Test
    fun `compra com templateId e assinatura`() {
        assertTrue(entry(templateId = "tmpl-1").isSubscription())
    }
}
