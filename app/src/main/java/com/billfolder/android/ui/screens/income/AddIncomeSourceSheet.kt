package com.billfolder.android.ui.screens.income

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.billfolder.android.R
import com.billfolder.android.data.dto.IncomeSourceResponse
import com.billfolder.android.ui.components.BillFolderDateField
import com.billfolder.android.ui.components.BillFolderDropdown
import com.billfolder.android.ui.components.BillFolderMoneyField
import com.billfolder.android.ui.components.BillFolderPrimaryButton
import com.billfolder.android.ui.components.BillFolderTextField
import com.billfolder.android.ui.components.BillFolderTransactionSheet
import com.billfolder.android.ui.components.DropdownOption

/**
 * Sheet de "nova/editar fonte de renda".
 *
 * Modos:
 *  - Create (existing == null): POST.
 *  - Edit (existing != null): PATCH parcial. Todos os campos editáveis;
 *    backend não recalcula entries existentes em mudanças (só novas
 *    entries geradas via recurrence usarão valores atualizados).
 *
 * UX nota: o tipo (work, rent, etc) é dropdown com labels traduzidos
 * pra pt-BR mas o value enviado ao backend continua em inglês lowercase
 * (espelho do enum IncomeOriginType serializado em JSON).
 */
@Composable
fun AddIncomeSourceSheet(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    existing: IncomeSourceResponse? = null,
    viewModel: AddIncomeSourceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Reset em cada abertura — o hiltViewModel() é compartilhado no
    // scope da tela pai, então savedSuccessfully/campos ficam poluídos
    // entre aberturas se não resetarmos. Ordem importa: reset primeiro,
    // depois prefill (se edit) — senão o reset zeraria o prefill.
    LaunchedEffect(Unit) {
        viewModel.resetForm()
        if (existing != null) {
            viewModel.prefill(existing)
        }
    }

    val originEmpty = stringResource(R.string.add_income_source_validation_origin)
    val amountInvalid = stringResource(R.string.add_income_source_validation_amount)
    val expectedDayInvalid = stringResource(R.string.add_income_source_validation_expected_day)
    val endBeforeStart = stringResource(R.string.add_income_source_validation_end_before_start)

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) {
            onSaved()
            onDismiss()
        }
    }

    val isEditing = state.editingId != null
    val title = if (isEditing) {
        stringResource(R.string.add_income_source_title_edit)
    } else {
        stringResource(R.string.add_income_source_title)
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
                        originEmptyMessage = originEmpty,
                        amountInvalidMessage = amountInvalid,
                        expectedDayInvalidMessage = expectedDayInvalid,
                        endBeforeStartMessage = endBeforeStart,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        // Dropdown de tipo. Labels pt-BR; values em inglês lowercase
        // batendo com o enum do backend (IncomeOriginType serializado).
        val typeOptions = listOf(
            DropdownOption(label = stringResource(R.string.income_origin_type_work),       value = "work"),
            DropdownOption(label = stringResource(R.string.income_origin_type_rent),       value = "rent"),
            DropdownOption(label = stringResource(R.string.income_origin_type_investment), value = "investment"),
            DropdownOption(label = stringResource(R.string.income_origin_type_freelance),  value = "freelance"),
            DropdownOption(label = stringResource(R.string.income_origin_type_gift),       value = "gift"),
            DropdownOption(label = stringResource(R.string.income_origin_type_other),      value = "other"),
        )
        val selectedTypeLabel = typeOptions
            .firstOrNull { it.value == state.originType }
            ?.label
            ?: ""

        BillFolderTextField(
            value = state.origin,
            onValueChange = viewModel::onOriginChange,
            label = stringResource(R.string.add_income_source_field_origin),
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Text,
            enabled = !state.isSaving,
        )

        BillFolderDropdown(
            label = stringResource(R.string.add_income_source_field_origin_type),
            selectedLabel = selectedTypeLabel,
            options = typeOptions,
            onSelect = viewModel::onOriginTypeChange,
            enabled = !state.isSaving,
        )

        BillFolderMoneyField(
            value = state.defaultAmount,
            onValueChange = viewModel::onDefaultAmountChange,
            label = stringResource(R.string.add_income_source_field_default_amount),
            enabled = !state.isSaving,
            imeAction = ImeAction.Next,
        )

        // Dia esperado (1..31) — validação no VM.
        OutlinedTextField(
            value = state.expectedDay,
            onValueChange = { input ->
                if (input.all { it.isDigit() } && input.length <= 2) {
                    viewModel.onExpectedDayChange(input)
                }
            },
            label = { Text(stringResource(R.string.add_income_source_field_expected_day)) },
            singleLine = true,
            enabled = !state.isSaving,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        // Datas de início e fim lado a lado — fim é opcional.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BillFolderDateField(
                isoDate = state.startDate,
                onIsoDateChange = viewModel::onStartDateChange,
                label = stringResource(R.string.add_income_source_field_start_date),
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f),
            )
            BillFolderDateField(
                isoDate = state.endDate.orEmpty(),
                onIsoDateChange = { iso ->
                    viewModel.onEndDateChange(iso.takeIf { it.isNotBlank() })
                },
                label = stringResource(R.string.add_income_source_field_end_date),
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
