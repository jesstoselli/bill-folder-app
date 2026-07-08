package com.billfolder.android.ui.screens.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.UpdateCardSubscriptionAmountRequest
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
 * ViewModel do RepriceSubscriptionSheet — "reprecificar" uma assinatura de
 * cartão (Netflix subiu de R$39,90 pra R$49,90). Variante enxuta: só o novo
 * valor e o escopo, sem data/conta.
 *
 * O escopo NÃO fica no form state — ele vem do RecurrenceScopeDialog que o
 * sheet abre no CTA, e é passado direto pro submit(scope). Assim a decisão
 * "só esta / esta e as próximas" fica no mesmo modal reutilizável do delete.
 *
 * ATENÇÃO ao casing do literal do escopo: o body do reprice usa camelCase
 * ("this"/"thisAndFollowing"), diferente do delete (snake_case). Quem
 * resolve o literal é ScopeChoice.repriceLiteral() no sheet.
 */
data class RepriceSubscriptionFormState(
    val entryId: String = "",
    val amount: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedSuccessfully: Boolean = false,
)

@HiltViewModel
class RepriceSubscriptionViewModel @Inject constructor(
    private val cardsRepository: CardsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RepriceSubscriptionFormState())
    val state: StateFlow<RepriceSubscriptionFormState> = _state.asStateFlow()

    /** Reseta o form (o sheet chama ao abrir/fechar, VM é scoped na tela pai). */
    fun resetForm() {
        _state.value = RepriceSubscriptionFormState()
    }

    /** Inicializa pré-preenchendo o valor atual da assinatura (editável). */
    fun initializeFor(entryId: String, currentAmount: Double) {
        _state.update {
            it.copy(entryId = entryId, amount = formatAmount(currentAmount))
        }
    }

    fun onAmountChange(value: String) = _state.update { it.copy(amount = value) }

    /**
     * `scope` é o literal camelCase já resolvido ("this"/"thisAndFollowing").
     * O caller (sheet) passa ScopeChoice.repriceLiteral() do modal de escopo.
     */
    fun submit(scope: String, amountInvalidMessage: String) {
        val current = _state.value
        val amountValue = parseAmount(current.amount) ?: 0.0
        if (amountValue <= 0) {
            _state.update { it.copy(errorMessage = amountInvalidMessage) }
            return
        }

        _state.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                cardsRepository.updateSubscriptionAmount(
                    id = current.entryId,
                    request = UpdateCardSubscriptionAmountRequest(
                        amount = amountValue,
                        scope = scope,
                    ),
                )
                _state.update { it.copy(isSaving = false, savedSuccessfully = true) }
            } catch (e: HttpException) {
                _state.update { it.copy(isSaving = false, errorMessage = "Erro do servidor (HTTP ${e.code()}).") }
            } catch (e: IOException) {
                _state.update { it.copy(isSaving = false, errorMessage = "Sem conexão. Tenta de novo.") }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, errorMessage = e.message ?: "Algo deu errado.") }
            }
        }
    }

    private fun parseAmount(input: String): Double? =
        input.replace(',', '.').toDoubleOrNull()

    /** "39.90" cru — UI espera valor sem máscara BRL. */
    private fun formatAmount(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}
