package com.billfolder.android.ui.screens.expenses

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.billfolder.android.R
import com.billfolder.android.ui.components.BillFolderDateField
import com.billfolder.android.ui.components.BillFolderDropdown
import com.billfolder.android.ui.components.BillFolderMoneyField
import com.billfolder.android.ui.components.BillFolderPrimaryButton
import com.billfolder.android.ui.components.BillFolderTextField
import com.billfolder.android.ui.components.BillFolderTransactionSheet
import com.billfolder.android.ui.components.DropdownOption

/**
 * Sheet de "nova recorrência semanal". Cria um template (label, valor por
 * sessão, dia da semana, categoria, início) que o backend usa pra gerar a
 * despesa provisionada a cada ciclo. Mesma estrutura do AddExpenseSheet
 * (reset ao abrir/fechar, savedSuccessfully → onSaved+onDismiss).
 *
 * weekday: dropdown domingo(0)..sábado(6), mapeando pt-BR label → Int.
 */
@Composable
fun AddWeeklyRecurrenceSheet(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddWeeklyRecurrenceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.resetForm()
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.resetForm() }
    }

    val labelEmpty = stringResource(R.string.add_daily_validation_label_empty)
    val amountInvalid = stringResource(R.string.add_daily_validation_amount_invalid)
    val categoryEmpty = stringResource(R.string.add_daily_validation_category_empty)
    val weekdayEmpty = stringResource(R.string.add_weekly_recurrence_validation_weekday_empty)

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) {
            onSaved()
            onDismiss()
        }
    }

    BillFolderTransactionSheet(
        title = stringResource(R.string.add_weekly_recurrence_title),
        onDismiss = onDismiss,
        isSaving = state.isSaving,
        errorMessage = state.errorMessage,
        footer = {
            BillFolderPrimaryButton(
                text = stringResource(R.string.sheet_save_cta),
                onClick = {
                    viewModel.submit(
                        labelEmptyMessage = labelEmpty,
                        amountInvalidMessage = amountInvalid,
                        categoryEmptyMessage = categoryEmpty,
                        weekdayEmptyMessage = weekdayEmpty,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoadingReferences,
            )
        },
    ) {
        val categoryOptions = state.categories.map {
            DropdownOption(label = it.namePt, value = it.id)
        }
        val selectedCategoryLabel = state.categories
            .firstOrNull { it.id == state.selectedCategoryId }
            ?.namePt
            ?: ""

        val weekdayOptions = weekdayLabels().mapIndexed { index, label ->
            DropdownOption(label = label, value = index)
        }
        val selectedWeekdayLabel = state.weekday
            ?.let { weekdayLabels().getOrNull(it) }
            ?: ""

        BillFolderTextField(
            value = state.label,
            onValueChange = viewModel::onLabelChange,
            label = stringResource(R.string.add_weekly_recurrence_field_label),
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Text,
            enabled = !state.isSaving,
        )

        BillFolderMoneyField(
            value = state.amount,
            onValueChange = viewModel::onAmountChange,
            label = stringResource(R.string.add_weekly_recurrence_field_amount),
            enabled = !state.isSaving,
            imeAction = ImeAction.Next,
        )

        BillFolderDropdown(
            label = stringResource(R.string.add_weekly_recurrence_field_weekday),
            selectedLabel = selectedWeekdayLabel,
            options = weekdayOptions,
            onSelect = viewModel::onWeekdayChange,
            enabled = !state.isSaving,
        )

        BillFolderDropdown(
            label = stringResource(R.string.add_weekly_recurrence_field_category),
            selectedLabel = selectedCategoryLabel,
            options = categoryOptions,
            onSelect = viewModel::onCategoryChange,
            enabled = !state.isSaving && categoryOptions.isNotEmpty(),
        )

        BillFolderDateField(
            isoDate = state.startDate,
            onIsoDateChange = viewModel::onStartDateChange,
            label = stringResource(R.string.add_weekly_recurrence_field_start_date),
            enabled = !state.isSaving,
        )
    }
}

/**
 * Labels dos dias da semana em ordem de índice do backend
 * (0=domingo .. 6=sábado). Índice na lista == valor weekday.
 */
@Composable
private fun weekdayLabels(): List<String> = listOf(
    stringResource(R.string.weekday_sunday),
    stringResource(R.string.weekday_monday),
    stringResource(R.string.weekday_tuesday),
    stringResource(R.string.weekday_wednesday),
    stringResource(R.string.weekday_thursday),
    stringResource(R.string.weekday_friday),
    stringResource(R.string.weekday_saturday),
)
