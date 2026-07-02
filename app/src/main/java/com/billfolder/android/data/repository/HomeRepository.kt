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
     *
     * cycleId null = ciclo atual (default). Passar cycleId específico é
     * o hook da navegação prev/next: a Home passa o ID do ciclo escolhido
     * no CycleNavigator e recebe os agregados daquele ciclo.
     */
    suspend fun getHome(cycleId: String? = null): HomeResponse = api.getHome(cycleId)
}
