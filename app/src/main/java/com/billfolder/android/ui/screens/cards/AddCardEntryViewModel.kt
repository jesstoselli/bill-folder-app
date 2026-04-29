package com.billfolder.android.ui.screens.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.CategoryDto
import com.billfolder.android.data.dto.CreateCardEntryRequest
import com.billfolder.android.data.dto.CreditCardAccountResponse
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

    fun onPurchaseDateChange(iso: String) = _state.update { it.copy(purchaseDate = iso) }
    fun onLabelChange(value: String) = _state.update { it.copy(label = value) }
    fun onTotalAmountChange(value: String) = _state.update { it.copy(totalAmount = value) }
    fun onInstallmentsChange(value: String) = _state.update { it.copy(installmentsCount = value) }
    fun onCardChange(id: String) = _state.update { it.copy(selectedCardId = id) }
    fun onCategoryChange(id: String) = _state.update { it.copy(selectedCategoryId = id) }
    fun onNotesChange(value: String) = _state.update { it.copy(notes = value) }

    fun submit(
        labelEmptyMessage: String,
        amountInvalidMessage: String,
        cardEmptyMessage: String,
        installmentsInvalidMessage: String,
        categoryEmptyMessage: String,
    ) {
        val current = _state.value
        val parsedAmount = parseAmount(current.totalAmount) ?: 0.0
        val parsedInstallments = current.installmentsCount.toIntOrNull() ?: 0

        val validationError = when {
            current.label.isBlank()                      -> labelEmptyMessage
            parsedAmount <= 0                            -> amountInvalidMessage
            current.selectedCardId.isNullOrBlank()       -> cardEmptyMessage
            parsedInstallments < 1                       -> installmentsInvalidMessage
            current.selectedCategoryId.isNullOrBlank()   -> categoryEmptyMessage
            else                                          -> null
        }
        if (validationError != null) {
            _state.update { it.copy(errorMessage = validationError) }
            return
        }

        val request = CreateCardEntryRequest(
            cardId = current.selectedCardId!!,
            purchaseDate = current.purchaseDate,
            label = current.label.trim(),
            totalAmount = parsedAmount,
            installmentsCount = parsedInstallments,
            categoryId = current.selectedCategoryId!!,
            notes = current.notes.takeIf { it.isNotBlank() }?.trim(),
        )

        _state.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                cardsRepository.createEntry(request)
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
                        // Pré-seleciona o único cartão se só tem um
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
}
