package com.billfolder.android.ui.screens.income

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
import com.billfolder.android.ui.components.BillFolderTransactionSheet
import com.billfolder.android.ui.components.DropdownOption

@Composable
fun AddIncomeEntrySheet(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddIncomeEntryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val amountInvalid = stringResource(R.string.add_daily_validation_amount_invalid)

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) {
            onSaved()
            onDismiss()
        }
    }

    BillFolderTransactionSheet(
        title = stringResource(R.string.add_income_title),
        onDismiss = onDismiss,
        isSaving = state.isSaving,
        errorMessage = state.errorMessage,
        footer = {
            BillFolderPrimaryButton(
                text = stringResource(R.string.sheet_save_cta),
                onClick = { viewModel.submit(amountInvalid) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoadingReferences,
            )
        },
    ) {
        // Sources dropdown — primeiro item é "avulso" (null), depois as
        // sources ativas. UX: pré-selecionado em "avulso" porque é o caso
        // mais comum no formulário (recorrentes geralmente já têm entries
        // geradas pelo backend via recurrence).
        //
        // Tipo explícito necessário em todos os DropdownOption: o primeiro
        // tem value=null (DropdownOption<String?>), os outros têm
        // value=String. Sem o <String?> explícito, Kotlin infere
        // List<DropdownOption<out String?>> e o site BillFolderDropdown<String?>
        // recusa por variance.
        val noneLabel = stringResource(R.string.add_income_source_none)
        val sourceOptions: List<DropdownOption<String?>> = buildList {
            add(DropdownOption<String?>(label = noneLabel, value = null))
            state.sources.forEach { src ->
                add(DropdownOption<String?>(label = src.origin, value = src.id))
            }
        }
        val selectedSourceLabel = state.sources
            .firstOrNull { it.id == state.selectedSourceId }
            ?.origin
            ?: noneLabel

        BillFolderDateField(
            isoDate = state.expectedDate,
            onIsoDateChange = viewModel::onExpectedDateChange,
            label = stringResource(R.string.add_income_field_expected_date),
            enabled = !state.isSaving,
        )

        BillFolderMoneyField(
            value = state.amount,
            onValueChange = viewModel::onAmountChange,
            label = stringResource(R.string.add_income_field_amount),
            enabled = !state.isSaving,
            imeAction = ImeAction.Next,
        )

        BillFolderDropdown<String?>(
            label = stringResource(R.string.add_income_field_source),
            selectedLabel = selectedSourceLabel,
            options = sourceOptions,
            onSelect = viewModel::onSourceChange,
            enabled = !state.isSaving,
        )

        OutlinedTextField(
            value = state.notes,
            onValueChange = viewModel::onNotesChange,
            label = { Text(stringResource(R.string.add_income_field_notes)) },
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
}
