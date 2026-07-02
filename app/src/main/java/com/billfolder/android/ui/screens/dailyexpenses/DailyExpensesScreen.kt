package com.billfolder.android.ui.screens.dailyexpenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
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
import androidx.compose.foundation.shape.CircleShape
import androidx.hilt.navigation.compose.hiltViewModel
import com.billfolder.android.R
import com.billfolder.android.data.dto.DailyExpenseResponse
import com.billfolder.android.ui.components.BillFolderPrimaryButton
import com.billfolder.android.ui.components.BillFolderTotalCard
import com.billfolder.android.ui.components.SwipeToActionRow
import com.billfolder.android.ui.screens.dailyexpenses.components.DailyExpenseRow
import com.billfolder.android.ui.screens.dailyexpenses.components.DayHeader
import com.billfolder.android.ui.screens.home.components.CycleNavigator
import com.billfolder.android.ui.theme.PillShape

/**
 * Tela "despesas avulsas" — lista das daily expenses no ciclo atual,
 * agrupadas por dia com header relativo ("hoje", "ontem", "29 de abr").
 *
 * Variantes da home: aqui o hero card é mais sóbrio (sem hatching) e
 * mostra apenas o total agregado das avulsas. O FAB é simples (não
 * Speed Dial) — leva direto pro sheet de adicionar avulsa.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyExpensesScreen(
    onMenuClick: () -> Unit,
    viewModel: DailyExpensesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.daily_screen_title),
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.common_add),
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val s = state) {
                DailyExpensesUiState.Loading -> CenteredLoading()
                DailyExpensesUiState.NoCycle -> NoCycleEmptyState()
                is DailyExpensesUiState.Error -> ErrorState(
                    message = s.message,
                    onRetry = viewModel::refresh,
                )
                is DailyExpensesUiState.Content -> DailyExpensesContent(
                    state = s,
                    onAddExpense = { showAddSheet = true },
                    onRequestDelete = viewModel::requestDelete,
                    onRequestEdit = viewModel::requestEdit,
                    onPreviousCycle = viewModel::goToPreviousCycle,
                    onNextCycle = viewModel::goToNextCycle,
                )
            }
        }

        // Sheet de adicionar (modo create). ModalBottomSheet do M3 já é
        // overlay próprio, não precisa estar dentro do Box.
        if (showAddSheet) {
            AddDailyExpenseSheet(
                onDismiss = { showAddSheet = false },
                onSaved = { viewModel.refresh() },
            )
        }

        // Sheet de editar (modo edit) — visível enquanto editing != null
        // no VM. Reusa o mesmo composable, só passa `existing`.
        val current = state
        if (current is DailyExpensesUiState.Content && current.editing != null) {
            AddDailyExpenseSheet(
                existing = current.editing,
                onDismiss = viewModel::cancelEdit,
                onSaved = {
                    viewModel.cancelEdit()
                    viewModel.refresh()
                },
            )
        }

        // Dialog de confirmação de delete — atrelado ao pendingDelete do VM.
        if (current is DailyExpensesUiState.Content && current.pendingDelete != null) {
            DeleteDailyExpenseDialog(
                expenseLabel = current.pendingDelete.label,
                onConfirm = viewModel::confirmDelete,
                onCancel = viewModel::cancelDelete,
            )
        }
    }
}

@Composable
private fun DailyExpensesContent(
    state: DailyExpensesUiState.Content,
    onAddExpense: () -> Unit,
    onRequestDelete: (DailyExpenseResponse) -> Unit,
    onRequestEdit: (DailyExpenseResponse) -> Unit,
    onPreviousCycle: () -> Unit,
    onNextCycle: () -> Unit,
) {
    val expenses = state.expenses
    val total = expenses.sumOf { it.amount }
    // Agrupa por dia mantendo a ordem (mais recente primeiro porque
    // a lista vem ordenada do VM).
    val byDay: Map<String, List<DailyExpenseResponse>> = expenses.groupBy { it.date }

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
                label = stringResource(R.string.daily_total_label),
            )
        }

        if (expenses.isEmpty()) {
            item { EmptyListState(onAddExpense = onAddExpense) }
        } else {
            byDay.forEach { (isoDate, dayExpenses) ->
                item(key = "header-$isoDate") {
                    DayHeader(
                        isoDate = isoDate,
                        dayTotal = dayExpenses.sumOf { it.amount },
                    )
                }
                items(dayExpenses, key = { it.id }) { expense ->
                    SwipeToActionRow(
                        isPending = state.pendingDelete?.id == expense.id ||
                            state.editing?.id == expense.id,
                        onDelete = { onRequestDelete(expense) },
                        onEdit = { onRequestEdit(expense) },
                    ) {
                        DailyExpenseRow(expense = expense)
                    }
                }
            }
        }

        // Espaço pro FAB não cobrir o último item.
        item { Spacer(Modifier.height(80.dp)) }
    }
}

/**
 * Dialog de confirmação de delete. Mesmo padrão do DeleteCardDialog em
 * ManageCardsScreen: confirm botão em vermelho, dismiss em texto neutro,
 * tap fora do dialog = cancel.
 */
@Composable
private fun DeleteDailyExpenseDialog(
    expenseLabel: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(text = stringResource(R.string.daily_delete_dialog_title))
        },
        text = {
            Text(
                text = stringResource(R.string.daily_delete_dialog_message, expenseLabel),
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
                text = stringResource(R.string.daily_no_cycle_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.daily_no_cycle_subtitle),
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
            text = stringResource(R.string.daily_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.daily_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        // CTA explícito no empty state — em estado normal (com itens) o
        // FAB já é a ação primária e adicionar outro botão seria redundante.
        BillFolderPrimaryButton(
            text = stringResource(R.string.common_add),
            onClick = onAddExpense,
            modifier = Modifier.fillMaxWidth(fraction = 0.7f),
        )
    }
}
