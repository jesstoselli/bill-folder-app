package com.billfolder.android.ui.screens.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.CreateIncomeEntryRequest
import com.billfolder.android.data.dto.IncomeEntryResponse
import com.billfolder.android.data.dto.IncomeSourceResponse
import com.billfolder.android.data.dto.UpdateIncomeEntryRequest
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
 * Form de "novo/editar recebimento".
 *
 * Modos:
 *  - Create (editingId == null): POST.
 *  - Edit (editingId != null): PATCH com UpdateIncomeEntryRequest mandando
 *    apenas os campos editáveis pelo user (sourceId, expectedAmount,
 *    expectedDate, notes). Não toca em status/actualAmount/actualDate —
 *    essas viraram caminho do ConfirmIncomeSheet (confirm-received).
 */
data class AddIncomeEntryFormState(
    val expectedDate: String = LocalDate.now().toString(),
    val amount: String = "",
    val selectedSourceId: String? = null, // null = avulso
    val notes: String = "",

    val sources: List<IncomeSourceResponse> = emptyList(),
    val isLoadingReferences: Boolean = true,

    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedSuccessfully: Boolean = false,

    val editingId: String? = null,
)

@HiltViewModel
class AddIncomeEntryViewModel @Inject constructor(
    private val incomeRepository: IncomeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AddIncomeEntryFormState())
    val state: StateFlow<AddIncomeEntryFormState> = _state.asStateFlow()

    init {
        loadSources()
    }

    /**
     * Reseta o form pros valores iniciais. Chamado pelo sheet toda vez
     * que abre (via LaunchedEffect(Unit)) porque o hiltViewModel() é
     * compartilhado entre aberturas — sem esse reset, savedSuccessfully
     * = true da submissão anterior faria o sheet fechar imediatamente
     * na 2ª abertura antes do user interagir.
     *
     * Preserva sources/isLoadingReferences porque o init só roda uma vez
     * — se resetássemos, o dropdown ficaria vazio sem forma de recarregar.
     */
    fun resetForm() {
        val current = _state.value
        _state.value = AddIncomeEntryFormState(
            sources = current.sources,
            isLoadingReferences = current.isLoadingReferences,
        )
    }

    fun onExpectedDateChange(iso: String) = _state.update { it.copy(expectedDate = iso) }
    fun onAmountChange(value: String) = _state.update { it.copy(amount = value) }
    fun onSourceChange(id: String?) = _state.update { it.copy(selectedSourceId = id) }
    fun onNotesChange(value: String) = _state.update { it.copy(notes = value) }

    /**
     * Preenche o form com uma entry existente — modo edit. Idempotente
     * (checa editingId pra não sobrescrever campos já editados pelo user
     * num recompose acidental do sheet).
     */
    fun prefill(item: IncomeEntryResponse) {
        if (_state.value.editingId == item.id) return
        _state.update {
            it.copy(
                editingId = item.id,
                expectedDate = item.expectedDate,
                amount = item.expectedAmount.toBrlInputString(),
                selectedSourceId = item.sourceId,
                notes = item.notes.orEmpty(),
                errorMessage = null,
            )
        }
    }

    fun submit(amountInvalidMessage: String) {
        val current = _state.value
        val parsedAmount = parseAmount(current.amount)
        if (parsedAmount == null || parsedAmount <= 0) {
            _state.update { it.copy(errorMessage = amountInvalidMessage) }
            return
        }

        _state.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                if (current.editingId != null) {
                    // PATCH — só os campos editáveis. Não mandamos status/
                    // actualAmount/actualDate aqui pra não interferir no
                    // confirm-received flow do ConfirmIncomeSheet.
                    val request = UpdateIncomeEntryRequest(
                        sourceId = current.selectedSourceId,
                        expectedAmount = parsedAmount,
                        expectedDate = current.expectedDate,
                        notes = current.notes.trim(),
                    )
                    incomeRepository.updateEntry(current.editingId, request)
                } else {
                    val request = CreateIncomeEntryRequest(
                        sourceId = current.selectedSourceId,
                        expectedAmount = parsedAmount,
                        expectedDate = current.expectedDate,
                        notes = current.notes.takeIf { it.isNotBlank() }?.trim(),
                    )
                    incomeRepository.createEntry(request)
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

    private fun loadSources() {
        viewModelScope.launch {
            try {
                val sources = incomeRepository.listSources().filter { it.isActive }
                _state.update { it.copy(sources = sources, isLoadingReferences = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoadingReferences = false,
                        errorMessage = "Falha ao carregar fontes.",
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
