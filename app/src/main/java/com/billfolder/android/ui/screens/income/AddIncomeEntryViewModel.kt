package com.billfolder.android.ui.screens.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.CreateIncomeEntryRequest
import com.billfolder.android.data.dto.IncomeSourceResponse
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

    fun onExpectedDateChange(iso: String) = _state.update { it.copy(expectedDate = iso) }
    fun onAmountChange(value: String) = _state.update { it.copy(amount = value) }
    fun onSourceChange(id: String?) = _state.update { it.copy(selectedSourceId = id) }
    fun onNotesChange(value: String) = _state.update { it.copy(notes = value) }

    fun submit(amountInvalidMessage: String) {
        val current = _state.value
        val parsedAmount = parseAmount(current.amount)
        if (parsedAmount == null || parsedAmount <= 0) {
            _state.update { it.copy(errorMessage = amountInvalidMessage) }
            return
        }

        val request = CreateIncomeEntryRequest(
            sourceId = current.selectedSourceId,
            expectedAmount = parsedAmount,
            expectedDate = current.expectedDate,
            notes = current.notes.takeIf { it.isNotBlank() }?.trim(),
        )

        _state.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                incomeRepository.createEntry(request)
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
}
