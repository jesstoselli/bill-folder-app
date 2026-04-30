package com.billfolder.android.ui.screens.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.CreateSavingsTransactionRequest
import com.billfolder.android.data.dto.SavingsAccountResponse
import com.billfolder.android.data.dto.SavingsTransactionResponse
import com.billfolder.android.data.dto.SavingsTransactionTypes
import com.billfolder.android.data.dto.UpdateSavingsTransactionRequest
import com.billfolder.android.data.repository.SavingsRepository
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
 * Form de "nova/editar movimentação de poupança".
 *
 * Modos:
 *  - Create (editingId == null): POST com todos os campos. Tipo default
 *    é DEPOSIT (mais comum). Dropdown de poupança pré-seleciona a
 *    primeira da lista; usuário troca se quiser registrar em outra.
 *
 *  - Edit (editingId != null): PATCH parcial. savingsAccountId NÃO é
 *    enviado — backend não permite mudar o vínculo da movimentação com
 *    a poupança (ver UpdateSavingsTransactionRequest, que não tem
 *    SavingsAccountId). Sheet desabilita o dropdown de poupança.
 *    Se a tx em edição é uma TransferOut/TransferIn, dropdown de tipo
 *    também fica desabilitado — UI não oferece esses tipos pra create
 *    nem permite trocar via PATCH (linkedTransactionId fora de escopo
 *    da Fase B).
 *
 * Tipos oferecidos no dropdown: SavingsTransactionTypes.CREATABLE_BY_USER
 * (deposit/withdrawal/yield). Transfer* só aparecem se a tx em edição já
 * for desse tipo — pra dar contexto visual sem permitir troca.
 *
 * Validações alinhadas com SavingsTransactionValidators do backend:
 *  - amount >= 0
 *  - date != default
 *  - label max 200 (não imposto local; backend valida)
 */
data class AddSavingsTransactionFormState(
    val savingsAccountId: String? = null,
    val type: String = SavingsTransactionTypes.DEPOSIT,
    val amount: String = "",
    val date: String = LocalDate.now().toString(),
    val label: String = "",

    val accounts: List<SavingsAccountResponse> = emptyList(),
    val isLoadingReferences: Boolean = true,

    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedSuccessfully: Boolean = false,

    val editingId: String? = null,
    /**
     * Quando o tipo da tx em edição NÃO está em CREATABLE_BY_USER (ou
     * seja, é TransferOut/TransferIn), travamos o dropdown de tipo. Esse
     * flag é setado no prefill e checado pela sheet.
     */
    val typeLocked: Boolean = false,
)

@HiltViewModel
class AddSavingsTransactionViewModel @Inject constructor(
    private val savingsRepository: SavingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AddSavingsTransactionFormState())
    val state: StateFlow<AddSavingsTransactionFormState> = _state.asStateFlow()

    init {
        loadAccounts()
    }

    fun onAccountChange(id: String) = _state.update { it.copy(savingsAccountId = id) }
    fun onTypeChange(value: String) = _state.update { it.copy(type = value) }
    fun onAmountChange(value: String) = _state.update { it.copy(amount = value) }
    fun onDateChange(iso: String) = _state.update { it.copy(date = iso) }
    fun onLabelChange(value: String) = _state.update { it.copy(label = value) }

    /**
     * Pré-seleciona uma poupança específica do carousel — usado quando o
     * usuário toca o FAB com uma poupança X selecionada na SavingsScreen.
     * Idempotente (não sobrescreve se a sheet já está em modo edit).
     */
    fun prefillForCreate(savingsAccountId: String) {
        if (_state.value.editingId != null) return
        _state.update { it.copy(savingsAccountId = savingsAccountId) }
    }

    /**
     * Preenche o form com uma transaction existente — modo edit. Idempotente
     * (checa editingId). Se a tx for Transfer*, marca typeLocked = true.
     */
    fun prefill(item: SavingsTransactionResponse) {
        if (_state.value.editingId == item.id) return
        val typeLocked = item.type !in SavingsTransactionTypes.CREATABLE_BY_USER
        _state.update {
            it.copy(
                editingId = item.id,
                savingsAccountId = item.savingsAccountId,
                type = item.type,
                amount = item.amount.toBrlInputString(),
                date = item.date,
                label = item.label.orEmpty(),
                typeLocked = typeLocked,
                errorMessage = null,
            )
        }
    }

    fun submit(
        accountEmptyMessage: String,
        amountInvalidMessage: String,
    ) {
        val current = _state.value
        val parsedAmount = parseAmount(current.amount)
        val isEditing = current.editingId != null

        val validationError = when {
            // accountId só obrigatório/validado em create — em edit, vínculo é fixo
            !isEditing && current.savingsAccountId.isNullOrBlank() -> accountEmptyMessage
            parsedAmount == null || parsedAmount < 0               -> amountInvalidMessage
            else                                                    -> null
        }
        if (validationError != null) {
            _state.update { it.copy(errorMessage = validationError) }
            return
        }

        _state.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                if (isEditing) {
                    // PATCH — não enviamos savingsAccountId (vínculo imutável)
                    // nem linkedTransactionId (out of scope em Fase B).
                    // Label vazia vira null pra "limpar" no backend.
                    val request = UpdateSavingsTransactionRequest(
                        type = current.type,
                        amount = parsedAmount,
                        date = current.date,
                        label = current.label.takeIf { it.isNotBlank() }?.trim(),
                        linkedTransactionId = null,
                    )
                    savingsRepository.updateTransaction(current.editingId!!, request)
                } else {
                    val request = CreateSavingsTransactionRequest(
                        savingsAccountId = current.savingsAccountId!!,
                        type = current.type,
                        amount = parsedAmount!!,
                        date = current.date,
                        label = current.label.takeIf { it.isNotBlank() }?.trim(),
                        linkedTransactionId = null,
                    )
                    savingsRepository.createTransaction(request)
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

    private fun loadAccounts() {
        viewModelScope.launch {
            try {
                val accounts = savingsRepository.listAccounts()
                _state.update {
                    it.copy(
                        accounts = accounts,
                        isLoadingReferences = false,
                        // Pré-seleciona a primeira poupança em modo create. Em edit,
                        // o prefill já setou — respeita o que veio.
                        savingsAccountId = it.savingsAccountId ?: accounts.firstOrNull()?.id,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoadingReferences = false,
                        errorMessage = "Falha ao carregar poupanças.",
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
