package com.billfolder.android.ui.screens.dailyexpenses.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.billfolder.android.R
import com.billfolder.android.ui.theme.MoneyRow
import com.billfolder.android.ui.util.formatBrl
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Header de grupo de dia na lista. Mostra "hoje", "ontem" ou "29 de abr"
 * à esquerda, e o total do dia à direita.
 *
 * Pegamos o date como String ISO ("yyyy-MM-dd") porque é o que o backend
 * retorna; se virar caso de uso comum, abstrair pra LocalDate.
 */
@Composable
fun DayHeader(
    isoDate: String,
    dayTotal: Double,
    today: LocalDate = LocalDate.now(),
    modifier: Modifier = Modifier,
) {
    val formatted = formatRelativeOrAbsolute(isoDate, today)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatted,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatBrl(dayTotal),
            style = MoneyRow,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun formatRelativeOrAbsolute(isoDate: String, today: LocalDate): String {
    val locale = Locale.Builder().setLanguage("pt").setRegion("BR").build()
    val formatter = DateTimeFormatter.ofPattern("dd 'de' MMM", locale)

    return runCatching {
        val date = LocalDate.parse(isoDate)
        when {
            date == today -> stringResource(R.string.daily_day_today)
            date == today.minusDays(1) -> stringResource(R.string.daily_day_yesterday)
            else -> date.format(formatter)
        }
    }.getOrDefault(isoDate)
}
