package com.billfolder.android.data.repository

import com.billfolder.android.data.api.BillFolderApi
import com.billfolder.android.data.dto.CycleResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CyclesRepository @Inject constructor(
    private val api: BillFolderApi,
) {
    /** Retorna o ciclo aberto. Lança HttpException(404) se não houver. */
    suspend fun getCurrent(): CycleResponse = api.getCurrentCycle()
}
