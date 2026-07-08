package com.billfolder.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.billfolder.android.R

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
 * Modal reutilizável pra escolher o escopo. Mesma linguagem visual do
 * DeleteExpenseDialog / logout confirm (AlertDialog M3, surfaceContainerHigh).
 *
 * Duas ações empilhadas ("só esta" / "esta e as próximas") + cancelar. O
 * AlertDialog do M3 só tem os slots confirm/dismiss, então empilhamos as
 * duas escolhas de escopo num Column no slot `confirmButton` e deixamos
 * cancelar no `dismissButton`.
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
        text = { Text(text = message) },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Top,
            ) {
                TextButton(
                    onClick = { onScopeChosen(ScopeChoice.This) },
                ) {
                    Text(stringResource(R.string.recurrence_scope_this))
                }
                TextButton(
                    onClick = { onScopeChosen(ScopeChoice.ThisAndFollowing) },
                ) {
                    Text(stringResource(R.string.recurrence_scope_this_and_following))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}
