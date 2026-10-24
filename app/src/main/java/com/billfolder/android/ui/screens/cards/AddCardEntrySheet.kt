package com.billfolder.android.ui.screens.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.billfolder.android.R
import com.billfolder.android.data.dto.CardEntryResponse
import com.billfolder.android.ui.components.BillFolderDateField
import com.billfolder.android.ui.components.BillFolderDropdown
import com.billfolder.android.ui.components.BillFolderMoneyField
import com.billfolder.android.ui.components.BillFolderPrimaryButton
import com.billfolder.android.ui.components.BillFolderTextField
import com.billfolder.android.ui.components.BillFolderTransactionSheet
import com.billfolder.android.ui.components.DropdownOption
import com.billfolder.android.ui.util.formatBrl

/**
 * Sheet de "nova/editar compra no cartão". Em modo edit (existing != null):
 *  - Título "editar compra...", CTA "atualizar".
 *  - Campos cartão/data/valor/parcelas ficam DISABLED — backend só aceita
 *    label/categoria/notes no PATCH (ver UpdateCardEntryRequest). Pra
 *    mudar valor/parcelas, user deleta e recria.
 *  - Hint texto explica a limitação visualmente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardEntrySheet(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    existing: CardEntryResponse? = null,
    viewModel: AddCardEntryViewModel = hiltViewModel(),
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

    // Reset proativo ao fechar — evita race na 2ª abertura onde o
    // LaunchedEffect(state.savedSuccessfully) veria o true stuck do
    // submit anterior antes do LaunchedEffect(Unit) resetar. Com o
    // onDispose, o state fica limpo no VM assim que o sheet sai da
    // composição.
    DisposableEffect(Unit) {
        onDispose { viewModel.resetForm() }
    }

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

    val isEditing = state.editingId != null
    val title = stringResource(
        when {
            isEditing && state.isRefund -> R.string.add_card_entry_title_edit_refund
            isEditing -> R.string.add_card_entry_title_edit
            state.isRefund -> R.string.add_card_entry_title_refund
            else -> R.string.add_card_entry_title
        },
    )
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

        // Compra ou estorno — só ao criar; editar não muda o valor.
        if (!isEditing) {
            val kinds = listOf(
                false to stringResource(R.string.add_card_entry_kind_purchase),
                true to stringResource(R.string.add_card_entry_kind_refund),
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                kinds.forEachIndexed { index, (refund, label) ->
                    SegmentedButton(
                        selected = state.isRefund == refund,
                        onClick = { viewModel.onRefundChange(refund) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = kinds.size),
                        enabled = !state.isSaving,
                    ) {
                        Text(label)
                    }
                }
            }
            if (state.isRefund) {
                Text(
                    text = stringResource(R.string.add_card_entry_refund_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Aviso explicando os campos lockados em modo edit. Mostrado uma
        // vez no topo do form pra não poluir cada campo individualmente.
        if (isEditing) {
            Text(
                text = stringResource(R.string.add_card_entry_edit_locked_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        BillFolderDropdown(
            label = stringResource(R.string.add_card_entry_field_card),
            selectedLabel = selectedCardLabel,
            options = cardOptions,
            onSelect = viewModel::onCardChange,
            // Disabled em modo edit (backend não aceita mudar cartão).
            enabled = !state.isSaving && cardOptions.isNotEmpty() && !isEditing,
        )

        BillFolderDateField(
            isoDate = state.purchaseDate,
            onIsoDateChange = viewModel::onPurchaseDateChange,
            label = stringResource(R.string.add_card_entry_field_purchase_date),
            enabled = !state.isSaving && !isEditing,
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
            label = stringResource(
                if (state.isRefund) {
                    R.string.add_card_entry_field_refund_amount
                } else {
                    R.string.add_card_entry_field_total_amount
                },
            ),
            enabled = !state.isSaving && !isEditing,
            imeAction = ImeAction.Next,
        )

        // Toggle "assinatura mensal" — só em modo create. Quando ON, a compra
        // vira um template de recorrência (1x/mês) e o campo de parcelas some.
        // Estorno não vira assinatura.
        if (!isEditing && !state.isRefund) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.add_card_entry_field_repeat_monthly),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.add_card_entry_field_repeat_monthly_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.repeatMonthly,
                    onCheckedChange = viewModel::onRepeatMonthlyChange,
                    enabled = !state.isSaving,
                )
            }
        }

        // Parcelas: escondido em assinatura e estorno (sempre 1x). Também
        // disabled em modo edit (backend não recalcula installments).
        if (!state.repeatMonthly && !state.isRefund) {
            OutlinedTextField(
                value = state.installmentsCount,
                onValueChange = viewModel::onInstallmentsChange,
                label = { Text(stringResource(R.string.add_card_entry_field_installments)) },
                singleLine = true,
                enabled = !state.isSaving && !isEditing,
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
        }

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
