package com.billfolder.android.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Testa os mappers dos literais de escopo. Casing DIFERE por operação:
 *  - delete (query param): snake_case → "this" / "this_and_following"
 *  - reprice (body enum camelCase): "this" / "thisAndFollowing"
 * O bug clássico aqui é mandar o literal errado no endpoint errado, então
 * cobrimos os dois lados explicitamente.
 */
class RecurrenceScopeTest {

    @Test
    fun `deleteLiteral e snake_case`() {
        assertEquals("this", ScopeChoice.This.deleteLiteral())
        assertEquals("this_and_following", ScopeChoice.ThisAndFollowing.deleteLiteral())
    }

    @Test
    fun `repriceLiteral e camelCase`() {
        assertEquals("this", ScopeChoice.This.repriceLiteral())
        assertEquals("thisAndFollowing", ScopeChoice.ThisAndFollowing.repriceLiteral())
    }
}
