package com.billfolder.android.ui.screens.adjustments

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.billfolder.android.R
import com.billfolder.android.data.dto.CycleAdjustmentResponse
import com.billfolder.android.data.dto.CycleAdjustmentTypes
import com.billfolder.android.ui.components.BillFolderDateField
import com.billfolder.android.ui.components.BillFolderDropdown
import com.billfolder.android.ui.components.BillFolderMoneyField
import com.billfolder.android.ui.components.BillFolderPrimaryButton
import com.billfolder.android.ui.components.BillFolderTextField
import com.billfolder.android.ui.components.BillFolderTransactionSheet
import com.billfolder.android.ui.components.DropdownOption

/**
 * Sheet de "novo/editar ajuste do ciclo".
 *
 * Campos: type (Entrada/Saída), label livre, valor, data.
 * Fase 1: sem linkagem a saque de poupança (`sourceSavingsTransactionId`).
 */
@Composable
fun AddAdjustmentSheet(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    existing: CycleAdjustmentResponse? = null,
    viewModel: AddAdjustmentViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.resetForm()
        if (existing != null) {
            viewModel.prefill(existing)
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.resetForm() }
    }

    val labelEmpty = stringResource(R.string.add_adjustment_validation_label)
    val amountInvalid = stringResource(R.string.add_adjustment_validation_amount)

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) {
            onSaved()
            onDismiss()
        }
    }

    val isEditing = state.editingId != null
    val title = if (isEditing) {
        stringResource(R.string.add_adjustment_title_edit)
    } else {
        stringResource(R.string.add_adjustment_title)
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
                        labelEmptyMessage = labelEmpty,
                        amountInvalidMessage = amountInvalid,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving,
            )
        },
    ) {
        // Dropdown de tipo
        val inflowLabel = stringResource(R.string.add_adjustment_type_inflow)
        val outflowLabel = stringResource(R.string.add_adjustment_type_outflow)
        val typeOptions = listOf(
            DropdownOption(label = inflowLabel, value = CycleAdjustmentTypes.INFLOW),
            DropdownOption(label = outflowLabel, value = CycleAdjustmentTypes.OUTFLOW),
        )
        val selectedTypeLabel = if (state.type == CycleAdjustmentTypes.INFLOW) inflowLabel else outflowLabel

        BillFolderDropdown(
            label = stringResource(R.string.add_adjustment_field_type),
            selectedLabel = selectedTypeLabel,
            options = typeOptions,
            onSelect = viewModel::onTypeChange,
            enabled = !state.isSaving,
        )

        BillFolderDateField(
            isoDate = state.date,
            onIsoDateChange = viewModel::onDateChange,
            label = stringResource(R.string.add_adjustment_field_date),
            enabled = !state.isSaving,
        )

        BillFolderTextField(
            value = state.label,
            onValueChange = viewModel::onLabelChange,
            label = stringResource(R.string.add_adjustment_field_label),
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Text,
            enabled = !state.isSaving,
        )

        BillFolderMoneyField(
            value = state.amount,
            onValueChange = viewModel::onAmountChange,
            label = stringResource(R.string.add_adjustment_field_amount),
            enabled = !state.isSaving,
            imeAction = ImeAction.Done,
        )
    }
}
