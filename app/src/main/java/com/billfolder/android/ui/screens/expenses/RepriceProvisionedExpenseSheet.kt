package com.billfolder.android.ui.screens.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.billfolder.android.R
import com.billfolder.android.data.dto.ExpenseResponse
import com.billfolder.android.ui.components.BillFolderMoneyField
import com.billfolder.android.ui.components.BillFolderPrimaryButton
import com.billfolder.android.ui.components.BillFolderTransactionSheet
import com.billfolder.android.ui.components.RecurrenceScopeDialog
import com.billfolder.android.ui.components.repriceLiteral

/**
 * Sheet de "reajustar o valor por sessão" de uma despesa provisionada
 * (ex: terapia semanal subiu de R$150 pra R$175). Fluxo: digita o novo
 * valor por sessão → CTA abre o RecurrenceScopeDialog ("só esta" vs "esta
 * e as próximas") → escolhido o escopo, submete com o literal camelCase
 * (repriceLiteral). O total do mês (expectedAmount) recalcula no backend.
 *
 * Recebe a `expense` resolvida (caller já tem dela na lista) pra pré-preencher
 * o valor atual por sessão (occurrenceAmount).
 */
@Composable
fun RepriceProvisionedExpenseSheet(
    expense: ExpenseResponse,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    viewModel: RepriceProvisionedExpenseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showScopeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.resetForm()
        viewModel.initializeFor(
            expenseId = expense.id,
            currentAmount = expense.occurrenceAmount ?: 0.0,
        )
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.resetForm() }
    }

    val amountInvalid = stringResource(R.string.add_daily_validation_amount_invalid)

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) {
            onSaved()
            onDismiss()
        }
    }

    BillFolderTransactionSheet(
        title = stringResource(R.string.reprice_provisioned_title),
        onDismiss = onDismiss,
        isSaving = state.isSaving,
        errorMessage = state.errorMessage,
        footer = {
            BillFolderPrimaryButton(
                text = stringResource(R.string.reprice_provisioned_cta),
                onClick = { showScopeDialog = true },
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        RepriceProvisionedSummaryCard(expense = expense)

        BillFolderMoneyField(
            value = state.amount,
            onValueChange = viewModel::onAmountChange,
            label = stringResource(R.string.reprice_provisioned_field_amount),
            enabled = !state.isSaving,
            imeAction = ImeAction.Done,
        )
    }

    // Modal de escopo — só aparece após o CTA. A escolha resolve o literal
    // camelCase (repriceLiteral) e dispara o submit.
    if (showScopeDialog) {
        RecurrenceScopeDialog(
            title = stringResource(R.string.reprice_scope_title),
            message = stringResource(R.string.reprice_scope_message, expense.label),
            onScopeChosen = { choice ->
                showScopeDialog = false
                viewModel.submit(
                    scope = choice.repriceLiteral(),
                    amountInvalidMessage = amountInvalid,
                )
            },
            onDismiss = { showScopeDialog = false },
        )
    }
}

@Composable
private fun RepriceProvisionedSummaryCard(expense: ExpenseResponse) {
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
            text = stringResource(R.string.reprice_provisioned_summary_format, expense.label),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
