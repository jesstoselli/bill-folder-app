package com.billfolder.android.ui.screens.adjustments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.CreateCycleAdjustmentRequest
import com.billfolder.android.data.dto.CycleAdjustmentResponse
import com.billfolder.android.data.dto.CycleAdjustmentTypes
import com.billfolder.android.data.dto.UpdateCycleAdjustmentRequest
import com.billfolder.android.data.repository.CycleAdjustmentsRepository
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
 * Form de "novo/editar ajuste do ciclo".
 *
 * Fase 1: não expomos o campo `sourceSavingsTransactionId` (linkagem
 * pra saque de poupança). Fica pra depois quando implementarmos "sacar
 * pro ciclo" como fluxo dedicado.
 *
 * Campos:
 *  - type: "inflow" (entra) ou "outflow" (sai) — dropdown
 *  - label: descrição livre ("venda bicicleta", "presente sogro")
 *  - amount: valor (positivo sempre; o sinal vem do type)
 *  - date: data do ajuste (default hoje)
 *
 * Sheet única serve create + edit: se `existing` != null, chama prefill()
 * e submit vira PATCH.
 */
data class AddAdjustmentFormState(
    val type: String = CycleAdjustmentTypes.INFLOW,
    val label: String = "",
    val amount: String = "",
    val date: String = LocalDate.now().toString(),

    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedSuccessfully: Boolean = false,

    val editingId: String? = null,
)

@HiltViewModel
class AddAdjustmentViewModel @Inject constructor(
    private val adjustmentsRepository: CycleAdjustmentsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AddAdjustmentFormState())
    val state: StateFlow<AddAdjustmentFormState> = _state.asStateFlow()

    /**
     * Reseta o form. Chamado toda vez que o sheet abre — hiltViewModel()
     * é retido entre aberturas, sem reset o savedSuccessfully da submissão
     * anterior faria a sheet fechar imediatamente na 2ª abertura.
     */
    fun resetForm() {
        _state.value = AddAdjustmentFormState()
    }

    fun onTypeChange(value: String)   = _state.update { it.copy(type = value) }
    fun onLabelChange(value: String)  = _state.update { it.copy(label = value) }
    fun onAmountChange(value: String) = _state.update { it.copy(amount = value) }
    fun onDateChange(iso: String)     = _state.update { it.copy(date = iso) }

    /**
     * Prefill em modo edit. Idempotente via check no editingId — recompose
     * não sobrescreve o que user já editou.
     */
    fun prefill(item: CycleAdjustmentResponse) {
        if (_state.value.editingId == item.id) return
        _state.update {
            it.copy(
                editingId = item.id,
                type = item.type,
                label = item.label,
                amount = item.amount.toBrlInputString(),
                date = item.date,
                errorMessage = null,
            )
        }
    }

    fun submit(
        labelEmptyMessage: String,
        amountInvalidMessage: String,
    ) {
        val current = _state.value
        val parsedAmount = parseAmount(current.amount)

        val validationError = when {
            current.label.isBlank()                -> labelEmptyMessage
            parsedAmount == null || parsedAmount <= 0.0 -> amountInvalidMessage
            else                                    -> null
        }
        if (validationError != null) {
            _state.update { it.copy(errorMessage = validationError) }
            return
        }

        _state.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                if (current.editingId != null) {
                    val request = UpdateCycleAdjustmentRequest(
                        type = current.type,
                        label = current.label.trim(),
                        amount = parsedAmount,
                        date = current.date,
                        sourceSavingsTransactionId = null,
                    )
                    adjustmentsRepository.update(current.editingId, request)
                } else {
                    val request = CreateCycleAdjustmentRequest(
                        type = current.type,
                        label = current.label.trim(),
                        amount = parsedAmount!!,
                        date = current.date,
                        sourceSavingsTransactionId = null,
                    )
                    adjustmentsRepository.create(request)
                }
                _state.update { it.copy(isSaving = false, savedSuccessfully = true) }
            } catch (e: HttpException) {
                _state.update {
                    it.copy(isSaving = false, errorMessage = "Erro do servidor (HTTP ${e.code()}).")
                }
            } catch (e: IOException) {
                _state.update { it.copy(isSaving = false, errorMessage = "Sem conexão. Tenta de novo.") }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, errorMessage = e.message ?: "Algo deu errado.") }
            }
        }
    }

    private fun parseAmount(input: String): Double? =
        input.replace(',', '.').toDoubleOrNull()

    private fun Double.toBrlInputString(): String =
        "%.2f".format(this).replace('.', ',')
}
