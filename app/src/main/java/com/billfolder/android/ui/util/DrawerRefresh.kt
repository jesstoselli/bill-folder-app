package com.billfolder.android.ui.util

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/** Chave usada no SavedStateHandle pra sinalizar "refresh vindo do drawer". */
const val DRAWER_REFRESH_TRIGGER_KEY = "drawerRefreshTrigger"

/**
 * Observa o sinal emitido pelo BillFolderNavHost.navigateFromDrawer sempre
 * que o user clica um item no menu lateral. A cada clique, o NavHost grava
 * `System.currentTimeMillis()` em `savedStateHandle[DRAWER_REFRESH_TRIGGER_KEY]`.
 *
 * Como o drawer usa `saveState = true` + `restoreState = true` pra preservar
 * scroll/state das telas, o VM NÃO é recriado ao voltar (init { load() }
 * não roda). Sem esse observador, dados ficam desatualizados até restart
 * do app ou pull-to-refresh manual.
 *
 * `.drop(1)` pula a emissão inicial do StateFlow (que devolve o valor atual
 * na 1ª coleta) — na primeira criação do VM, init { load() } já cuida do
 * fetch inicial, então não queremos double-fetch.
 *
 * Usa: no init do VM, depois de load():
 *
 *   observeDrawerRefresh(savedStateHandle) { pullRefresh() }
 */
fun ViewModel.observeDrawerRefresh(
    savedStateHandle: SavedStateHandle,
    onRefresh: () -> Unit,
) {
    viewModelScope.launch {
        savedStateHandle.getStateFlow<Long?>(DRAWER_REFRESH_TRIGGER_KEY, null)
            .filterNotNull()
            .drop(1)
            .collect { onRefresh() }
    }
}
