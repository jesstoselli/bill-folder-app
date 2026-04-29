package com.billfolder.android.ui.screens.cards

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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.billfolder.android.R
import com.billfolder.android.data.dto.CreditCardAccountResponse
import com.billfolder.android.ui.components.BillFolderPrimaryButton
import com.billfolder.android.ui.components.BillFolderTextField
import com.billfolder.android.ui.components.BillFolderTransactionSheet

/**
 * Sheet de "novo/editar cartão". Diferente do AddCardEntrySheet (que
 * registra uma compra), este cria/atualiza a entidade CreditCardAccount.
 *
 * Modos:
 *  - Create (existing == null): POST com todos os campos.
 *  - Edit (existing != null): PATCH parcial. Todos os campos editáveis,
 *    mas exibimos hint avisando que mudar fechamento/vencimento afeta
 *    apenas lançamentos futuros (statements/installments existentes
 *    ficam intactos — backend não recalcula).
 */
@Composable
fun AddCreditCardSheet(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    existing: CreditCardAccountResponse? = null,
    viewModel: AddCreditCardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(existing) {
        if (existing != null) {
            viewModel.prefill(existing)
        }
    }

    val nameEmpty = stringResource(R.string.add_card_validation_name)
    val closingInvalid = stringResource(R.string.add_card_validation_closing_day)
    val dueInvalid = stringResource(R.string.add_card_validation_due_day)

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) {
            onSaved()
            onDismiss()
        }
    }

    val isEditing = state.editingId != null
    val title = if (isEditing) {
        stringResource(R.string.add_card_title_edit)
    } else {
        stringResource(R.string.add_card_title)
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
                        nameEmptyMessage = nameEmpty,
                        closingInvalidMessage = closingInvalid,
                        dueInvalidMessage = dueInvalid,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        BillFolderTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            label = stringResource(R.string.add_card_field_name),
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Text,
            enabled = !state.isSaving,
        )

        BillFolderTextField(
            value = state.issuerBank,
            onValueChange = viewModel::onIssuerBankChange,
            label = stringResource(R.string.add_card_field_issuer_bank),
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Text,
            enabled = !state.isSaving,
        )

        BillFolderTextField(
            value = state.brand,
            onValueChange = viewModel::onBrandChange,
            label = stringResource(R.string.add_card_field_brand),
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Text,
            enabled = !state.isSaving,
        )

        // Os 2 campos de dia ficam lado a lado — economiza altura no sheet
        // e reforça a relação semântica (mesmo cartão, 2 datas-chave).
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DayField(
                value = state.closingDay,
                onValueChange = viewModel::onClosingDayChange,
                label = stringResource(R.string.add_card_field_closing_day),
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f),
            )
            DayField(
                value = state.dueDay,
                onValueChange = viewModel::onDueDayChange,
                label = stringResource(R.string.add_card_field_due_day),
                enabled = !state.isSaving,
                imeAction = ImeAction.Done,
                modifier = Modifier.weight(1f),
            )
        }

        // Em modo edit, avisa o user que mudar fechamento/vencimento não
        // mexe em statements/compras já existentes — só novos lançamentos
        // usam os dias atualizados.
        if (isEditing) {
            Text(
                text = stringResource(R.string.add_card_edit_days_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DayField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            // Aceita só dígitos, evita vírgula/ponto/símbolo no campo de dia
            if (input.all { it.isDigit() } && input.length <= 2) {
                onValueChange(input)
            }
        },
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction,
            capitalization = KeyboardCapitalization.None,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = modifier,
    )
}
