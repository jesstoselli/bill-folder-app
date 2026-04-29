package com.billfolder.android.ui.screens.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.CreateCreditCardAccountRequest
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
 * Form de cadastrar cartão. closingDay e dueDay são entrados como string
 * (TextField numérico) e validados como inteiros 1..31. Pra MVP não
 * forçamos diferença entre os dois, embora na prática sempre são —
 * backend valida o resto.
 */
data class AddCreditCardFormState(
    val name: String = "",
    val issuerBank: String = "",
    val brand: String = "",
    val closingDay: String = "",
    val dueDay: String = "",

    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedSuccessfully: Boolean = false,
)

@HiltViewModel
class AddCreditCardViewModel @Inject constructor(
    private val cardsRepository: CardsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AddCreditCardFormState())
    val state: StateFlow<AddCreditCardFormState> = _state.asStateFlow()

    fun onNameChange(value: String) = _state.update { it.copy(name = value) }
    fun onIssuerBankChange(value: String) = _state.update { it.copy(issuerBank = value) }
    fun onBrandChange(value: String) = _state.update { it.copy(brand = value) }
    fun onClosingDayChange(value: String) = _state.update { it.copy(closingDay = value) }
    fun onDueDayChange(value: String) = _state.update { it.copy(dueDay = value) }

    fun submit(
        nameEmptyMessage: String,
        closingInvalidMessage: String,
        dueInvalidMessage: String,
    ) {
        val current = _state.value
        val closingDay = current.closingDay.toIntOrNull()
        val dueDay = current.dueDay.toIntOrNull()

        val validationError = when {
            current.name.isBlank()                 -> nameEmptyMessage
            closingDay == null || closingDay !in 1..31 -> closingInvalidMessage
            dueDay == null || dueDay !in 1..31     -> dueInvalidMessage
            else                                    -> null
        }
        if (validationError != null) {
            _state.update { it.copy(errorMessage = validationError) }
            return
        }

        val request = CreateCreditCardAccountRequest(
            name = current.name.trim(),
            issuerBank = current.issuerBank.takeIf { it.isNotBlank() }?.trim(),
            brand = current.brand.takeIf { it.isNotBlank() }?.trim(),
            closingDay = closingDay!!,
            dueDay = dueDay!!,
        )

        _state.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                cardsRepository.createCard(request)
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
}
