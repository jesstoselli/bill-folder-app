package com.billfolder.android.ui.screens.cards.components

import androidx.compose.foundation.background
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
import com.billfolder.android.ui.theme.BfPositiveAmount
import com.billfolder.android.ui.theme.MoneyRow
import com.billfolder.android.ui.theme.PillShape
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
 * Estorno (valor negativo) aparece em verde com o chip "estorno".
 */
@Composable
fun CardInstallmentRow(
    installment: CardInstallmentDisplay,
    modifier: Modifier = Modifier,
) {
    // Estorno é crédito: parcela negativa, mostrada em verde com o chip.
    val isRefund = installment.amount < 0
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = labelWithProgress,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isRefund) {
                        Text(
                            text = stringResource(R.string.cards_refund_chip),
                            style = MaterialTheme.typography.labelSmall,
                            color = BfPositiveAmount,
                            modifier = Modifier
                                .background(
                                    color = BfPositiveAmount.copy(alpha = 0.14f),
                                    shape = PillShape,
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
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
                color = if (isRefund) BfPositiveAmount else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
