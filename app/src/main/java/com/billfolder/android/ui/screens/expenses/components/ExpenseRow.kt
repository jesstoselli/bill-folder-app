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
import androidx.compose.ui.unit.dp
import com.billfolder.android.data.dto.ExpenseResponse
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
                Text(
                    text = expense.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${expense.categoryName} · ${formatShortDate(expense.dueDate)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    // Pra paid, mostramos actualAmount (se houver). Pra outras,
                    // o expectedAmount.
                    text = formatBrl(
                        if (expense.status.equals("paid", ignoreCase = true)) {
                            expense.actualAmount ?: expense.expectedAmount
                        } else {
                            expense.expectedAmount
                        },
                    ),
                    style = MoneyRow,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                StatusChip(status = expense.status)
            }
        }
    }
}
