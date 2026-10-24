package com.billfolder.android.ui.screens.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.billfolder.android.R
import com.billfolder.android.data.dto.ExpenseResponse
import com.billfolder.android.ui.components.BillFolderPrimaryButton
import com.billfolder.android.ui.components.BillFolderTransactionSheet
import com.billfolder.android.ui.theme.PillShape
import com.billfolder.android.ui.util.formatBrl

/**
 * Sheet de "sessões neste ciclo" de uma despesa provisionada: − / + na
 * quantidade, com o novo total já calculado. Só este ciclo muda.
 */
@Composable
fun AdjustOccurrencesSheet(
    expense: ExpenseResponse,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AdjustOccurrencesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(expense.id) { viewModel.initializeFor(expense) }

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) {
            onSaved()
            onDismiss()
        }
    }

    BillFolderTransactionSheet(
        title = stringResource(R.string.adjust_occurrences_title),
        onDismiss = onDismiss,
        isSaving = state.isSaving,
        errorMessage = state.errorMessage,
        footer = {
            BillFolderPrimaryButton(
                text = stringResource(R.string.adjust_occurrences_cta),
                onClick = viewModel::submit,
                modifier = Modifier.fillMaxWidth(),
                loading = state.isSaving,
                enabled = state.hasChanged,
            )
        },
    ) {
        Text(
            text = stringResource(R.string.adjust_occurrences_summary_format, expense.label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                FilledTonalIconButton(
                    onClick = viewModel::decrement,
                    enabled = state.canDecrement && !state.isSaving,
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = stringResource(R.string.adjust_occurrences_decrement),
                    )
                }
                Text(
                    text = sessionsLabel(state.count),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                FilledTonalIconButton(
                    onClick = viewModel::increment,
                    enabled = state.canIncrement && !state.isSaving,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.adjust_occurrences_increment),
                    )
                }
            }

            Text(
                text = stringResource(
                    R.string.adjust_occurrences_total_format,
                    sessionsLabel(state.count),
                    formatBrl(state.perSession),
                    formatBrl(state.newTotal),
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            if (state.hasChanged) {
                Text(
                    text = stringResource(
                        R.string.adjust_occurrences_before_format,
                        formatBrl(state.originalTotal),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.paidCount > 0) {
            Text(
                text = stringResource(R.string.adjust_occurrences_paid_hint_format, state.paidCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun sessionsLabel(count: Int): String =
    if (count == 1) {
        stringResource(R.string.adjust_occurrences_count_one)
    } else {
        stringResource(R.string.adjust_occurrences_count_format, count)
    }

/** O que ajustar numa despesa semanal: o valor por sessão ou as sessões deste ciclo. */
enum class ProvisionedEditChoice { Amount, Sessions }

/**
 * Pergunta o que ajustar ao editar (swipe) uma despesa semanal. Mesmo formato
 * do RecurrenceScopeDialog: escolhas empilhadas, "cancelar" sozinho embaixo.
 */
@Composable
fun ProvisionedEditChoiceDialog(
    expenseLabel: String,
    onChoice: (ProvisionedEditChoice) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.provisioned_edit_choice_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.provisioned_edit_choice_message, expenseLabel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalButton(
                    onClick = { onChoice(ProvisionedEditChoice.Amount) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = PillShape,
                ) {
                    Text(stringResource(R.string.provisioned_edit_choice_amount))
                }
                FilledTonalButton(
                    onClick = { onChoice(ProvisionedEditChoice.Sessions) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = PillShape,
                ) {
                    Text(stringResource(R.string.provisioned_edit_choice_sessions))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}
