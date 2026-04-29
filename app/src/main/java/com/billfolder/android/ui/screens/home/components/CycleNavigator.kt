package com.billfolder.android.ui.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.billfolder.android.R
import com.billfolder.android.ui.util.formatCycleRange
import java.time.LocalDate
import java.util.Locale

/**
 * Navegador de ciclo. Mostra o label do ciclo atual em fonte grande,
 * range de datas embaixo, e setas pra navegar pra ciclo anterior/próximo.
 *
 * Por enquanto as setas são ativas mas sem ação real — backend ainda não
 * tem GET /v1/cycles?direction=prev|next implementado. Quando tiver,
 * onPrevious/onNext fazem o request e o ViewModel atualiza o state.
 */
@Composable
fun CycleNavigator(
    cycleLabel: String,
    startIso: String,
    endIso: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Text(
                text = formatPrettyMonthYear(cycleLabel, startIso),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(Modifier.size(4.dp))

            IconButton(
                onClick = onPrevious,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.cycle_previous_content_description),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.cycle_next_content_description),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(2.dp))

        Text(
            text = formatCycleRange(startIso, endIso),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Tenta gerar "abril/2026" em lowercase a partir do startIso.
 * Se não conseguir parsear, cai no label que veio do backend.
 */
private fun formatPrettyMonthYear(fallback: String, startIso: String): String =
    runCatching {
        val date = LocalDate.parse(startIso)
        val monthName = date.month.getDisplayName(
            java.time.format.TextStyle.FULL,
            Locale.Builder().setLanguage("pt").setRegion("BR").build(),
        )
        "${monthName.lowercase()}/${date.year}"
    }.getOrDefault(fallback)
