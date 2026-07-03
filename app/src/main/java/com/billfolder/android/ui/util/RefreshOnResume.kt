package com.billfolder.android.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * Dispara [refresh] toda vez que a tela volta pro RESUMED state (ex: user
 * navegou pra outra tela via drawer e voltou), pulando a primeira chamada.
 *
 * Motivo: o nav do drawer usa `popUpTo(HOME) { saveState = true } +
 * restoreState = true` pra preservar scroll/state entre navegações. Isso
 * evita `init { load() }` do VM rodar de novo quando a tela é restaurada,
 * então mudanças feitas em outra tela (ex: adicionar compra no cartão)
 * não aparecem até o app ser morto.
 *
 * A 1ª chamada de [LifecycleEventEffect] com ON_RESUME coincide com a
 * composição inicial — não queremos double-fetch nem loading flash. O
 * flag [isInitialResume] segura a primeira e libera a partir da segunda.
 *
 * Uso: colocar no topo de cada tela que precisa desse comportamento:
 *   RefreshOnResume { viewModel.refresh() }
 */
@Composable
fun RefreshOnResume(refresh: () -> Unit) {
    var isInitialResume by remember { mutableStateOf(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (isInitialResume) {
            isInitialResume = false
        } else {
            refresh()
        }
    }
}
