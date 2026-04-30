package com.billfolder.android.ui.screens.managesavings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.billfolder.android.R
import com.billfolder.android.data.dto.SavingsAccountResponse
import com.billfolder.android.ui.components.BillFolderDropdown
import com.billfolder.android.ui.components.BillFolderMoneyField
import com.billfolder.android.ui.components.BillFolderPrimaryButton
import com.billfolder.android.ui.components.BillFolderTextField
import com.billfolder.android.ui.components.BillFolderTransactionSheet
import com.billfolder.android.ui.components.DropdownOption

/**
 * Sheet de "nova/editar poupança". Cria/atualiza a entidade SavingsAccount.
 *
 * Modos:
 *  - Create (existing == null): POST com todos os campos. Dropdown de
 *    checking habilitado (e pré-seleciona a primary).
 *  - Edit (existing != null): PATCH parcial. Dropdown de checking
 *    DESABILITADO (vínculo é imutável no PATCH); banco/agência/número/
 *    saldo inicial editáveis. Hint avisa o user.
 *
 * Validações 1:1 com o backend (BillFolder.Application/Validators):
 *  - bankName: obrigatório, max 100
 *  - branch: obrigatório, max 20
 *  - accountNumber: obrigatório, max 30
 *  - initialBalance: >= 0
 *
 * O backend retorna 409 "checking_already_has_savings" se a checking
 * selecionada já tiver poupança — VM intercepta e mostra mensagem em PT.
 */
@Composable
fun AddSavingsAccountSheet(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    existing: SavingsAccountResponse? = null,
    viewModel: AddSavingsAccountViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(existing) {
        if (existing != null) {
            viewModel.prefill(existing)
        }
    }

    val checkingEmpty = stringResource(R.string.add_savings_validation_checking_empty)
    val bankNameEmpty = stringResource(R.string.add_savings_validation_bank_name)
    val branchEmpty = stringResource(R.string.add_savings_validation_branch)
    val accountNumberEmpty = stringResource(R.string.add_savings_validation_account_number)
    val initialBalanceInvalid = stringResource(R.string.add_savings_validation_initial_balance)
    val duplicateChecking = stringResource(R.string.add_savings_error_duplicate_checking)

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) {
            onSaved()
            onDismiss()
        }
    }

    val isEditing = state.editingId != null
    val title = if (isEditing) {
        stringResource(R.string.add_savings_title_edit)
    } else {
        stringResource(R.string.add_savings_title)
    }
    val ctaText = if (isEditing) {
        stringResource(R.string.sheet_update_cta)
    } else {
        stringResource(R.string.sheet_save_cta)
    }

    BillFolderTransactionSheet(
        title = title,
        onDismiss = onDismiss,
        isSaving = state.isSaving,
        errorMessage = state.errorMessage,
        footer = {
            BillFolderPrimaryButton(
                text = ctaText,
                onClick = {
                    viewModel.submit(
                        checkingEmptyMessage = checkingEmpty,
                        bankNameEmptyMessage = bankNameEmpty,
                        branchEmptyMessage = branchEmpty,
                        accountNumberEmptyMessage = accountNumberEmpty,
                        initialBalanceInvalidMessage = initialBalanceInvalid,
                        duplicateCheckingMessage = duplicateChecking,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoadingReferences,
            )
        },
    ) {
        // Dropdown de checking — uma linha por checking, identificada por
        // bankName + accountNumber (pra distinguir contas no mesmo banco).
        val checkingOptions = state.checkingAccounts.map { checking ->
            val accountSuffix = checking.accountNumber?.takeIf { it.isNotBlank() }
                ?.let { " · $it" }
                .orEmpty()
            DropdownOption(
                label = "${checking.bankName}$accountSuffix",
                value = checking.id,
            )
        }
        val selectedCheckingLabel = state.checkingAccounts
            .firstOrNull { it.id == state.checkingAccountId }
            ?.let { c ->
                val suffix = c.accountNumber?.takeIf { it.isNotBlank() }
                    ?.let { " · $it" }
                    .orEmpty()
                "${c.bankName}$suffix"
            }
            ?: ""

        BillFolderDropdown(
            label = stringResource(R.string.add_savings_field_checking),
            selectedLabel = selectedCheckingLabel,
            options = checkingOptions,
            onSelect = viewModel::onCheckingChange,
            // Em modo edit, vínculo é imutável → dropdown desabilitado.
            // Em modo create, libera quando os checkings já carregaram.
            enabled = !state.isSaving && !isEditing && checkingOptions.isNotEmpty(),
        )

        if (isEditing) {
            // Reforça visualmente que o vínculo está fixo. Sem isso, dá pra
            // confundir um dropdown disabled com "campo bloqueado por bug".
            Text(
                text = stringResource(R.string.add_savings_edit_checking_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        BillFolderTextField(
            value = state.bankName,
            onValueChange = viewModel::onBankNameChange,
            label = stringResource(R.string.add_savings_field_bank_name),
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Text,
            enabled = !state.isSaving,
        )

        BillFolderTextField(
            value = state.branch,
            onValueChange = viewModel::onBranchChange,
            label = stringResource(R.string.add_savings_field_branch),
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Text,
            enabled = !state.isSaving,
        )

        BillFolderTextField(
            value = state.accountNumber,
            onValueChange = viewModel::onAccountNumberChange,
            label = stringResource(R.string.add_savings_field_account_number),
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Text,
            enabled = !state.isSaving,
        )

        BillFolderMoneyField(
            value = state.initialBalance,
            onValueChange = viewModel::onInitialBalanceChange,
            label = stringResource(R.string.add_savings_field_initial_balance),
            enabled = !state.isSaving,
            imeAction = ImeAction.Done,
        )
    }
}
