package com.billfolder.android.ui.screens.cards

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
import com.billfolder.android.ui.util.formatBrl

@Composable
fun AddCardEntrySheet(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddCardEntryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val labelEmpty = stringResource(R.string.add_daily_validation_label_empty)
    val amountInvalid = stringResource(R.string.add_daily_validation_amount_invalid)
    val cardEmpty = stringResource(R.string.add_card_entry_validation_card)
    val installmentsInvalid = stringResource(R.string.add_card_entry_validation_installments)
    val categoryEmpty = stringResource(R.string.add_daily_validation_category_empty)

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) {
            onSaved()
            onDismiss()
        }
    }

    BillFolderTransactionSheet(
        title = stringResource(R.string.add_card_entry_title),
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
                        cardEmptyMessage = cardEmpty,
                        installmentsInvalidMessage = installmentsInvalid,
                        categoryEmptyMessage = categoryEmpty,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoadingReferences,
            )
        },
    ) {
        val cardOptions = state.cards.map {
            DropdownOption(label = it.name, value = it.id)
        }
        val selectedCardLabel = state.cards
            .firstOrNull { it.id == state.selectedCardId }
            ?.name
            ?: ""

        val categoryOptions = state.categories.map {
            DropdownOption(label = it.namePt, value = it.id)
        }
        val selectedCategoryLabel = state.categories
            .firstOrNull { it.id == state.selectedCategoryId }
            ?.namePt
            ?: ""

        BillFolderDropdown(
            label = stringResource(R.string.add_card_entry_field_card),
            selectedLabel = selectedCardLabel,
            options = cardOptions,
            onSelect = viewModel::onCardChange,
            enabled = !state.isSaving && cardOptions.isNotEmpty(),
        )

        BillFolderDateField(
            isoDate = state.purchaseDate,
            onIsoDateChange = viewModel::onPurchaseDateChange,
            label = stringResource(R.string.add_card_entry_field_purchase_date),
            enabled = !state.isSaving,
        )

        BillFolderTextField(
            value = state.label,
            onValueChange = viewModel::onLabelChange,
            label = stringResource(R.string.add_card_entry_field_label),
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Text,
            enabled = !state.isSaving,
        )

        BillFolderMoneyField(
            value = state.totalAmount,
            onValueChange = viewModel::onTotalAmountChange,
            label = stringResource(R.string.add_card_entry_field_total_amount),
            enabled = !state.isSaving,
            imeAction = ImeAction.Next,
        )

        OutlinedTextField(
            value = state.installmentsCount,
            onValueChange = viewModel::onInstallmentsChange,
            label = { Text(stringResource(R.string.add_card_entry_field_installments)) },
            singleLine = true,
            enabled = !state.isSaving,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
            ),
            supportingText = {
                // Preview do parcelamento: "6x R$ 116,65"
                val previewText = computeInstallmentPreview(
                    totalAmount = state.totalAmount,
                    count = state.installmentsCount,
                )
                if (previewText != null) {
                    Text(previewText)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        BillFolderDropdown(
            label = stringResource(R.string.add_card_entry_field_category),
            selectedLabel = selectedCategoryLabel,
            options = categoryOptions,
            onSelect = viewModel::onCategoryChange,
            enabled = !state.isSaving && categoryOptions.isNotEmpty(),
        )

        OutlinedTextField(
            value = state.notes,
            onValueChange = viewModel::onNotesChange,
            label = { Text(stringResource(R.string.add_card_entry_field_notes)) },
            minLines = 2,
            maxLines = 4,
            enabled = !state.isSaving,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun computeInstallmentPreview(totalAmount: String, count: String): String? {
    val total = totalAmount.replace(',', '.').toDoubleOrNull() ?: return null
    val n = count.toIntOrNull() ?: return null
    if (total <= 0 || n < 1) return null
    val perInstallment = total / n
    return stringResource(
        R.string.add_card_entry_installments_preview_format,
        n,
        formatBrl(perInstallment),
    )
}
