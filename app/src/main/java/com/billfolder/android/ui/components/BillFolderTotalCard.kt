package com.billfolder.android.ui.components

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
import androidx.compose.ui.unit.dp
import com.billfolder.android.ui.theme.MoneyDisplay
import com.billfolder.android.ui.util.formatBrl

/**
 * Card de "total no ciclo" — reutilizável entre todas as telas de listagem
 * de transação (despesas avulsas, despesas, recebimentos, faturas, poupança).
 *
 * Variante mais sóbria que o hero da Home: usa `surfaceContainer` sem
 * outline ou hatching, porque não é a estrela da tela — só dá contexto
 * agregado pra lista logo abaixo.
 *
 * Caller passa o label apropriado pra cada contexto:
 *  - "total avulsas no ciclo"
 *  - "total despesas no ciclo"
 *  - "total recebido no ciclo"
 *  - etc.
 *
 * Subtítulo opcional pra quando o caller quer mostrar uma 2ª métrica
 * agregada (ex: "recebido R$ 5.730 / 9.947" no card de income).
 */
@Composable
fun BillFolderTotalCard(
    total: Double,
    label: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
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
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = formatBrl(total),
                style = MoneyDisplay,
                color = MaterialTheme.colorScheme.primary,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
