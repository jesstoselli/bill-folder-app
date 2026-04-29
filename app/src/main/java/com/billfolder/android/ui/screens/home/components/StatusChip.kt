package com.billfolder.android.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.billfolder.android.R
import com.billfolder.android.ui.theme.PillShape

/**
 * Chip pequeno mostrando o status de uma despesa ou fatura.
 * Usa pares container/onContainer do M3 — assim o chip sempre tem
 * contraste correto e respeita dark/light automaticamente.
 *
 * Mapeamento de status (alinhado ao DS v0.2):
 *  - paid / received       → primary (concluído, verde de marca)
 *  - open / closed         → tertiary (aguardando ação, neutro positivo)
 *  - overdue / late        → error (urgente, coral)
 *  - pending / notoccurred → surface neutro (aguardando, sem urgência)
 */
@Composable
fun StatusChip(status: String, modifier: Modifier = Modifier) {
    val (label, container, onContainer) = roleFor(status)

    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = onContainer,
        modifier = modifier
            .background(color = container, shape = PillShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

private data class StatusRole(
    val label: String,
    val container: Color,
    val onContainer: Color,
)

@Composable
private fun roleFor(status: String): StatusRole {
    val cs = MaterialTheme.colorScheme
    // Cada status tem rótulo específico — "paid" e "received" são
    // semanticamente diferentes mesmo com cor igual: pago é despesa,
    // recebido é receita. Idem pra "overdue" (despesa atrasada) vs
    // "late" (recebimento que não veio).
    return when (status.lowercase()) {
        "paid"        -> StatusRole(stringResource(R.string.status_paid),     cs.primaryContainer,   cs.onPrimaryContainer)
        "received"    -> StatusRole(stringResource(R.string.status_received), cs.primaryContainer,   cs.onPrimaryContainer)
        "open"        -> StatusRole(stringResource(R.string.status_open),     cs.tertiaryContainer,  cs.onTertiaryContainer)
        "closed"      -> StatusRole(stringResource(R.string.status_closed),   cs.tertiaryContainer,  cs.onTertiaryContainer)
        "overdue"     -> StatusRole(stringResource(R.string.status_overdue),  cs.errorContainer,     cs.onErrorContainer)
        "late"        -> StatusRole(stringResource(R.string.status_late),     cs.errorContainer,     cs.onErrorContainer)
        "pending"     -> StatusRole(stringResource(R.string.status_pending),  cs.surfaceContainerHigh, cs.onSurfaceVariant)
        "expected"    -> StatusRole(stringResource(R.string.status_expected), cs.surfaceContainerHigh, cs.onSurfaceVariant)
        "notoccurred" -> StatusRole(stringResource(R.string.status_waiting),  cs.surfaceContainerHigh, cs.onSurfaceVariant)
        else          -> StatusRole(
            label = status.replaceFirstChar { it.uppercase() },
            container = cs.surfaceContainerHigh,
            onContainer = cs.onSurfaceVariant,
        )
    }
}
