package com.billfolder.android.ui.navigation

import com.billfolder.android.ui.components.DrawerDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * `toDrawerDestination` mapeia a rota atual → item do drawer. Isso alimenta
 * `gesturesEnabled` do ModalNavigationDrawer — e no M3 o fechar-pelo-scrim é
 * gated por `gesturesEnabled`. As telas Cards/Savings usam rotas com query
 * ("cards?cardId={cardId}"), então precisam casar pela base da rota; senão o
 * menu abre mas não fecha nessas telas.
 */
class DrawerDestinationTest {

    @Test
    fun `rota simples casa`() {
        assertEquals(DrawerDestination.Home, "home".toDrawerDestination())
        assertEquals(DrawerDestination.Expenses, "expenses".toDrawerDestination())
        assertEquals(DrawerDestination.ManageBanks, "manage-banks".toDrawerDestination())
    }

    @Test
    fun `rota parametrizada de cards casa pela base`() {
        assertEquals(DrawerDestination.Cards, "cards?cardId={cardId}".toDrawerDestination())
    }

    @Test
    fun `rota parametrizada de savings casa pela base`() {
        assertEquals(DrawerDestination.Savings, "savings?savingsAccountId={savingsAccountId}".toDrawerDestination())
    }

    @Test
    fun `rotas de auth e nula nao casam`() {
        assertNull("login".toDrawerDestination())
        assertNull("reset-password?email={email}".toDrawerDestination())
        val nullRoute: String? = null
        assertNull(nullRoute.toDrawerDestination())
    }
}
