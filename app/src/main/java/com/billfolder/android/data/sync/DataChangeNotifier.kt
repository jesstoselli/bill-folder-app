package com.billfolder.android.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sinal global de "os dados mudaram". Um único @Singleton compartilhado por
 * todo o app (Hilt provê via @Inject constructor).
 *
 * Motivação: telas de consumo (Home, Cards, Expenses, Income, DailyExpenses,
 * Savings, Adjustments) têm ViewModels independentes, cada um com seu próprio
 * snapshot do backend. Como o drawer usa saveState/restoreState, esses VMs são
 * RETIDOS ao navegar — `init { load() }` não roda de novo. Antes, uma compra
 * adicionada no Cards não aparecia na Home até relogar.
 *
 * Agora todo write passa pelos repositories, que chamam [notifyDataChanged]
 * após sucesso. Cada VM de consumo observa [changes] (via observeDataChanges)
 * e refaz o fetch in-place. Desacoplado 100% da navegação — funciona voltando
 * pelo drawer, pelo botão back ou pelo app voltando do background.
 *
 * A "versão" é um contador monotônico; o valor em si não importa, só a
 * MUDANÇA. StateFlow conflaciona, então múltiplos writes em rajada podem
 * colapsar num único refresh — o que é desejável (menos fetches redundantes).
 */
@Singleton
class DataChangeNotifier @Inject constructor() {

    private val _changes = MutableStateFlow(0L)

    /** Versão atual dos dados. Incrementa a cada mutação bem-sucedida. */
    val changes: StateFlow<Long> = _changes.asStateFlow()

    /** Chamado pelos repositories após qualquer write bem-sucedido. */
    fun notifyDataChanged() {
        _changes.update { it + 1 }
    }
}

/**
 * Executa um write ([block]) e só então sinaliza a mudança. Se [block] lança
 * (rede, HTTP não-2xx convertido em exception, etc), o bump NÃO acontece —
 * nada mudou de fato no backend. Chokepoint único usado por todos os
 * repositories pra não esquecer de notificar em nenhum caminho de escrita.
 */
suspend fun <T> DataChangeNotifier.notifyingOnSuccess(block: suspend () -> T): T {
    val result = block()
    notifyDataChanged()
    return result
}
