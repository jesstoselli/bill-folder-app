package com.billfolder.android.ui.screens.income.components

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
import com.billfolder.android.data.dto.IncomeEntryResponse
import com.billfolder.android.ui.screens.home.components.StatusChip
import com.billfolder.android.ui.theme.MoneyRow
import com.billfolder.android.ui.util.formatBrl
import com.billfolder.android.ui.util.formatShortDate

/**
 * Linha de IncomeEntry. Click só ativo pra entries pendentes (expected/late);
 * received já foi recebido, tap não faz nada.
 *
 * Subtítulo: nome da fonte recorrente (se houver) ou "avulso" pra one-off.
 */
@Composable
fun IncomeEntryRow(
    entry: IncomeEntryResponse,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val subtitle = entry.sourceOrigin
        ?: stringResource(R.string.income_entry_one_off)

    val amount = if (entry.status.equals("received", ignoreCase = true)) {
        entry.actualAmount ?: entry.expectedAmount
    } else {
        entry.expectedAmount
    }
    val date = entry.actualDate ?: entry.expectedDate

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
                    text = subtitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = formatShortDate(date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatBrl(amount),
                    style = MoneyRow,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                StatusChip(status = entry.status)
            }
        }
    }
}
