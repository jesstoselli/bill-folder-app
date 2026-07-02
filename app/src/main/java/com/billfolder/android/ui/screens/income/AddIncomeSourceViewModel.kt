package com.billfolder.android.ui.screens.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.CreateIncomeSourceRequest
import com.billfolder.android.data.dto.IncomeSourceResponse
import com.billfolder.android.data.dto.UpdateIncomeSourceRequest
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

/**
 * Form de "nova/editar fonte de renda".
 *
 * Modos:
 *  - Create (editingId == null): POST com todos os campos.
 *  - Edit (editingId != null): PATCH parcial. Backend valida cross-field
 *    endDate >= startDate; cuidamos disso na validação local também pra
 *    feedback rápido sem round-trip.
 *
 * Tipos suportados (IncomeOriginType no backend, lowercase no JSON):
 *  work / rent / investment / freelance / gift / other
 */
data class AddIncomeSourceFormState(
    val origin: String = "",
    val originType: String = "work",
    val defaultAmount: String = "",
    val expectedDay: String = "",
    val startDate: String = LocalDate.now().toString(),
    val endDate: String? = null,

    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedSuccessfully: Boolean = false,

    val editingId: String? = null,
)

@HiltViewModel
class AddIncomeSourceViewModel @Inject constructor(
    private val incomeRepository: IncomeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AddIncomeSourceFormState())
    val state: StateFlow<AddIncomeSourceFormState> = _state.asStateFlow()

    /**
     * Reseta o form pros valores iniciais. Chamado pelo sheet toda vez
     * que abre (via LaunchedEffect(Unit)) porque o hiltViewModel() é
     * compartilhado entre aberturas — sem esse reset, savedSuccessfully
     * = true da submissão anterior faria o sheet fechar imediatamente
     * na 2ª abertura antes do user interagir.
     */
    fun resetForm() {
        _state.value = AddIncomeSourceFormState()
    }

    fun onOriginChange(value: String) = _state.update { it.copy(origin = value) }
    fun onOriginTypeChange(value: String) = _state.update { it.copy(originType = value) }
    fun onDefaultAmountChange(value: String) = _state.update { it.copy(defaultAmount = value) }
    fun onExpectedDayChange(value: String) = _state.update { it.copy(expectedDay = value) }
    fun onStartDateChange(iso: String) = _state.update { it.copy(startDate = iso) }
    fun onEndDateChange(iso: String?) = _state.update { it.copy(endDate = iso) }

    /**
     * Preenche o form com uma fonte existente — modo edit. Idempotente
     * (checa editingId pra não resetar campos editados num recompose).
     */
    fun prefill(source: IncomeSourceResponse) {
        if (_state.value.editingId == source.id) return
        _state.update {
            it.copy(
                editingId = source.id,
                origin = source.origin,
                originType = source.originType,
                defaultAmount = source.defaultAmount.toBrlInputString(),
                expectedDay = source.expectedDay.toString(),
                startDate = source.startDate,
                endDate = source.endDate,
                errorMessage = null,
            )
        }
    }

    fun submit(
        originEmptyMessage: String,
        amountInvalidMessage: String,
        expectedDayInvalidMessage: String,
        endBeforeStartMessage: String,
    ) {
        val current = _state.value
        val parsedAmount = parseAmount(current.defaultAmount)
        val parsedDay = current.expectedDay.toIntOrNull()

        val validationError = when {
            current.origin.isBlank()                       -> originEmptyMessage
            parsedAmount == null || parsedAmount <= 0      -> amountInvalidMessage
            parsedDay == null || parsedDay !in 1..31       -> expectedDayInvalidMessage
            isEndBeforeStart(current.startDate, current.endDate) -> endBeforeStartMessage
            else                                            -> null
        }
        if (validationError != null) {
            _state.update { it.copy(errorMessage = validationError) }
            return
        }

        _state.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                if (current.editingId != null) {
                    val request = UpdateIncomeSourceRequest(
                        origin = current.origin.trim(),
                        originType = current.originType,
                        defaultAmount = parsedAmount,
                        expectedDay = parsedDay,
                        startDate = current.startDate,
                        endDate = current.endDate,
                    )
                    incomeRepository.updateSource(current.editingId, request)
                } else {
                    val request = CreateIncomeSourceRequest(
                        origin = current.origin.trim(),
                        originType = current.originType,
                        defaultAmount = parsedAmount!!,
                        expectedDay = parsedDay!!,
                        startDate = current.startDate,
                        endDate = current.endDate,
                    )
                    incomeRepository.createSource(request)
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

    /**
     * Valida cross-field localmente — backend também valida, mas damos
     * feedback rápido sem round-trip. Strings ISO comparam direto
     * lexicograficamente.
     */
    private fun isEndBeforeStart(start: String, end: String?): Boolean {
        if (end.isNullOrBlank()) return false
        return end < start
    }

    private fun parseAmount(input: String): Double? =
        input.replace(',', '.').toDoubleOrNull()

    private fun Double.toBrlInputString(): String =
        "%.2f".format(this).replace('.', ',')
}
