package com.billfolder.android.ui.screens.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.billfolder.android.R
import com.billfolder.android.data.dto.ExpenseResponse
import com.billfolder.android.ui.components.BillFolderPrimaryButton
import com.billfolder.android.ui.components.BillFolderSpeedDialFab
import com.billfolder.android.ui.components.BillFolderTotalCard
import com.billfolder.android.ui.components.BillFolderPullToRefresh
import com.billfolder.android.ui.components.SpeedDialItem
import com.billfolder.android.ui.components.SwipeToActionRow
import com.billfolder.android.ui.screens.expenses.components.ExpenseRow
import com.billfolder.android.ui.screens.home.components.CycleNavigator
import com.billfolder.android.ui.theme.PillShape

/**
 * Tela de "despesas". Estrutura idêntica à de daily expenses, com 3 seções
 * separadas por status (atrasadas / próximas / pagas) em vez de
 * agrupamento por dia. Tap numa row pending ou overdue abre o
 * PayExpenseSheet pra marcar como pago.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    onMenuClick: () -> Unit,
    viewModel: ExpensesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var showWeeklyRecurrenceSheet by remember { mutableStateOf(false) }
    var payingExpense by remember { mutableStateOf<ExpenseResponse?>(null) }
    var payingOccurrence by remember { mutableStateOf<ExpenseResponse?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.expenses_screen_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(R.string.topbar_menu),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val s = state) {
                ExpensesUiState.Loading -> CenteredLoading()
                ExpensesUiState.NoCycle -> NoCycleEmptyState()
                is ExpensesUiState.Error -> ErrorState(
                    message = s.message,
                    onRetry = viewModel::refresh,
                )
                is ExpensesUiState.Content -> ExpensesContent(
                    state = s,
                    onAddExpense = { showAddSheet = true },
                    // Branching do tap: provisionada em andamento → dar baixa
                    // (PayOccurrenceSheet); despesa normal → fluxo de pagamento
                    // cheio (PayExpenseSheet). Provisionada já 100% quitada não
                    // dispara ação (o ExpenseRow passa onClick=null nesse caso
                    // via a lista de paid, e aqui também é defensivo).
                    onPayExpense = { expense ->
                        if (expense.isProvisionedInProgress()) {
                            payingOccurrence = expense
                        } else {
                            payingExpense = expense
                        }
                    },
                    onRequestDelete = viewModel::requestDelete,
                    onRequestEdit = viewModel::requestEdit,
                    onPreviousCycle = viewModel::goToPreviousCycle,
                    onNextCycle = viewModel::goToNextCycle,
                    onPullRefresh = viewModel::pullRefresh,
                )
            }

            // Speed Dial: "nova despesa" (existente) + "nova recorrência
            // semanal" (novo). Mesmo padrão da Home — overlay full-screen.
            BillFolderSpeedDialFab(
                items = listOf(
                    SpeedDialItem(
                        label = stringResource(R.string.speed_dial_new_weekly_recurrence),
                        icon = Icons.Default.Event,
                        onClick = { showWeeklyRecurrenceSheet = true },
                    ),
                    SpeedDialItem(
                        label = stringResource(R.string.speed_dial_new_expense),
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        onClick = { showAddSheet = true },
                    ),
                ),
            )
        }

        if (showAddSheet) {
            AddExpenseSheet(
                onDismiss = { showAddSheet = false },
                onSaved = { viewModel.refresh() },
            )
        }

        if (showWeeklyRecurrenceSheet) {
            AddWeeklyRecurrenceSheet(
                onDismiss = { showWeeklyRecurrenceSheet = false },
                onSaved = { viewModel.refresh() },
            )
        }

        payingExpense?.let { exp ->
            PayExpenseSheet(
                expense = exp,
                onDismiss = { payingExpense = null },
                onSaved = { viewModel.refresh() },
            )
        }

        payingOccurrence?.let { exp ->
            PayOccurrenceSheet(
                expense = exp,
                onDismiss = { payingOccurrence = null },
                onSaved = { viewModel.refresh() },
            )
        }

        // Sheet de editar (modo edit) — visível enquanto editing != null no VM.
        // Reusa AddExpenseSheet com `existing` preenchido. PATCH sem mexer
        // em status/paid* (esse é caminho do PayExpenseSheet acima).
        val current = state
        if (current is ExpensesUiState.Content && current.editing != null) {
            AddExpenseSheet(
                existing = current.editing,
                onDismiss = viewModel::cancelEdit,
                onSaved = {
                    viewModel.cancelEdit()
                    viewModel.refresh()
                },
            )
        }

        // Dialog de confirmação de delete — atrelado ao pendingDelete do VM.
        if (current is ExpensesUiState.Content && current.pendingDelete != null) {
            DeleteExpenseDialog(
                expenseLabel = current.pendingDelete.label,
                onConfirm = viewModel::confirmDelete,
                onCancel = viewModel::cancelDelete,
            )
        }
    }
}

@Composable
private fun ExpensesContent(
    state: ExpensesUiState.Content,
    onAddExpense: () -> Unit,
    onPayExpense: (ExpenseResponse) -> Unit,
    onRequestDelete: (ExpenseResponse) -> Unit,
    onRequestEdit: (ExpenseResponse) -> Unit,
    onPreviousCycle: () -> Unit,
    onNextCycle: () -> Unit,
    onPullRefresh: () -> Unit,
) {
    val expenses = state.expenses
    val total = expenses.sumOf { it.actualAmount ?: it.expectedAmount }
    val overdue = expenses.filter { it.status.equals("overdue", ignoreCase = true) }
    val upcoming = expenses.filter { it.status.equals("pending", ignoreCase = true) }
    val paid = expenses.filter { it.status.equals("paid", ignoreCase = true) }

    BillFolderPullToRefresh(
        isRefreshing = state.isRefreshing,
        onRefresh = onPullRefresh,
    ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            CycleNavigator(
                cycleLabel = state.cycle.label,
                startIso = state.cycle.startDate,
                endIso = state.cycle.endDate,
                onPrevious = onPreviousCycle,
                onNext = onNextCycle,
            )
        }

        item {
            BillFolderTotalCard(
                total = total,
                label = stringResource(R.string.expenses_total_label),
            )
        }

        if (expenses.isEmpty()) {
            item { EmptyListState(onAddExpense = onAddExpense) }
        }

        if (overdue.isNotEmpty()) {
            item {
                SectionHeader(
                    text = stringResource(R.string.expenses_section_overdue),
                    showWarningIcon = true,
                )
            }
            items(overdue, key = { it.id }) { exp ->
                SwipeToActionRow(
                    isPending = state.pendingDelete?.id == exp.id ||
                        state.editing?.id == exp.id,
                    onDelete = { onRequestDelete(exp) },
                    onEdit = { onRequestEdit(exp) },
                ) {
                    ExpenseRow(expense = exp, onClick = { onPayExpense(exp) })
                }
            }
        }

        if (upcoming.isNotEmpty()) {
            item {
                SectionHeader(text = stringResource(R.string.expenses_section_upcoming))
            }
            items(upcoming, key = { it.id }) { exp ->
                SwipeToActionRow(
                    isPending = state.pendingDelete?.id == exp.id ||
                        state.editing?.id == exp.id,
                    onDelete = { onRequestDelete(exp) },
                    onEdit = { onRequestEdit(exp) },
                ) {
                    ExpenseRow(expense = exp, onClick = { onPayExpense(exp) })
                }
            }
        }

        if (paid.isNotEmpty()) {
            item { SectionHeader(text = stringResource(R.string.expenses_section_paid)) }
            // Paid sem onClick (tap não tem efeito — já tá pago) mas swipe
            // funciona normal: user pode querer deletar o registro ou
            // corrigir um campo errado.
            items(paid, key = { it.id }) { exp ->
                SwipeToActionRow(
                    isPending = state.pendingDelete?.id == exp.id ||
                        state.editing?.id == exp.id,
                    onDelete = { onRequestDelete(exp) },
                    onEdit = { onRequestEdit(exp) },
                ) {
                    ExpenseRow(expense = exp, onClick = null)
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
    }
}

/**
 * Dialog de confirmação de delete. Mesmo padrão do DeleteCardDialog /
 * DeleteDailyExpenseDialog: confirm em vermelho, dismiss neutro.
 */
@Composable
private fun DeleteExpenseDialog(
    expenseLabel: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(text = stringResource(R.string.expense_delete_dialog_title))
        },
        text = {
            Text(
                text = stringResource(R.string.expense_delete_dialog_message, expenseLabel),
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.common_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.common_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}

@Composable
private fun SectionHeader(text: String, showWarningIcon: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (showWarningIcon) {
            Spacer(Modifier.size(8.dp))
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ----------------------------------------------------------------------------
// States laterais
// ----------------------------------------------------------------------------

@Composable
private fun CenteredLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            OutlinedButton(onClick = onRetry, shape = PillShape) {
                Text(stringResource(R.string.common_retry))
            }
        }
    }
}

@Composable
private fun NoCycleEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.expenses_no_cycle_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.expenses_no_cycle_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun EmptyListState(onAddExpense: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.expenses_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.expenses_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        BillFolderPrimaryButton(
            text = stringResource(R.string.common_add),
            onClick = onAddExpense,
            modifier = Modifier.fillMaxWidth(fraction = 0.7f),
        )
    }
}
