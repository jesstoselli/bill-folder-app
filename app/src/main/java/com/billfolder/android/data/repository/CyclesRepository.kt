package com.billfolder.android.data.repository

import com.billfolder.android.data.api.BillFolderApi
import com.billfolder.android.data.dto.CreateCycleRequest
import com.billfolder.android.data.dto.CycleResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CyclesRepository @Inject constructor(
    private val api: BillFolderApi,
) {
    /** Retorna o ciclo aberto. Lança HttpException(404) se não houver. */
    suspend fun getCurrent(): CycleResponse = api.getCurrentCycle()

    /**
     * Lista todos os ciclos do user, ordenados por startDate asc pelo
     * backend. Usado pela navegação prev/next — precisamos da lista
     * completa pra resolver "qual é o ciclo antes/depois deste ID?".
     * from/to opcionais permitem limitar a janela se algum dia virar
     * paginação (por enquanto o backend devolve tudo).
     */
    suspend fun list(from: String? = null, to: String? = null): List<CycleResponse> =
        api.listCycles(from, to)

    /**
     * Cria um novo ciclo. Pode lançar HttpException(409) se já houver
     * um ciclo do user com a mesma startDate — caller deve traduzir
     * pra mensagem em PT (convenção: "Já existe um ciclo começando
     * nessa data.").
     */
    suspend fun create(request: CreateCycleRequest): CycleResponse =
        api.createCycle(request)
}
