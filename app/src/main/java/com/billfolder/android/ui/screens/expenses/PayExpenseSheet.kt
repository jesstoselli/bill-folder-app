package com.billfolder.android.ui.screens.expenses

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.navigation.compose.hiltViewModel
import com.billfolder.android.R
import com.billfolder.android.data.dto.ExpenseResponse
import com.billfolder.android.ui.components.BillFolderDateField
import com.billfolder.android.ui.components.BillFolderDropdown
import com.billfolder.android.ui.components.BillFolderMoneyField
import com.billfolder.android.ui.components.BillFolderPrimaryButton
import com.billfolder.android.ui.components.BillFolderTransactionSheet
import com.billfolder.android.ui.components.DropdownOption
import com.billfolder.android.ui.theme.MoneyRow
import com.billfolder.android.ui.util.formatBrl
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import com.billfolder.android.ui.util.formatShortDate

/**
 * Sheet de "marcar despesa como paga". Recebe a `expense` resolvida
 * (caller já tem dela na lista) e usa pra mostrar header informativo
 * + pré-preencher actualAmount.
 *
 * Uso primário do BillFolderTransactionSheet com header customizado:
 * o card resumo da despesa fica como primeiro item do `content`,
 * acima dos campos do form.
 */
@Composable
fun PayExpenseSheet(
    expense: ExpenseResponse,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    viewModel: PayExpenseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Reset em cada abertura — o hiltViewModel() é compartilhado no
    // scope da tela pai, então savedSuccessfully/campos ficam poluídos
    // entre aberturas se não resetarmos. Ordem importa: reset primeiro,
    // depois initializeFor — senão o reset zeraria o expenseId/actualAmount.
    LaunchedEffect(Unit) {
        viewModel.resetForm()
        viewModel.initializeFor(expense.id, expense.expectedAmount)
    }

    val amountInvalid = stringResource(R.string.add_daily_validation_amount_invalid)

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) {
            onSaved()
            onDismiss()
        }
    }

    BillFolderTransactionSheet(
        title = stringResource(R.string.pay_expense_title),
        onDismiss = onDismiss,
        isSaving = state.isSaving,
        errorMessage = state.errorMessage,
        footer = {
            BillFolderPrimaryButton(
                text = stringResource(R.string.pay_expense_cta),
                onClick = { viewModel.submit(amountInvalid) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoadingReferences,
            )
        },
    ) {
        ExpenseSummaryCard(expense = expense)

        BillFolderDateField(
            isoDate = state.paidDate,
            onIsoDateChange = viewModel::onPaidDateChange,
            label = stringResource(R.string.pay_expense_field_paid_date),
            enabled = !state.isSaving,
        )

        BillFolderMoneyField(
            value = state.actualAmount,
            onValueChange = viewModel::onActualAmountChange,
            label = stringResource(R.string.pay_expense_field_actual_amount),
            enabled = !state.isSaving,
            imeAction = ImeAction.Next,
        )

        val accountOptions = state.accounts.map {
            DropdownOption(
                label = listOfNotNull(it.bankName, it.accountNumber).joinToString(" · "),
                value = it.id,
            )
        }
        val selectedAccountLabel = state.accounts
            .firstOrNull { it.id == state.selectedAccountId }
            ?.let { listOfNotNull(it.bankName, it.accountNumber).joinToString(" · ") }
            ?: ""

        BillFolderDropdown(
            label = stringResource(R.string.pay_expense_field_account),
            selectedLabel = selectedAccountLabel,
            options = accountOptions,
            onSelect = viewModel::onAccountChange,
            enabled = !state.isSaving && accountOptions.isNotEmpty(),
        )
    }
}

/**
 * Card pequeno no topo do sheet com o resumo da despesa (label + valor
 * esperado + data de vencimento). Dá contexto sem precisar fechar e
 * voltar pra lista.
 */
@Composable
private fun ExpenseSummaryCard(expense: ExpenseResponse) {
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
            text = expense.label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.pay_expense_summary_format,
                formatBrl(expense.expectedAmount),
                formatShortDate(expense.dueDate),
            ),
            style = MoneyRow,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
