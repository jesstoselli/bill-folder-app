package com.billfolder.android.ui.screens.income

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.billfolder.android.R
import com.billfolder.android.data.dto.IncomeEntryResponse
import com.billfolder.android.ui.components.BillFolderDateField
import com.billfolder.android.ui.components.BillFolderMoneyField
import com.billfolder.android.ui.components.BillFolderPrimaryButton
import com.billfolder.android.ui.components.BillFolderTransactionSheet
import com.billfolder.android.ui.theme.MoneyRow
import com.billfolder.android.ui.util.formatBrl
import com.billfolder.android.ui.util.formatShortDate

/**
 * Sheet de "confirmar recebimento". Análogo ao PayExpenseSheet:
 * recebe a entry resolvida (caller passa do response da lista) e pré-preenche
 * actualAmount com expectedAmount. Header com card resumo da entry.
 */
@Composable
fun ConfirmIncomeSheet(
    entry: IncomeEntryResponse,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ConfirmIncomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(entry.id) {
        viewModel.initializeFor(entry.id, entry.expectedAmount)
    }

    val amountInvalid = stringResource(R.string.add_daily_validation_amount_invalid)

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) {
            onSaved()
            onDismiss()
        }
    }

    BillFolderTransactionSheet(
        title = stringResource(R.string.confirm_income_title),
        onDismiss = onDismiss,
        isSaving = state.isSaving,
        errorMessage = state.errorMessage,
        footer = {
            BillFolderPrimaryButton(
                text = stringResource(R.string.confirm_income_cta),
                onClick = { viewModel.submit(amountInvalid) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        EntrySummaryCard(entry = entry)

        BillFolderDateField(
            isoDate = state.actualDate,
            onIsoDateChange = viewModel::onActualDateChange,
            label = stringResource(R.string.confirm_income_field_actual_date),
            enabled = !state.isSaving,
        )

        BillFolderMoneyField(
            value = state.actualAmount,
            onValueChange = viewModel::onActualAmountChange,
            label = stringResource(R.string.confirm_income_field_actual_amount),
            enabled = !state.isSaving,
            imeAction = ImeAction.Done,
        )
    }
}

@Composable
private fun EntrySummaryCard(entry: IncomeEntryResponse) {
    val title = entry.sourceOrigin
        ?: stringResource(R.string.income_entry_one_off)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.confirm_income_summary_format,
                formatBrl(entry.expectedAmount),
                formatShortDate(entry.expectedDate),
            ),
            style = MoneyRow,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
