package com.billfolder.android.ui.screens.cycles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.billfolder.android.R
import com.billfolder.android.ui.components.BillFolderDateField
import com.billfolder.android.ui.components.BillFolderPrimaryButton
import com.billfolder.android.ui.components.BillFolderTextField
import com.billfolder.android.ui.components.BillFolderTransactionSheet

/**
 * Sheet de "criar ciclo" — usado pelo NoCycle empty state da Home pra
 * desbloquear o app no primeiro uso.
 *
 * Layout enxuto (3 campos): início, fim, nome. Defaults preenchidos
 * pra cobrir o caso "ciclo do mês corrente" sem digitação. Dois date
 * fields lado a lado pra economizar altura e reforçar a relação
 * temporal.
 *
 * Não há modo edit — quando ganharmos tela de gerenciar ciclos, ela
 * usa um sheet próprio (ou esse mesmo, dependendo de como crescer a
 * superfície).
 */
@Composable
fun CreateCycleSheet(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    viewModel: CreateCycleViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val labelEmpty = stringResource(R.string.create_cycle_validation_label)
    val endBeforeStart = stringResource(R.string.create_cycle_validation_dates)
    val duplicateStart = stringResource(R.string.create_cycle_error_duplicate_start)

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) {
            onSaved()
            onDismiss()
        }
    }

    BillFolderTransactionSheet(
        title = stringResource(R.string.create_cycle_title),
        onDismiss = onDismiss,
        isSaving = state.isSaving,
        errorMessage = state.errorMessage,
        footer = {
            BillFolderPrimaryButton(
                text = stringResource(R.string.sheet_save_cta),
                onClick = {
                    viewModel.submit(
                        labelEmptyMessage = labelEmpty,
                        endBeforeStartMessage = endBeforeStart,
                        duplicateStartMessage = duplicateStart,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        BillFolderTextField(
            value = state.label,
            onValueChange = viewModel::onLabelChange,
            label = stringResource(R.string.create_cycle_field_label),
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Text,
            enabled = !state.isSaving,
        )

        // Início + fim lado a lado — economiza altura e deixa visualmente
        // claro que são as 2 pernas do mesmo intervalo.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BillFolderDateField(
                isoDate = state.startDate,
                onIsoDateChange = viewModel::onStartDateChange,
                label = stringResource(R.string.create_cycle_field_start_date),
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f),
            )
            BillFolderDateField(
                isoDate = state.endDate,
                onIsoDateChange = viewModel::onEndDateChange,
                label = stringResource(R.string.create_cycle_field_end_date),
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
