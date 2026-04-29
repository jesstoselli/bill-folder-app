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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.billfolder.android.ui.screens.dailyexpenses.components.DailyTotalHeroCard
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
    onBack: () -> Unit,
    viewModel: ExpensesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var payingExpense by remember { mutableStateOf<ExpenseResponse?>(null) }

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
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
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
                ExpensesUiState.Loading -> CenteredLoading()
                ExpensesUiState.NoCycle -> NoCycleEmptyState()
                is ExpensesUiState.Error -> ErrorState(
                    message = s.message,
                    onRetry = viewModel::refresh,
                )
                is ExpensesUiState.Content -> ExpensesContent(
                    expenses = s.expenses,
                    cycleStart = s.cycle.startDate,
                    cycleEnd = s.cycle.endDate,
                    cycleLabel = s.cycle.label,
                    onAddExpense = { showAddSheet = true },
                    onPayExpense = { expense -> payingExpense = expense },
                )
            }
        }

        if (showAddSheet) {
            AddExpenseSheet(
                onDismiss = { showAddSheet = false },
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
    }
}

@Composable
private fun ExpensesContent(
    expenses: List<ExpenseResponse>,
    cycleStart: String,
    cycleEnd: String,
    cycleLabel: String,
    onAddExpense: () -> Unit,
    onPayExpense: (ExpenseResponse) -> Unit,
) {
    val total = expenses.sumOf { it.actualAmount ?: it.expectedAmount }
    val overdue = expenses.filter { it.status.equals("overdue", ignoreCase = true) }
    val upcoming = expenses.filter { it.status.equals("pending", ignoreCase = true) }
    val paid = expenses.filter { it.status.equals("paid", ignoreCase = true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            CycleNavigator(
                cycleLabel = cycleLabel,
                startIso = cycleStart,
                endIso = cycleEnd,
                onPrevious = { /* TODO navegação de ciclo */ },
                onNext = { /* TODO */ },
            )
        }

        item {
            DailyTotalHeroCard(
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
                ExpenseRow(expense = exp, onClick = { onPayExpense(exp) })
            }
        }

        if (upcoming.isNotEmpty()) {
            item {
                SectionHeader(text = stringResource(R.string.expenses_section_upcoming))
            }
            items(upcoming, key = { it.id }) { exp ->
                ExpenseRow(expense = exp, onClick = { onPayExpense(exp) })
            }
        }

        if (paid.isNotEmpty()) {
            item { SectionHeader(text = stringResource(R.string.expenses_section_paid)) }
            // Paid sem onClick — já tá pago, tap não faz nada
            items(paid, key = { it.id }) { exp ->
                ExpenseRow(expense = exp, onClick = null)
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
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
