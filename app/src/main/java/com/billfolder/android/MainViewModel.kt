package com.billfolder.android

import androidx.lifecycle.ViewModel
import com.billfolder.android.data.preferences.ThemeMode
import com.billfolder.android.data.preferences.ThemePreferences
import com.billfolder.android.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

/**
 * ViewModel da Activity raiz: decide o start destination do NavHost (Home
 * se já tem token salvo, Login se não) e expõe a aparência escolhida.
 *
 * Usa StateFlow nullable pra UI conseguir distinguir "ainda não sei"
 * (null — mantém splash) de "sei que sim/não" (true/false — renderiza NavHost).
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    authRepository: AuthRepository,
    themePreferences: ThemePreferences,
) : ViewModel() {

    /** Aparência escolhida; null até o DataStore responder (evita piscar o tema errado). */
    val themeMode: StateFlow<ThemeMode?> = themePreferences.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    val isLoggedIn: StateFlow<Boolean?> = authRepository.isLoggedIn
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )
}
