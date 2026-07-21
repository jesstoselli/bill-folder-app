package com.billfolder.android.ui.screens.expenses.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.billfolder.android.R
import com.billfolder.android.data.dto.ExpenseResponse
import com.billfolder.android.ui.screens.expenses.displayAmount
import com.billfolder.android.ui.screens.expenses.isProvisioned
import com.billfolder.android.ui.screens.expenses.isProvisionedInProgress
import com.billfolder.android.ui.screens.home.components.StatusChip
import com.billfolder.android.ui.theme.MoneyRow
import com.billfolder.android.ui.util.formatBrl
import com.billfolder.android.ui.util.formatShortDate

/**
 * Linha da lista de despesas.
 *
 * Layout:
 *   [label]                 [valor]
 *   [categoria · data]     [chip de status]
 *
 * Card.onClick é o gancho pra abrir o sheet de "marcar como pago" — só
 * faz sentido pra rows não-paid; pago já tá pago, tap não faz nada (caller
 * decide passar `null` em onClick pras paid).
 */
@Composable
fun ExpenseRow(
    expense: ExpenseResponse,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        onClick = onClick ?: {},
        enabled = onClick != null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Provisionada: anexa "(pagas/total)" ao label pra o user
                // acompanhar "2/4 pagas" numa olhada.
                val labelText = if (expense.isProvisioned()) {
                    stringResource(
                        R.string.expense_provisioned_progress_format,
                        expense.label,
                        expense.occurrencesPaid,
                        expense.occurrencesTotal ?: 0,
                    )
                } else {
                    expense.label
                }
                Text(
                    text = labelText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${expense.categoryName} · ${formatShortDate(expense.dueDate)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Provisionada em andamento com baixas dadas: o número
                // principal já mostra o reservado que resta, então aqui vai o
                // total cheio do mês como contexto ("R$800 no mês"). Sem baixas,
                // principal == mês cheio, então a linha seria redundante.
                if (expense.isProvisionedInProgress() && expense.occurrencesPaid > 0) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(
                            R.string.expense_provisioned_month_total_format,
                            formatBrl(expense.expectedAmount),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    // Paga → realizado; provisionada em andamento → reservado que
                    // resta; demais → expectedAmount. (ver displayAmount)
                    text = formatBrl(expense.displayAmount()),
                    style = MoneyRow,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                StatusChip(status = expense.status)
            }
        }
    }
}
