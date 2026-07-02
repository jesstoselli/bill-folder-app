package com.billfolder.android.ui.screens.managesavings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billfolder.android.data.dto.CheckingAccountResponse
import com.billfolder.android.data.dto.CreateSavingsAccountRequest
import com.billfolder.android.data.dto.SavingsAccountResponse
import com.billfolder.android.data.dto.UpdateSavingsAccountRequest
import com.billfolder.android.data.repository.ReferenceDataRepository
import com.billfolder.android.data.repository.SavingsRepository
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
 * Form de "nova/editar conta poupança". Carrega checkings disponíveis pra
 * popular o dropdown de vínculo (1:1 com checking).
 *
 * Modos:
 *  - Create (editingId == null): POST. Dropdown de checking habilitado;
 *    backend valida 1:1 e retorna 409 "checking_already_has_savings" se
 *    a checking selecionada já tiver uma poupança — mensagem traduzida
 *    pra português no submit().
 *  - Edit (editingId != null): PATCH parcial. Dropdown de checking
 *    DESABILITADO (vínculo é imutável no PATCH); só bankName/branch/
 *    accountNumber/initialBalance são editáveis.
 *
 * Observação sobre initialBalance: o caller pode editar mesmo no modo
 * edit. Backend trata isso como rebase do baseline pro cálculo de saldo
 * — útil pra corrigir um valor digitado errado no momento do cadastro.
 * Na Fase B, quando tivermos transações, podemos exibir um aviso de
 * "essa edição vai recalcular o saldo atual" no momento do submit.
 */
data class AddSavingsAccountFormState(
    val checkingAccountId: String? = null,
    val bankName: String = "",
    val branch: String = "",
    val accountNumber: String = "",
    val initialBalance: String = "",

    val checkingAccounts: List<CheckingAccountResponse> = emptyList(),
    val isLoadingReferences: Boolean = true,

    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedSuccessfully: Boolean = false,

    val editingId: String? = null,
)

