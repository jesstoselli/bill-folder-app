package com.billfolder.android.ui.screens.managebanks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.billfolder.android.R
import com.billfolder.android.data.dto.CheckingAccountResponse
import com.billfolder.android.ui.components.BillFolderMoneyField
import com.billfolder.android.ui.components.BillFolderPrimaryButton
import com.billfolder.android.ui.components.BillFolderTextField
import com.billfolder.android.ui.components.BillFolderTransactionSheet

/**
 * Sheet de "nova/editar conta corrente".
 *
 * Modos:
 *  - Create (existing == null): POST com todos os campos.
 *  - Edit (existing != null): PATCH parcial. Todos os campos editáveis.
 *
 * Sobre isPrimary: switch dedicado. Se marcado, backend automaticamente
 * desmarca as outras primaries do user — hint visual explica.
 */
@Composable
fun AddCheckingAccountSheet(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    existing: CheckingAccountResponse? = null,
    viewModel: AddCheckingAccountViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Reset em cada abertura — hiltViewModel() é compartilhado no scope
    // da tela pai. Reset primeiro, depois prefill (se edit).
    LaunchedEffect(Unit) {
        viewModel.resetForm()
        if (existing != null) {
            viewModel.prefill(existing)
        }
    }

    // Reset proativo ao fechar — evita race na 2ª abertura.
    DisposableEffect(Unit) {
        onDispose { viewModel.resetForm() }
    }

    val bankNameEmpty = stringResource(R.string.add_checking_validation_bank_name)
    val branchEmpty = stringResource(R.string.add_checking_validation_branch)
    val accountNumberEmpty = stringResource(R.string.add_checking_validation_account_number)
    val initialBalanceInvalid = stringResource(R.string.add_checking_validation_initial_balance)

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) {
            onSaved()
            onDismiss()
        }
    }

    val isEditing = state.editingId != null
    val title = if (isEditing) {
        stringResource(R.string.add_checking_title_edit)
    } else {
        stringResource(R.string.add_checking_title)
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
                        bankNameEmptyMessage = bankNameEmpty,
                        branchEmptyMessage = branchEmpty,
                        accountNumberEmptyMessage = accountNumberEmpty,
                        initialBalanceInvalidMessage = initialBalanceInvalid,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        BillFolderTextField(
            value = state.bankName,
            onValueChange = viewModel::onBankNameChange,
            label = stringResource(R.string.add_checking_field_bank_name),
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Text,
            enabled = !state.isSaving,
        )

        BillFolderTextField(
            value = state.branch,
            onValueChange = viewModel::onBranchChange,
            label = stringResource(R.string.add_checking_field_branch),
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Text,
            enabled = !state.isSaving,
        )

        BillFolderTextField(
            value = state.accountNumber,
            onValueChange = viewModel::onAccountNumberChange,
            label = stringResource(R.string.add_checking_field_account_number),
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Text,
            enabled = !state.isSaving,
        )

        BillFolderMoneyField(
            value = state.initialBalance,
            onValueChange = viewModel::onInitialBalanceChange,
            label = stringResource(R.string.add_checking_field_initial_balance),
            enabled = !state.isSaving,
            imeAction = ImeAction.Done,
        )

        // Toggle primary — invariante do backend garante max 1 primary
        // por user, então marcar aqui desmarca as outras automaticamente.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.add_checking_field_is_primary),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.add_checking_field_is_primary_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = state.isPrimary,
                onCheckedChange = viewModel::onIsPrimaryChange,
                enabled = !state.isSaving,
            )
        }
    }
}
