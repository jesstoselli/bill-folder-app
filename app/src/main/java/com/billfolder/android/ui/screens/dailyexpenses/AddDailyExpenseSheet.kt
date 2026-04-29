package com.billfolder.android.ui.screens.dailyexpenses

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.billfolder.android.R
import com.billfolder.android.ui.components.BillFolderDateField
import com.billfolder.android.ui.components.BillFolderDropdown
import com.billfolder.android.ui.components.BillFolderMoneyField
import com.billfolder.android.ui.components.BillFolderPrimaryButton
import com.billfolder.android.ui.components.BillFolderTextField
import com.billfolder.android.ui.components.BillFolderTransactionSheet
import com.billfolder.android.ui.components.DropdownOption

/**
 * Sheet de "nova despesa avulsa". Usa o BillFolderTransactionSheet
 * genérico — todo o boilerplate de bottom sheet (drag handle, header,
 * dismiss) vive lá. Aqui só o conteúdo específico do form.
 *
 * Fluxo:
 *  - Usuário preenche os campos
 *  - Clica "adicionar" → ViewModel valida + POST
 *  - Backend confirma → savedSuccessfully=true → caller fecha sheet
 *    e dispara refresh da lista
 */
@Composable
fun AddDailyExpenseSheet(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddDailyExpenseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Mensagens de validação resolvidas no escopo composable pra passar
    // pro VM, que não tem acesso a Context.
    val labelEmpty = stringResource(R.string.add_daily_validation_label_empty)
    val amountInvalid = stringResource(R.string.add_daily_validation_amount_invalid)
    val categoryEmpty = stringResource(R.string.add_daily_validation_category_empty)
    val accountEmpty = stringResource(R.string.add_daily_validation_account_empty)

    // Quando o backend confirma, fecha o sheet e dispara refresh.
    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) {
            onSaved()
            onDismiss()
        }
    }

    BillFolderTransactionSheet(
        title = stringResource(R.string.add_daily_title),
        onDismiss = onDismiss,
        isSaving = state.isSaving,
        errorMessage = state.errorMessage,
        footer = {
            BillFolderPrimaryButton(
                text = stringResource(R.string.sheet_save_cta),
                onClick = {
                    viewModel.submit(
                        labelEmptyMessage = labelEmpty,
                        amountInvalidMessage = amountInvalid,
                        categoryEmptyMessage = categoryEmpty,
                        accountEmptyMessage = accountEmpty,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoadingReferences,
            )
        },
    ) {
        FormContent(
            state = state,
            viewModel = viewModel,
        )
    }
}

@Composable
private fun FormContent(
    state: AddDailyExpenseFormState,
    viewModel: AddDailyExpenseViewModel,
) {
    val categoryOptions = state.categories.map {
        DropdownOption(label = it.namePt, value = it.id)
    }
    val accountOptions = state.accounts.map {
        DropdownOption(
            label = listOfNotNull(it.bankName, it.accountNumber).joinToString(" · "),
            value = it.id,
        )
    }

    val selectedCategoryLabel = state.categories
        .firstOrNull { it.id == state.selectedCategoryId }
        ?.namePt
        ?: ""

    val selectedAccountLabel = state.accounts
        .firstOrNull { it.id == state.selectedAccountId }
        ?.let { listOfNotNull(it.bankName, it.accountNumber).joinToString(" · ") }
        ?: ""

    BillFolderDateField(
        isoDate = state.date,
        onIsoDateChange = viewModel::onDateChange,
        label = stringResource(R.string.add_daily_field_date),
        enabled = !state.isSaving,
    )

    BillFolderTextField(
        value = state.label,
        onValueChange = viewModel::onLabelChange,
        label = stringResource(R.string.add_daily_field_label),
        imeAction = ImeAction.Next,
        keyboardType = KeyboardType.Text,
        enabled = !state.isSaving,
    )

    BillFolderMoneyField(
        value = state.amount,
        onValueChange = viewModel::onAmountChange,
        label = stringResource(R.string.add_daily_field_amount),
        enabled = !state.isSaving,
        imeAction = ImeAction.Next,
    )

    BillFolderDropdown(
        label = stringResource(R.string.add_daily_field_category),
        selectedLabel = selectedCategoryLabel,
        options = categoryOptions,
        onSelect = viewModel::onCategoryChange,
        enabled = !state.isSaving && categoryOptions.isNotEmpty(),
    )

    BillFolderDropdown(
        label = stringResource(R.string.add_daily_field_account),
        selectedLabel = selectedAccountLabel,
        options = accountOptions,
        onSelect = viewModel::onAccountChange,
        enabled = !state.isSaving && accountOptions.isNotEmpty(),
    )

    OutlinedTextField(
        value = state.notes,
        onValueChange = viewModel::onNotesChange,
        label = { Text(stringResource(R.string.add_daily_field_notes)) },
        minLines = 2,
        maxLines = 4,
        enabled = !state.isSaving,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
