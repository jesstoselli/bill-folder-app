package com.billfolder.android.ui.screens.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.repository.IncomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject

data class ConfirmIncomeFormState(
    val entryId: String = "",
    val actualDate: String = LocalDate.now().toString(),
    val actualAmount: String = "",

    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedSuccessfully: Boolean = false,
)

@HiltViewModel
class ConfirmIncomeViewModel @Inject constructor(
    private val incomeRepository: IncomeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ConfirmIncomeFormState())
    val state: StateFlow<ConfirmIncomeFormState> = _state.asStateFlow()

    /** Pré-preenche actualAmount com expectedAmount do entry sendo confirmado. */
    fun initializeFor(entryId: String, expectedAmount: Double) {
        _state.update {
            it.copy(
                entryId = entryId,
                actualAmount = formatAmount(expectedAmount),
            )
        }
    }

    fun onActualDateChange(iso: String) = _state.update { it.copy(actualDate = iso) }
    fun onActualAmountChange(value: String) = _state.update { it.copy(actualAmount = value) }

    fun submit(amountInvalidMessage: String) {
        val current = _state.value
        val amountValue = parseAmount(current.actualAmount) ?: 0.0
        if (amountValue <= 0) {
            _state.update { it.copy(errorMessage = amountInvalidMessage) }
            return
        }

        _state.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                incomeRepository.markReceived(
                    id = current.entryId,
                    actualDate = current.actualDate,
                    actualAmount = amountValue,
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

    private fun formatAmount(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}
