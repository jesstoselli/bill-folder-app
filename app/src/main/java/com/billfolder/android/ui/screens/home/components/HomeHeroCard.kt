package com.billfolder.android.ui.screens.home.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.billfolder.android.R
import com.billfolder.android.data.dto.HomeBalanceDto
import com.billfolder.android.ui.theme.BfBrandBill
import com.billfolder.android.ui.theme.MoneyDisplayLarge
import com.billfolder.android.ui.theme.cashHatching
import com.billfolder.android.ui.util.formatBrl

/**
 * Hero card da Home, conforme DS v0.2:
 *  - Fundo `surfaceContainerHigh` (#2D2D2D)
 *  - Outline 1.5dp em primary (estado ativo)
 *  - Shape `extraLarge` (28dp) — único componente com esse raio
 *  - Cash hatching ativo (textura sutil de cédula a 6% de alpha)
 *
 * Conteúdo (lowercase, conforme convenção):
 *  - "available amount"
 *  - Valor grande em primary (verde claro) ou error (se negativo)
 *  - "realizado: R$ X.XXX,XX" — paid + daily expenses do ciclo
 */
@Composable
fun HomeHeroCard(
    balance: HomeBalanceDto,
    modifier: Modifier = Modifier,
    withHatching: Boolean = true,
) {
    val realized = balance.paidExpenses + balance.dailyExpensesSpent
    val remainingColor = if (balance.remaining >= 0) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.extraLarge,
            ),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .let {
                    if (withHatching) {
                        // Hatching usa BfBrandBill (seed exata, constante entre dark/light).
                        it.cashHatching(color = BfBrandBill, alpha = 0.06f)
                    } else {
                        it
                    }
                }
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.home_hero_available_amount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = formatBrl(balance.remaining),
                style = MoneyDisplayLarge,
                color = remainingColor,
            )
            Spacer(Modifier.height(20.dp))
            RealizedRow(realized = realized, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun RealizedRow(realized: Double, color: Color) {
    Text(
        text = stringResource(R.string.home_hero_realized_format, formatBrl(realized)),
        style = MaterialTheme.typography.titleMedium,
        color = color,
    )
}
