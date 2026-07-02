package com.billfolder.android.ui.screens.managebanks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.CheckingAccountResponse
import com.billfolder.android.data.repository.CheckingAccountsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/**
 * Tela "gerenciar > bancos". CRUD completo de CheckingAccount — mesmo
 * molde do ManageSavings/ManageCards. Contas são duráveis; não dependem
 * de ciclo.
 *
 * Deleção é em 2 passos: swipe-left abre dialog de confirmação; confirm
 * faz DELETE. Backend faz CASCADE em savings vinculadas (1:1) e SET NULL
 * em income entries — o dialog avisa disso.
 *
 * Edição: swipe-right abre AddCheckingAccountSheet em modo edit. PATCH é
 * feito pela sheet.
 */
sealed interface ManageBanksUiState {
    data object Loading : ManageBanksUiState
    data class Content(
        val accounts: List<CheckingAccountResponse>,
        val pendingDelete: CheckingAccountResponse? = null,
        val editing: CheckingAccountResponse? = null,
        val deletingId: String? = null,
    ) : ManageBanksUiState
    data class Error(val message: String) : ManageBanksUiState
}

@HiltViewModel
class ManageBanksViewModel @Inject constructor(
    private val checkingAccountsRepository: CheckingAccountsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ManageBanksUiState>(ManageBanksUiState.Loading)
    val state: StateFlow<ManageBanksUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun refresh() {
        if (_state.value is ManageBanksUiState.Loading) return
        _state.value = ManageBanksUiState.Loading
        load()
    }

    fun requestDelete(account: CheckingAccountResponse) {
        _state.update { current ->
            if (current is ManageBanksUiState.Content) {
                current.copy(pendingDelete = account)
            } else {
                current
            }
        }
    }

    fun cancelDelete() {
        _state.update { current ->
            if (current is ManageBanksUiState.Content) {
                current.copy(pendingDelete = null)
            } else {
                current
            }
        }
    }

    fun confirmDelete() {
        val current = _state.value
        if (current !is ManageBanksUiState.Content) return
        val account = current.pendingDelete ?: return

        _state.update {
            (it as? ManageBanksUiState.Content)
                ?.copy(pendingDelete = null, deletingId = account.id)
                ?: it
        }

        viewModelScope.launch {
            try {
                checkingAccountsRepository.deleteAccount(account.id)
                _state.update { s ->
                    if (s is ManageBanksUiState.Content) {
                        s.copy(
                            accounts = s.accounts.filterNot { it.id == account.id },
                            deletingId = null,
                        )
                    } else {
                        s
                    }
                }
            } catch (e: HttpException) {
                _state.value = ManageBanksUiState.Error("Erro ao deletar (HTTP ${e.code()})")
            } catch (e: IOException) {
                _state.value = ManageBanksUiState.Error("Sem conexão. Tenta de novo.")
            } catch (e: Exception) {
                _state.value = ManageBanksUiState.Error(e.message ?: "Algo deu errado.")
            }
        }
    }

    fun requestEdit(account: CheckingAccountResponse) {
        _state.update { current ->
            if (current is ManageBanksUiState.Content) {
                current.copy(editing = account)
            } else {
                current
            }
        }
    }

    fun cancelEdit() {
        _state.update { current ->
            if (current is ManageBanksUiState.Content) {
                current.copy(editing = null)
            } else {
                current
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = try {
                ManageBanksUiState.Content(
                    accounts = checkingAccountsRepository.listAccounts(),
                )
            } catch (e: HttpException) {
                ManageBanksUiState.Error("Erro ao carregar (HTTP ${e.code()})")
            } catch (e: IOException) {
                ManageBanksUiState.Error("Sem conexão. Verifique sua internet.")
            } catch (e: Exception) {
                ManageBanksUiState.Error(e.message ?: "Algo deu errado.")
            }
        }
    }
}
