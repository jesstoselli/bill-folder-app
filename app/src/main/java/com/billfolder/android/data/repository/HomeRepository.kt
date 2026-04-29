package com.billfolder.android.data.repository

import com.billfolder.android.data.api.BillFolderApi
import com.billfolder.android.data.dto.HomeResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRepository @Inject constructor(
    private val api: BillFolderApi,
) {
    /**
     * Devolve o snapshot do dashboard. Erros (rede, HTTP) propagam como
     * exceção — o ViewModel converte pra estado de UI.
     */
    suspend fun getHome(): HomeResponse = api.getHome()
}
