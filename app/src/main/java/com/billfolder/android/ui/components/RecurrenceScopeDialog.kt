package com.billfolder.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.billfolder.android.R
import com.billfolder.android.ui.theme.PillShape

/**
 * Escopo de uma ação sobre uma ocorrência de recorrência (despesa semanal
 * provisionada OU assinatura de cartão): afeta só ESTA ocorrência, ou esta
 * E as próximas.
 *
 * Modelado app-side como enum; os literais que vão pro backend saem dos
 * mappers abaixo — e o casing DIFERE por endpoint (por isso dois mappers):
 *  - DELETE (query param, snake_case): "this" / "this_and_following"
 *  - REPRICE (body enum camelCase):    "this" / "thisAndFollowing"
 */
enum class ScopeChoice {
    This,
    ThisAndFollowing,
}

/** Literal do DELETE (query param snake_case). */
fun ScopeChoice.deleteLiteral(): String = when (this) {
    ScopeChoice.This -> "this"
    ScopeChoice.ThisAndFollowing -> "this_and_following"
}

/** Literal do REPRICE (body enum camelCase — difere do delete!). */
fun ScopeChoice.repriceLiteral(): String = when (this) {
    ScopeChoice.This -> "this"
    ScopeChoice.ThisAndFollowing -> "thisAndFollowing"
}

/**
 * Modal reutilizável pra escolher o escopo (excluir/reajustar recorrência).
 *
 * As duas escolhas são botões FULL-WIDTH EMPILHADOS no corpo (slot `text`),
 * não nos slots de botão do AlertDialog — porque o M3 dispõe
 * confirmButton+dismissButton lado a lado numa linha, o que espremia/
 * desalinhava as ações longas. Aqui as escolhas ficam claras e empilhadas;
 * `cancelar` fica sozinho no `dismissButton` (canto inferior), e o
 * `confirmButton` fica vazio de propósito.
 */
@Composable
fun RecurrenceScopeDialog(
    title: String,
    message: String,
    onScopeChosen: (ScopeChoice) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalButton(
                    onClick = { onScopeChosen(ScopeChoice.This) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = PillShape,
                ) {
                    Text(stringResource(R.string.recurrence_scope_this))
                }
                FilledTonalButton(
                    onClick = { onScopeChosen(ScopeChoice.ThisAndFollowing) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = PillShape,
                ) {
                    Text(stringResource(R.string.recurrence_scope_this_and_following))
                }
            }
        },
        // Vazio de propósito — as ações são os botões empilhados acima.
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}
