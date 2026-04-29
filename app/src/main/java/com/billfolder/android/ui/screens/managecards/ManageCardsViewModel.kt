package com.billfolder.android.ui.screens.managecards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.CreditCardAccountResponse
import com.billfolder.android.data.repository.CardsRepository
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
 * Tela "gerenciar > cartões". CRUD da entidade CreditCardAccount —
 * lista, adiciona, deleta. Cartões são durávels; não dependem de ciclo.
 *
 * Deleção é em 2 passos: swipe abre dialog de confirmação (pendingDelete),
 * confirmação faz a chamada DELETE. Cancelar (ou tap fora) limpa o pending.
 */
sealed interface ManageCardsUiState {
    data object Loading : ManageCardsUiState
    data class Content(
        val cards: List<CreditCardAccountResponse>,
        val pendingDelete: CreditCardAccountResponse? = null,
        val deletingId: String? = null,
    ) : ManageCardsUiState
    data class Error(val message: String) : ManageCardsUiState
}

@HiltViewModel
class ManageCardsViewModel @Inject constructor(
    private val cardsRepository: CardsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ManageCardsUiState>(ManageCardsUiState.Loading)
    val state: StateFlow<ManageCardsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun refresh() {
        if (_state.value is ManageCardsUiState.Loading) return
        _state.value = ManageCardsUiState.Loading
        load()
    }

    /** Swipe completou — pede confirmação antes de fazer DELETE. */
    fun requestDelete(card: CreditCardAccountResponse) {
        _state.update { current ->
            if (current is ManageCardsUiState.Content) {
                current.copy(pendingDelete = card)
            } else {
                current
            }
        }
    }

    fun cancelDelete() {
        _state.update { current ->
            if (current is ManageCardsUiState.Content) {
                current.copy(pendingDelete = null)
            } else {
                current
            }
        }
    }

    /** User confirmou no dialog — faz DELETE. Em sucesso recarrega lista. */
    fun confirmDelete() {
        val current = _state.value
        if (current !is ManageCardsUiState.Content) return
        val card = current.pendingDelete ?: return

        _state.update {
            (it as? ManageCardsUiState.Content)
                ?.copy(pendingDelete = null, deletingId = card.id)
                ?: it
        }

        viewModelScope.launch {
            try {
                cardsRepository.deleteCard(card.id)
                // Optimistic-ish: remove da lista local. Próximo refresh
                // confirma com o backend de qualquer jeito.
                _state.update { s ->
                    if (s is ManageCardsUiState.Content) {
                        s.copy(
                            cards = s.cards.filterNot { it.id == card.id },
                            deletingId = null,
                        )
                    } else {
                        s
                    }
                }
            } catch (e: HttpException) {
                _state.value = ManageCardsUiState.Error("Erro ao deletar (HTTP ${e.code()})")
            } catch (e: IOException) {
                _state.value = ManageCardsUiState.Error("Sem conexão. Tenta de novo.")
            } catch (e: Exception) {
                _state.value = ManageCardsUiState.Error(e.message ?: "Algo deu errado.")
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = try {
                ManageCardsUiState.Content(cards = cardsRepository.listCards())
            } catch (e: HttpException) {
                ManageCardsUiState.Error("Erro ao carregar (HTTP ${e.code()})")
            } catch (e: IOException) {
                ManageCardsUiState.Error("Sem conexão. Verifique sua internet.")
            } catch (e: Exception) {
                ManageCardsUiState.Error(e.message ?: "Algo deu errado.")
            }
        }
    }
}
