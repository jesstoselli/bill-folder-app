package com.billfolder.android.ui.screens.cards

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
import com.billfolder.android.data.dto.CardEntryResponse
import com.billfolder.android.ui.components.BillFolderMoneyField
import com.billfolder.android.ui.components.BillFolderPrimaryButton
import com.billfolder.android.ui.components.BillFolderTransactionSheet
import com.billfolder.android.ui.components.RecurrenceScopeDialog
import com.billfolder.android.ui.components.repriceLiteral

/**
 * Sheet de "reprecificar" uma assinatura de cartão (novo valor mensal).
 * Fluxo: digita o novo valor → CTA abre o RecurrenceScopeDialog ("só esta"
 * vs "esta e as próximas") → escolhido o escopo, submete com o literal
 * camelCase (repriceLiteral).
 *
 * Recebe a `entry` resolvida (caller já tem dela na lista) pra pré-preencher
 * o valor atual (totalAmount — numa assinatura installmentsCount=1, então
 * totalAmount é o valor da mensalidade).
 */
@Composable
fun RepriceSubscriptionSheet(
    entry: CardEntryResponse,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    viewModel: RepriceSubscriptionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showScopeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.resetForm()
        viewModel.initializeFor(entryId = entry.id, currentAmount = entry.totalAmount)
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
        title = stringResource(R.string.reprice_subscription_title),
        onDismiss = onDismiss,
        isSaving = state.isSaving,
        errorMessage = state.errorMessage,
        footer = {
            BillFolderPrimaryButton(
                text = stringResource(R.string.reprice_subscription_cta),
                onClick = { showScopeDialog = true },
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        RepriceSummaryCard(entry = entry)

        BillFolderMoneyField(
            value = state.amount,
            onValueChange = viewModel::onAmountChange,
            label = stringResource(R.string.reprice_subscription_field_amount),
            enabled = !state.isSaving,
            imeAction = ImeAction.Done,
        )
    }

    // Modal de escopo — só aparece após o CTA. A escolha resolve o literal
    // camelCase (repriceLiteral) e dispara o submit.
    if (showScopeDialog) {
        RecurrenceScopeDialog(
            title = stringResource(R.string.reprice_scope_title),
            message = stringResource(R.string.reprice_scope_message, entry.label),
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
private fun RepriceSummaryCard(entry: CardEntryResponse) {
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
            text = stringResource(R.string.reprice_subscription_summary_format, entry.label),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
