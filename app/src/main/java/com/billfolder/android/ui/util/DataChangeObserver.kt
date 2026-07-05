package com.billfolder.android.ui.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.sync.DataChangeNotifier
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Faz o ViewModel reagir ao [DataChangeNotifier] global: sempre que qualquer
 * write acontece em qualquer tela, [onChange] é chamado. Uso típico no init:
 *
 *   init {
 *       load()
 *       observeDataChanges(dataChangeNotifier) { pullRefresh() }
 *   }
 *
 * Substitui a antiga abordagem acoplada à navegação (observeDrawerRefresh +
 * RefreshOnResume), que não funcionava porque:
 *  - RefreshOnResume guardava o flag "primeira vez" em `remember`, que morre
 *    quando a tela sai da composição no nav — toda volta era tratada como a
 *    primeira e o refresh nunca disparava.
 *  - observeDrawerRefresh escrevia no savedStateHandle do entry recriado por
 *    restoreState (instância diferente da observada pelo VM retido) e o
 *    `.drop(1)` após `filterNotNull()` ainda comia o primeiro trigger real.
 *
 * Aqui o `.drop(1)` é correto: o StateFlow replica só o valor atual na 1ª
 * coleta (o contador de versão nunca é null), então dropamos exatamente essa
 * emissão inicial — o fetch inicial já é feito pelo `init { load() }`.
 */
fun ViewModel.observeDataChanges(
    notifier: DataChangeNotifier,
    onChange: () -> Unit,
) {
    viewModelScope.launch {
        notifier.changes
            .drop(1)
            .collect { onChange() }
    }
}
