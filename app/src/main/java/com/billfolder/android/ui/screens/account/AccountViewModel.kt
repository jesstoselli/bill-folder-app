package com.billfolder.android.ui.screens.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.UserDto
import com.billfolder.android.data.preferences.ReminderPreferences
import com.billfolder.android.data.preferences.ThemeMode
import com.billfolder.android.data.preferences.ThemePreferences
import com.billfolder.android.data.repository.AuthRepository
import com.billfolder.android.data.repository.UsersRepository
import com.billfolder.android.notifications.DueBillsScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/** Dados do usuário na tela "sua conta". */
sealed interface AccountUserState {
    data object Loading : AccountUserState
    data class Loaded(val user: UserDto) : AccountUserState
    data class Error(val message: String) : AccountUserState
}

/**
 * Tela "sua conta": quem está logado, a aparência e os lembretes deste
 * aparelho, e o sair. As escolhas são salvas na hora — não tem botão de salvar.
 */
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val usersRepository: UsersRepository,
    private val authRepository: AuthRepository,
    private val themePreferences: ThemePreferences,
    private val reminderPreferences: ReminderPreferences,
    private val dueBillsScheduler: DueBillsScheduler,
) : ViewModel() {

    private val _user = MutableStateFlow<AccountUserState>(AccountUserState.Loading)
    val user: StateFlow<AccountUserState> = _user.asStateFlow()

    val themeMode: StateFlow<ThemeMode> = themePreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val dueRemindersEnabled: StateFlow<Boolean> = reminderPreferences.dueRemindersEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        loadUser()
    }

    fun loadUser() {
        _user.value = AccountUserState.Loading
        viewModelScope.launch {
            _user.value = try {
                AccountUserState.Loaded(usersRepository.me())
            } catch (e: HttpException) {
                AccountUserState.Error("Erro ao carregar (HTTP ${e.code()})")
            } catch (e: IOException) {
                AccountUserState.Error("Sem conexão. Tenta de novo.")
            } catch (e: Exception) {
                AccountUserState.Error(e.message ?: "Algo deu errado.")
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { themePreferences.setThemeMode(mode) }
    }

    /** A permissão de notificação (Android 13+) é pedida pela tela antes de ligar. */
    fun setDueRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            reminderPreferences.setDueRemindersEnabled(enabled)
            if (enabled) {
                dueBillsScheduler.schedule(reminderPreferences.lastCheckedDate.first())
            } else {
                dueBillsScheduler.cancel()
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }
}
