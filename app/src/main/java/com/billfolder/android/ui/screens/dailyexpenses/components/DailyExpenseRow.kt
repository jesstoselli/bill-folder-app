package com.billfolder.android.ui.screens.dailyexpenses.components

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
import com.billfolder.android.data.dto.DailyExpenseResponse
import com.billfolder.android.ui.theme.MoneyRow
import com.billfolder.android.ui.util.formatBrl

/**
 * Linha da lista de daily expenses.
 *
 * Layout:
 *   [label]            [valor]
 *   [categoria · conta]
 *
 * Sem chip de status — daily expenses são sempre "concluídas" no momento
 * que são lançadas (não tem pending/paid). Subtítulo combina categoria
 * + conta de origem pra contexto sem ocupar muito espaço.
 */
@Composable
fun DailyExpenseRow(
    expense: DailyExpenseResponse,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
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
                    text = "${expense.categoryName} · ${expense.accountName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = formatBrl(expense.amount),
                style = MoneyRow,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
