package com.billfolder.android.ui.screens.cards.components

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
import com.billfolder.android.data.dto.CardEntryResponse
import com.billfolder.android.ui.theme.MoneyRow
import com.billfolder.android.ui.util.formatBrl
import com.billfolder.android.ui.util.formatShortDate

/**
 * Linha de compra no cartão. Mostra label + categoria · data + valor total.
 * Quando parcelada, indica "(Nx)" ao lado do label pra deixar claro que
 * o valor é o total e vai ser pago em N statements diferentes.
 */
@Composable
fun CardEntryRow(
    entry: CardEntryResponse,
    modifier: Modifier = Modifier,
) {
    val labelWithInstallments = if (entry.installmentsCount > 1) {
        stringResource(R.string.cards_entry_installments_format, entry.label, entry.installmentsCount)
    } else {
        entry.label
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
                    text = labelWithInstallments,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(
                        R.string.cards_entry_subtitle_format,
                        entry.categoryName,
                        formatShortDate(entry.purchaseDate),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = formatBrl(entry.totalAmount),
                style = MoneyRow,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
