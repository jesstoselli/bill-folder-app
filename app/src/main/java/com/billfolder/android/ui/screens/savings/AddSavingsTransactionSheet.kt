package com.billfolder.android.ui.screens.savings

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
import com.billfolder.android.data.dto.SavingsTransactionResponse
import com.billfolder.android.data.dto.SavingsTransactionTypes
import com.billfolder.android.ui.components.BillFolderDateField
import com.billfolder.android.ui.components.BillFolderDropdown
import com.billfolder.android.ui.components.BillFolderMoneyField
import com.billfolder.android.ui.components.BillFolderPrimaryButton
import com.billfolder.android.ui.components.BillFolderTextField
import com.billfolder.android.ui.components.BillFolderTransactionSheet
import com.billfolder.android.ui.components.DropdownOption

/**
 * Sheet de "nova/editar movimentação de poupança".
 *
 * Modos:
 *  - Create (existing == null): POST. Dropdown de poupança e tipo
 *    habilitados. Se preferredAccountId for passado (ex: usuário tocou
 *    FAB com uma poupança específica selecionada no carousel), pré-
 *    seleciona ela; senão fica na primeira da lista.
 *  - Edit (existing != null): PATCH. Dropdown de poupança DESABILITADO
 *    (vínculo imutável). Dropdown de tipo desabilitado se a tx é
 *    Transfer* (não editável pela UI em Fase B); habilitado se é
 *    Deposit/Withdrawal/Yield (user pode alternar entre os 3).
 *
 * Os 3 tipos oferecidos no create — Deposit, Withdrawal, Yield —
 * recebem labels em PT no dropdown. Se a tx em edição for Transfer*,
 * o tipo correspondente também aparece no dropdown como opção única
 * (pra renderizar o estado atual sem permitir troca).
 */
@Composable
fun AddSavingsTransactionSheet(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    existing: SavingsTransactionResponse? = null,
    preferredAccountId: String? = null,
    viewModel: AddSavingsTransactionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(existing) {
        if (existing != null) {
            viewModel.prefill(existing)
        }
    }

    LaunchedEffect(preferredAccountId, state.isLoadingReferences) {
        // Só pré-seleciona em create, e só depois que as poupanças
        // já carregaram (senão a chamada se perde).
        if (existing == null && preferredAccountId != null && !state.isLoadingReferences) {
            viewModel.prefillForCreate(preferredAccountId)
        }
    }

    val accountEmpty = stringResource(R.string.add_savings_tx_validation_account_empty)
    val amountInvalid = stringResource(R.string.add_savings_tx_validation_amount)

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) {
            onSaved()
            onDismiss()
        }
    }

    val isEditing = state.editingId != null
    val title = if (isEditing) {
        stringResource(R.string.add_savings_tx_title_edit)
    } else {
        stringResource(R.string.add_savings_tx_title)
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
                        accountEmptyMessage = accountEmpty,
                        amountInvalidMessage = amountInvalid,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoadingReferences,
            )
        },
    ) {
        // ----- Dropdown de poupança -----
        val accountOptions = state.accounts.map { account ->
            val suffix = account.accountNumber.takeIf { it.isNotBlank() }
                ?.let { " · $it" }
                .orEmpty()
            DropdownOption(label = "${account.bankName}$suffix", value = account.id)
        }
        val selectedAccountLabel = state.accounts
            .firstOrNull { it.id == state.savingsAccountId }
            ?.let { acc ->
                val suffix = acc.accountNumber.takeIf { it.isNotBlank() }
                    ?.let { " · $it" }
                    .orEmpty()
                "${acc.bankName}$suffix"
            }
            ?: ""

        BillFolderDropdown(
            label = stringResource(R.string.add_savings_tx_field_account),
            selectedLabel = selectedAccountLabel,
            options = accountOptions,
            onSelect = viewModel::onAccountChange,
            // Disabled em edit (backend não aceita mudar savingsAccountId).
            enabled = !state.isSaving && accountOptions.isNotEmpty() && !isEditing,
        )

        if (isEditing) {
            Text(
                text = stringResource(R.string.add_savings_tx_edit_account_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ----- Dropdown de tipo -----
        // Em create, lista os 3 tipos editáveis pelo user. Em edit:
        //   - se a tx é Deposit/Withdrawal/Yield: lista os 3 (user pode trocar)
        //   - se é Transfer*: mostra só o tipo atual + dropdown disabled
        val typeOptions = if (state.typeLocked) {
            listOf(DropdownOption(label = transactionTypeLabel(state.type), value = state.type))
        } else {
            SavingsTransactionTypes.CREATABLE_BY_USER.map { type ->
                DropdownOption(label = transactionTypeLabel(type), value = type)
            }
        }
        val selectedTypeLabel = transactionTypeLabel(state.type)

        BillFolderDropdown(
            label = stringResource(R.string.add_savings_tx_field_type),
            selectedLabel = selectedTypeLabel,
            options = typeOptions,
            onSelect = viewModel::onTypeChange,
            enabled = !state.isSaving && !state.typeLocked,
        )

        if (state.typeLocked) {
            Text(
                text = stringResource(R.string.add_savings_tx_edit_transfer_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ----- Data -----
        BillFolderDateField(
            isoDate = state.date,
            onIsoDateChange = viewModel::onDateChange,
            label = stringResource(R.string.add_savings_tx_field_date),
            enabled = !state.isSaving,
        )

        // ----- Valor -----
        BillFolderMoneyField(
            value = state.amount,
            onValueChange = viewModel::onAmountChange,
            label = stringResource(R.string.add_savings_tx_field_amount),
            enabled = !state.isSaving,
            imeAction = ImeAction.Next,
        )

        // ----- Label opcional -----
        BillFolderTextField(
            value = state.label,
            onValueChange = viewModel::onLabelChange,
            label = stringResource(R.string.add_savings_tx_field_label),
            imeAction = ImeAction.Done,
            keyboardType = KeyboardType.Text,
            enabled = !state.isSaving,
        )
    }
}

/**
 * Mapeia o type string do backend pro label localizado em PT. Reusa as
 * mesmas strings da SavingsTransactionRow (string resources são
 * compartilhados — não duplica copy).
 */
@Composable
private fun transactionTypeLabel(type: String): String = when (type) {
    SavingsTransactionTypes.DEPOSIT      -> stringResource(R.string.savings_transaction_type_deposit)
    SavingsTransactionTypes.WITHDRAWAL   -> stringResource(R.string.savings_transaction_type_withdrawal)
    SavingsTransactionTypes.YIELD        -> stringResource(R.string.savings_transaction_type_yield)
    SavingsTransactionTypes.TRANSFER_OUT -> stringResource(R.string.savings_transaction_type_transfer_out)
    SavingsTransactionTypes.TRANSFER_IN  -> stringResource(R.string.savings_transaction_type_transfer_in)
    else                                 -> type
}
