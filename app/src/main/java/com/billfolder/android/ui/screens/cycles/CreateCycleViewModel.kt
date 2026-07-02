package com.billfolder.android.ui.screens.cycles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.CreateCycleRequest
import com.billfolder.android.data.repository.CyclesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

/**
 * Form de "criar ciclo". MVP: cobre só create — edit/delete de ciclos
 * fica pra quando ganharmos tela dedicada de gerenciamento.
 *
 * Defaults sensatos pro mês corrente:
 *  - startDate: dia 1
 *  - endDate: último dia do mês
 *  - label: "<mês>/<ano>" em PT-BR (ex: "abril/2026")
 *
 * O user pode editar tudo livremente — defaults são só pra reduzir
 * atrito no caso comum (criar o "ciclo do mês" como começa o app).
 */
data class CreateCycleFormState(
    val startDate: String = defaultStartDate(),
    val endDate: String = defaultEndDate(),
    val label: String = defaultLabel(),

    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedSuccessfully: Boolean = false,
)

@HiltViewModel
class CreateCycleViewModel @Inject constructor(
    private val cyclesRepository: CyclesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateCycleFormState())
    val state: StateFlow<CreateCycleFormState> = _state.asStateFlow()

    /**
     * Reseta o form pros valores iniciais. Chamado pelo sheet toda vez
     * que abre (via LaunchedEffect(Unit)) porque o hiltViewModel() é
     * compartilhado entre aberturas — sem esse reset, savedSuccessfully
     * = true da submissão anterior faria o sheet fechar imediatamente
     * na 2ª abertura antes do user interagir.
     *
     * Bônus: instanciar CreateCycleFormState() de novo re-executa os
     * defaultXxx() do mês corrente — se o user reabrir num mês diferente
     * ganha datas/label atualizados.
     */
    fun resetForm() {
        _state.value = CreateCycleFormState()
    }

    fun onStartDateChange(iso: String) = _state.update { it.copy(startDate = iso) }
    fun onEndDateChange(iso: String) = _state.update { it.copy(endDate = iso) }
    fun onLabelChange(value: String) = _state.update { it.copy(label = value) }

    fun submit(
        labelEmptyMessage: String,
        endBeforeStartMessage: String,
        duplicateStartMessage: String,
    ) {
        val current = _state.value

        // Strings ISO "yyyy-MM-dd" comparam direto lexicograficamente.
        // Mesma convenção dos outros validators cross-field do app.
        val validationError = when {
            current.label.isBlank()              -> labelEmptyMessage
            current.endDate <= current.startDate -> endBeforeStartMessage
            else                                  -> null
        }
        if (validationError != null) {
            _state.update { it.copy(errorMessage = validationError) }
            return
        }

        _state.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                cyclesRepository.create(
                    CreateCycleRequest(
                        startDate = current.startDate,
                        endDate = current.endDate,
                        label = current.label.trim(),
                    ),
                )
                _state.update { it.copy(isSaving = false, savedSuccessfully = true) }
            } catch (e: HttpException) {
                // 409 → duplicate_start_date (ciclo do user com a mesma
                // startDate). Sem parser de error body por enquanto;
                // mensagem traduzida por código.
                val message = if (e.code() == 409) {
                    duplicateStartMessage
                } else {
                    "Erro do servidor (HTTP ${e.code()})."
                }
                _state.update { it.copy(isSaving = false, errorMessage = message) }
            } catch (e: IOException) {
                _state.update { it.copy(isSaving = false, errorMessage = "Sem conexão. Tenta de novo.") }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, errorMessage = e.message ?: "Algo deu errado.") }
            }
        }
    }
}

// ----- Helpers de default ----------------------------------------------------

private fun defaultStartDate(): String =
    LocalDate.now().withDayOfMonth(1).toString()

private fun defaultEndDate(): String {
    val today = LocalDate.now()
    return today.withDayOfMonth(today.lengthOfMonth()).toString()
}

/**
 * "abril/2026" — minúsculo igual a convenção do label que aparece na
 * Home (ex: cycle.label exibido no CycleNavigator é minúsculo).
 */
private fun defaultLabel(): String {
    val locale = Locale.Builder().setLanguage("pt").setRegion("BR").build()
    val now = LocalDate.now()
    val monthName = now.month.getDisplayName(TextStyle.FULL, locale).lowercase(locale)
    return "$monthName/${now.year}"
}
