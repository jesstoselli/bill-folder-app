package com.billfolder.android.ui.screens.managesavings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.SavingsAccountResponse
import com.billfolder.android.data.repository.SavingsRepository
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
 * Tela "gerenciar > poupanças". CRUD completo da entidade SavingsAccount —
 * lista, adiciona, edita, deleta. Igual a Cards: a entidade é durável,
 * não depende de ciclo (transações de poupança é que serão filtradas por
 * ciclo, mas isso é Fase B).
 *
 * Deleção é em 2 passos: swipe-left abre dialog de confirmação
 * (pendingDelete), confirmação faz a chamada DELETE. DELETE no backend
 * tem CASCADE — apaga todas as SavingsTransactions vinculadas. Cancelar
 * limpa o pending.
 *
 * Edição: swipe-right abre AddSavingsAccountSheet em modo edit (editing).
 * PATCH é feito pela própria sheet; aqui só guardamos o flag pra propagar
 * pro `existing` da sheet e pro `isPending` do SwipeToActionRow. Lembrar
 * que checkingAccountId é imutável no PATCH — sheet em modo edit precisa
 * deixar o dropdown de checking disabled.
 */
sealed interface ManageSavingsUiState {
    data object Loading : ManageSavingsUiState
    data class Content(
        val accounts: List<SavingsAccountResponse>,
        val pendingDelete: SavingsAccountResponse? = null,
        val editing: SavingsAccountResponse? = null,
        val deletingId: String? = null,
    ) : ManageSavingsUiState
    data class Error(val message: String) : ManageSavingsUiState
}

@HiltViewModel
class ManageSavingsViewModel @Inject constructor(
    private val savingsRepository: SavingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ManageSavingsUiState>(ManageSavingsUiState.Loading)
    val state: StateFlow<ManageSavingsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun refresh() {
        if (_state.value is ManageSavingsUiState.Loading) return
        _state.value = ManageSavingsUiState.Loading
        load()
    }

    /** Swipe completou — pede confirmação antes de fazer DELETE. */
    fun requestDelete(account: SavingsAccountResponse) {
        _state.update { current ->
            if (current is ManageSavingsUiState.Content) {
                current.copy(pendingDelete = account)
            } else {
                current
            }
        }
    }

    fun cancelDelete() {
        _state.update { current ->
            if (current is ManageSavingsUiState.Content) {
                current.copy(pendingDelete = null)
            } else {
                current
            }
        }
    }

    /**
     * User confirmou no dialog — faz DELETE. Em sucesso recarrega lista
     * (optimistic-ish: remove local, próximo refresh confirma com backend).
     * Atenção: backend faz CASCADE em SavingsTransactions — confirmar com
     * o user no dialog que ele entende a consequência (texto da sheet
     * cuida disso no Passo 3).
     */
    fun confirmDelete() {
        val current = _state.value
        if (current !is ManageSavingsUiState.Content) return
        val account = current.pendingDelete ?: return

        _state.update {
            (it as? ManageSavingsUiState.Content)
                ?.copy(pendingDelete = null, deletingId = account.id)
                ?: it
        }

        viewModelScope.launch {
            try {
                savingsRepository.deleteAccount(account.id)
                // Optimistic-ish: remove da lista local. Próximo refresh
                // confirma com o backend de qualquer jeito.
                _state.update { s ->
                    if (s is ManageSavingsUiState.Content) {
                        s.copy(
                            accounts = s.accounts.filterNot { it.id == account.id },
                            deletingId = null,
                        )
                    } else {
                        s
                    }
                }
            } catch (e: HttpException) {
                _state.value = ManageSavingsUiState.Error("Erro ao deletar (HTTP ${e.code()})")
            } catch (e: IOException) {
                _state.value = ManageSavingsUiState.Error("Sem conexão. Tenta de novo.")
            } catch (e: Exception) {
                _state.value = ManageSavingsUiState.Error(e.message ?: "Algo deu errado.")
            }
        }
    }

    // ------------------------------------------------------------------------
    // Edit flow (swipe-right) — toggles do flag; PATCH é feito pela sheet
    // ------------------------------------------------------------------------

    fun requestEdit(account: SavingsAccountResponse) {
        _state.update { current ->
            if (current is ManageSavingsUiState.Content) {
                current.copy(editing = account)
            } else {
                current
            }
        }
    }

    fun cancelEdit() {
        _state.update { current ->
            if (current is ManageSavingsUiState.Content) {
                current.copy(editing = null)
            } else {
                current
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = try {
                ManageSavingsUiState.Content(accounts = savingsRepository.listAccounts())
            } catch (e: HttpException) {
                ManageSavingsUiState.Error("Erro ao carregar (HTTP ${e.code()})")
            } catch (e: IOException) {
                ManageSavingsUiState.Error("Sem conexão. Verifique sua internet.")
            } catch (e: Exception) {
                ManageSavingsUiState.Error(e.message ?: "Algo deu errado.")
            }
        }
    }
}