@HiltViewModel
class AddSavingsAccountViewModel @Inject constructor(
    private val referenceDataRepository: ReferenceDataRepository,
    private val savingsRepository: SavingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AddSavingsAccountFormState())
    val state: StateFlow<AddSavingsAccountFormState> = _state.asStateFlow()

    init {
        loadCheckings()
    }

    /**
     * Reseta o form pros valores iniciais. Chamado pelo sheet toda vez
     * que abre (via LaunchedEffect(Unit)) porque o hiltViewModel() é
     * compartilhado entre aberturas — sem esse reset, savedSuccessfully
     * = true da submissão anterior faria o sheet fechar imediatamente
     * na 2ª abertura antes do user interagir.
     *
     * Preserva checkingAccounts/isLoadingReferences porque o init só roda
     * uma vez — se resetássemos, o dropdown ficaria vazio sem forma de
     * recarregar. Também restaura a pré-seleção da primary (mesma lógica
     * do loadCheckings).
     */
    fun resetForm() {
        val current = _state.value
        val defaultChecking = current.checkingAccounts.firstOrNull { it.isPrimary }
            ?: current.checkingAccounts.firstOrNull()
        _state.value = AddSavingsAccountFormState(
            checkingAccounts = current.checkingAccounts,
            isLoadingReferences = current.isLoadingReferences,
            checkingAccountId = defaultChecking?.id,
            // Auto-fill dos dados que a UI escondeu — herda da checking
            // pré-selecionada. O user brasileiro típico usa mesmo banco/
            // agência/número da conta pra a poupança vinculada; o sheet
            // simplifica pra 2 campos (checking + saldo inicial) e o VM
            // preserva contrato do backend enviando os 3 campos.
            bankName = defaultChecking?.bankName.orEmpty(),
            branch = defaultChecking?.branch.orEmpty(),
            accountNumber = defaultChecking?.accountNumber.orEmpty(),
        )
    }

    /**
     * Trocar a checking selecionada refresca os 3 campos herdados
     * (bankName/branch/accountNumber) com os dados da nova checking. A
     * sheet não expõe esses campos visualmente, mas o backend ainda
     * espera receber; herdar automaticamente cobre 99% dos casos.
     */
    fun onCheckingChange(id: String) = _state.update { current ->
        val checking = current.checkingAccounts.firstOrNull { it.id == id }
        current.copy(
            checkingAccountId = id,
            bankName = checking?.bankName ?: current.bankName,
            branch = checking?.branch.orEmpty(),
            accountNumber = checking?.accountNumber.orEmpty(),
        )
    }

    fun onInitialBalanceChange(value: String) = _state.update { it.copy(initialBalance = value) }

    /**
     * Preenche o form com uma poupança existente — modo edit. Idempotente
     * (checa editingId pra não resetar campos que o user já modificou
     * num recompose acidental do sheet).
     */
    fun prefill(account: SavingsAccountResponse) {
        if (_state.value.editingId == account.id) return
        _state.update {
            it.copy(
                editingId = account.id,
                checkingAccountId = account.checkingAccountId,
                bankName = account.bankName,
                branch = account.branch,
                accountNumber = account.accountNumber,
                initialBalance = account.initialBalance.toBrlInputString(),
                errorMessage = null,
            )
        }
    }

    fun submit(
        checkingEmptyMessage: String,
        bankNameEmptyMessage: String,
        branchEmptyMessage: String,
        accountNumberEmptyMessage: String,
        initialBalanceInvalidMessage: String,
        duplicateCheckingMessage: String,
    ) {
        val current = _state.value
        val balance = parseAmount(current.initialBalance)
        val isEditing = current.editingId != null

        val validationError = when {
            // checking só é obrigatório/validado no modo create — em edit, é imutável
            !isEditing && current.checkingAccountId.isNullOrBlank() -> checkingEmptyMessage
            current.bankName.isBlank()                              -> bankNameEmptyMessage
            current.branch.isBlank()                                -> branchEmptyMessage
            current.accountNumber.isBlank()                         -> accountNumberEmptyMessage
            balance == null || balance < 0                          -> initialBalanceInvalidMessage
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
                    // PATCH parcial. Mandamos os campos editáveis pelo user;
                    // checkingAccountId é deliberadamente omitido — backend
                    // não permite alterar o vínculo via PATCH.
                    val request = UpdateSavingsAccountRequest(
                        bankName = current.bankName.trim(),
                        branch = current.branch.trim(),
                        accountNumber = current.accountNumber.trim(),
                        initialBalance = balance,
                    )
                    savingsRepository.updateAccount(current.editingId!!, request)
                } else {
                    val request = CreateSavingsAccountRequest(
                        checkingAccountId = current.checkingAccountId!!,
                        bankName = current.bankName.trim(),
                        branch = current.branch.trim(),
                        accountNumber = current.accountNumber.trim(),
                        initialBalance = balance!!,
                    )
                    savingsRepository.createAccount(request)
                }
                _state.update { it.copy(isSaving = false, savedSuccessfully = true) }
            } catch (e: HttpException) {
                // 409 → checking_already_has_savings (vínculo 1:1 violado).
                // Não temos parser do body de erro do backend ainda; mensagem
                // genérica por código pra UX previsível. Quando isso virar
                // dor (mais codes traduzidos), centraliza num ApiErrorMapper.
                val message = if (e.code() == 409) {
                    duplicateCheckingMessage
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

    private fun loadCheckings() {
        viewModelScope.launch {
            try {
                val checkings = referenceDataRepository.getCheckingAccounts()
                val defaultChecking = checkings.firstOrNull { c -> c.isPrimary }
                    ?: checkings.firstOrNull()

                _state.update {
                    // Pré-seleciona a primary só em modo create. Em edit, o
                    // prefill já setou checkingAccountId + os 3 herdados
                    // do próprio SavingsAccountResponse.
                    val shouldAutoFill = it.checkingAccountId == null
                    it.copy(
                        checkingAccounts = checkings,
                        isLoadingReferences = false,
                        checkingAccountId = it.checkingAccountId ?: defaultChecking?.id,
                        bankName = if (shouldAutoFill) defaultChecking?.bankName.orEmpty() else it.bankName,
                        branch = if (shouldAutoFill) defaultChecking?.branch.orEmpty() else it.branch,
                        accountNumber = if (shouldAutoFill) defaultChecking?.accountNumber.orEmpty() else it.accountNumber,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoadingReferences = false,
                        errorMessage = "Falha ao carregar contas correntes.",
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
