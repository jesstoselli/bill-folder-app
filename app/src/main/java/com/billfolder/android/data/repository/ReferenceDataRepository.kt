package com.billfolder.android.data.repository

import com.billfolder.android.data.api.BillFolderApi
import com.billfolder.android.data.dto.CategoryDto
import com.billfolder.android.data.dto.CheckingAccountResponse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositório de "reference data" — categorias e contas correntes que
 * alimentam dropdowns de formulários. Junta os dois pra evitar inflar
 * o número de repositories agora; quando crescer, se separa.
 *
 * Sem cache local por enquanto — a lista é pequena e o backend é rápido.
 * Quando virar gargalo (ou quando offline-first entrar em jogo), passa
 * pra cache via Room.
 */
@Singleton
class ReferenceDataRepository @Inject constructor(
    private val api: BillFolderApi,
) {
    suspend fun getCategories(): List<CategoryDto> =
        api.getCategories().sortedBy { it.displayOrder }

    suspend fun getCheckingAccounts(): List<CheckingAccountResponse> =
        api.getCheckingAccounts()
            // Conta primária primeiro — é o default mais provável no formulário
            .sortedByDescending { it.isPrimary }
}
