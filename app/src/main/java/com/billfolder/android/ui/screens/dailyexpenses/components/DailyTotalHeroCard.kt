package com.billfolder.android.ui.screens.dailyexpenses.components

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.billfolder.android.R
import com.billfolder.android.ui.theme.MoneyDisplay
import com.billfolder.android.ui.util.formatBrl

/**
 * Hero card da tela "despesas avulsas". Variante mais sóbria que o
 * hero da Home: usa surfaceContainer (#1E1E1E) sem outline ou hatching,
 * porque não é a estrela da tela — só dá contexto agregado pra lista.
 */
@Composable
fun DailyTotalHeroCard(
    total: Double,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.daily_total_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = formatBrl(total),
                style = MoneyDisplay,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
