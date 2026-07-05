package com.billfolder.android.ui.screens.adjustments.components

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
import com.billfolder.android.data.dto.CycleAdjustmentResponse
import com.billfolder.android.data.dto.CycleAdjustmentTypes
import com.billfolder.android.ui.theme.MoneyRow
import com.billfolder.android.ui.util.formatBrl
import com.billfolder.android.ui.util.formatShortDate

/**
 * Linha da lista de ajustes do ciclo.
 *
 * Layout:
 *   [label]                        [+ valor]
 *   [entrada|saída · data]
 *
 * Valor é prefixado com "+" ou "−" conforme o tipo, e colorido em verde
 * (inflow) ou vermelho suave (outflow) pra rápida leitura visual.
 */
@Composable
fun AdjustmentRow(
    adjustment: CycleAdjustmentResponse,
    modifier: Modifier = Modifier,
) {
    val isInflow = adjustment.type.equals(CycleAdjustmentTypes.INFLOW, ignoreCase = true)
    val typeLabel = if (isInflow) {
        stringResource(R.string.adjustment_row_type_inflow)
    } else {
        stringResource(R.string.adjustment_row_type_outflow)
    }
    val sign = if (isInflow) "+" else "−"
    val amountColor = if (isInflow) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

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
                    text = adjustment.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$typeLabel · ${formatShortDate(adjustment.date)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "$sign ${formatBrl(adjustment.amount)}",
                style = MoneyRow,
                color = amountColor,
            )
        }
    }
}
