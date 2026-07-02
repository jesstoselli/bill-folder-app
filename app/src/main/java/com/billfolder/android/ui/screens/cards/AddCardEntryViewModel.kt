package com.billfolder.android.ui.screens.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.CardEntryResponse
import com.billfolder.android.data.dto.CategoryDto
import com.billfolder.android.data.dto.CreateCardEntryRequest
import com.billfolder.android.data.dto.CreditCardAccountResponse
import com.billfolder.android.data.dto.UpdateCardEntryRequest
import com.billfolder.android.data.repository.CardsRepository
import com.billfolder.android.data.repository.ReferenceDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject

/**
 * Form de "nova/editar compra no cartão".
 *
 * Modos:
 *  - Create (editingId == null): POST com todos os campos.
 *  - Edit (editingId != null): PATCH só com label/categoryId/notes —
 *    backend não permite mudar cartão/data/valor/parcelas (mexer em
 *    qualquer um exigiria recalcular installments e mover entre
 *    statements). Sheet bloqueia esses campos visualmente.
 */
data class AddCardEntryFormState(
    val purchaseDate: String = LocalDate.now().toString(),
    val label: String = "",
    val totalAmount: String = "",
    val installmentsCount: String = "1",
    val selectedCardId: String? = null,
    val selectedCategoryId: String? = null,
    val notes: String = "",

    val cards: List<CreditCardAccountResponse> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val isLoadingReferences: Boolean = true,

    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedSuccessfully: Boolean = false,

    val editingId: String? = null,
)

@HiltViewModel
class AddCardEntryViewModel @Inject constructor(
    private val cardsRepository: CardsRepository,
    private val referenceDataRepository: ReferenceDataRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AddCardEntryFormState())
    val state: StateFlow<AddCardEntryFormState> = _state.asStateFlow()

    init {
        loadReferences()
    }

    /**
     * Reseta o form pros valores iniciais. Chamado pelo sheet toda vez
     * que abre (via LaunchedEffect(Unit)) porque o hiltViewModel() é
     * compartilhado entre aberturas — sem esse reset, savedSuccessfully
     * = true da submissão anterior faria o sheet fechar imediatamente
     * na 2ª abertura antes do user interagir.
     *
     * Preserva references (cards/categories) e isLoadingReferences porque
     * o init { loadReferences() } só roda uma vez — se resetássemos, os
     * dropdowns ficariam vazios sem forma de recarregar. Também restaura
     * a pré-seleção do primeiro cartão (mesma lógica do loadReferences).
     */
    fun resetForm() {
        val current = _state.value
        _state.value = AddCardEntryFormState(
            cards = current.cards,
            categories = current.categories,
            isLoadingReferences = current.isLoadingReferences,
            selectedCardId = current.cards.firstOrNull()?.id,
        )
    }

    fun onPurchaseDateChange(iso: String) = _state.update { it.copy(purchaseDate = iso) }
    fun onLabelChange(value: String) = _state.update { it.copy(label = value) }
    fun onTotalAmountChange(value: String) = _state.update { it.copy(totalAmount = value) }
    fun onInstallmentsChange(value: String) = _state.update { it.copy(installmentsCount = value) }
    fun onCardChange(id: String) = _state.update { it.copy(selectedCardId = id) }
    fun onCategoryChange(id: String) = _state.update { it.copy(selectedCategoryId = id) }
    fun onNotesChange(value: String) = _state.update { it.copy(notes = value) }

    /**
     * Preenche o form com uma entry existente — modo edit. Todos os campos
     * são populados pra dar contexto ao user, mas só label/categoria/notes
     * vão ser submetidos no PATCH (sheet desabilita os outros visualmente).
     * Idempotente — checa editingId pra não resetar.
     */
    fun prefill(item: CardEntryResponse) {
        if (_state.value.editingId == item.id) return
        _state.update {
            it.copy(
                editingId = item.id,
                purchaseDate = item.purchaseDate,
                label = item.label,
                totalAmount = item.totalAmount.toBrlInputString(),
                installmentsCount = item.installmentsCount.toString(),
                selectedCardId = item.cardId,
                selectedCategoryId = item.categoryId,
                notes = item.notes.orEmpty(),
                errorMessage = null,
            )
        }
    }

    fun submit(
        labelEmptyMessage: String,
        amountInvalidMessage: String,
        cardEmptyMessage: String,
        installmentsInvalidMessage: String,
        categoryEmptyMessage: String,
    ) {
        val current = _state.value

        // Em modo edit, validamos só os campos que vão no PATCH (label,
        // categoria). Os outros já são imutáveis pelo prefill, sem motivo
        // pra revalidar.
        val validationError = if (current.editingId != null) {
            when {
                current.label.isBlank()                    -> labelEmptyMessage
                current.selectedCategoryId.isNullOrBlank() -> categoryEmptyMessage
                else                                       -> null
            }
        } else {
            val parsedAmount = parseAmount(current.totalAmount) ?: 0.0
            val parsedInstallments = current.installmentsCount.toIntOrNull() ?: 0
            when {
                current.label.isBlank()                    -> labelEmptyMessage
                parsedAmount <= 0                          -> amountInvalidMessage
                current.selectedCardId.isNullOrBlank()     -> cardEmptyMessage
                parsedInstallments < 1                     -> installmentsInvalidMessage
                current.selectedCategoryId.isNullOrBlank() -> categoryEmptyMessage
                else                                       -> null
            }
        }
        if (validationError != null) {
            _state.update { it.copy(errorMessage = validationError) }
            return
        }

        _state.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                if (current.editingId != null) {
                    // PATCH limitado: backend só aceita label/categoria/notes.
                    val request = UpdateCardEntryRequest(
                        label = current.label.trim(),
                        categoryId = current.selectedCategoryId,
                        notes = current.notes.trim(),
                    )
                    cardsRepository.updateEntry(current.editingId, request)
                } else {
                    val request = CreateCardEntryRequest(
                        cardId = current.selectedCardId!!,
                        purchaseDate = current.purchaseDate,
                        label = current.label.trim(),
                        totalAmount = parseAmount(current.totalAmount)!!,
                        installmentsCount = current.installmentsCount.toInt(),
                        categoryId = current.selectedCategoryId!!,
                        notes = current.notes.takeIf { it.isNotBlank() }?.trim(),
                    )
                    cardsRepository.createEntry(request)
                }
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

    private fun loadReferences() {
        viewModelScope.launch {
            try {
                val (cards, categories) = coroutineScope {
                    val c = async { cardsRepository.listCards() }
                    val cat = async { referenceDataRepository.getCategories() }
                    awaitAll(c, cat).let {
                        @Suppress("UNCHECKED_CAST")
                        Pair(it[0] as List<CreditCardAccountResponse>, it[1] as List<CategoryDto>)
                    }
                }
                _state.update {
                    it.copy(
                        cards = cards,
                        categories = categories,
                        // Pré-seleciona o único cartão se só tem um. Em modo edit,
                        // respeita o que veio do prefill (já tem selectedCardId).
                        selectedCardId = it.selectedCardId
                            ?: cards.firstOrNull()?.id,
                        isLoadingReferences = false,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoadingReferences = false,
                        errorMessage = "Falha ao carregar opções.",
                    )
                }
            }
        }
    }

    private fun parseAmount(input: String): Double? =
        input.replace(',', '.').toDoubleOrNull()

    /** "1234.5" → "1234,50" pra preencher o MoneyField em modo edit. */
    private fun Double.toBrlInputString(): String =
        "%.2f".format(this).replace('.', ',')
}
