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
import com.billfolder.android.ui.components.BillFolderPrimaryButton
import com.billfolder.android.ui.components.BillFolderTextField
import com.billfolder.android.ui.components.BillFolderTransactionSheet

/**
 * Sheet de cadastrar novo cartão. Diferente do AddCardEntrySheet
 * (que registra uma compra), este cria a entidade CreditCardAccount —
 * é o equivalente a "Add Bank Account" do tela de Manage.
 */
@Composable
fun AddCreditCardSheet(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddCreditCardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val nameEmpty = stringResource(R.string.add_card_validation_name)
    val closingInvalid = stringResource(R.string.add_card_validation_closing_day)
    val dueInvalid = stringResource(R.string.add_card_validation_due_day)

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) {
            onSaved()
            onDismiss()
        }
    }

    BillFolderTransactionSheet(
        title = stringResource(R.string.add_card_title),
        onDismiss = onDismiss,
        isSaving = state.isSaving,
        errorMessage = state.errorMessage,
        footer = {
            BillFolderPrimaryButton(
                text = stringResource(R.string.sheet_save_cta),
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
