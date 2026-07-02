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
import com.billfolder.android.ui.screens.cards.CardInstallmentDisplay
import com.billfolder.android.ui.theme.MoneyRow
import com.billfolder.android.ui.util.formatBrl
import com.billfolder.android.ui.util.formatShortDate

/**
 * Linha de PARCELA na fatura do cartão, mostra uma parcela específica com seu
 * valor unitário.
 *
 * Formato do label:
 *  - Compra à vista (installmentsCount == 1): "Karoline Miranda"
 *  - Parcelada: "Karoline Miranda (1/4)" — indica qual parcela dentro
 *    do total, útil pra user rastrear "essa é a 3ª parcela de 6"
 *
 * Valor exibido: valor da PARCELA (installment.amount), não da compra.
 */
@Composable
fun CardInstallmentRow(
    installment: CardInstallmentDisplay,
    modifier: Modifier = Modifier,
) {
    val labelWithProgress = if (installment.installmentsCount > 1) {
        stringResource(
            R.string.cards_installment_progress_format,
            installment.label,
            installment.installmentNumber,
            installment.installmentsCount,
        )
    } else {
        installment.label
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
                    text = labelWithProgress,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(
                        R.string.cards_entry_subtitle_format,
                        installment.categoryName,
                        formatShortDate(installment.purchaseDate),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = formatBrl(installment.amount),
                style = MoneyRow,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
