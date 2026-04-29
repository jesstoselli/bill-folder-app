package com.billfolder.android.ui.screens.income

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import com.billfolder.android.data.dto.IncomeEntryResponse
import com.billfolder.android.data.dto.IncomeSourceResponse
import com.billfolder.android.ui.components.BillFolderPrimaryButton
import com.billfolder.android.ui.components.BillFolderTotalCard
import com.billfolder.android.ui.screens.home.components.CycleNavigator
import com.billfolder.android.ui.screens.income.components.IncomeEntryRow
import com.billfolder.android.ui.screens.income.components.IncomeSourceRow
import com.billfolder.android.ui.theme.PillShape
import com.billfolder.android.ui.util.formatBrl

/**
 * Tela "recebimentos". Estrutura:
 *  - Hero card com total esperado + subtítulo "recebido X / Y"
 *  - Seção "no ciclo" — entries (status expected/late/received/notOccurred)
 *    com tap pra abrir Confirm Received pras pendentes
 *  - Seção "fontes recorrentes" — config (não transações)
 *  - FAB → AddIncomeEntrySheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeScreen(
    onBack: () -> Unit,
    viewModel: IncomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var confirmingEntry by remember { mutableStateOf<IncomeEntryResponse?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.income_screen_title),
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
                IncomeUiState.Loading -> CenteredLoading()
                IncomeUiState.NoCycle -> NoCycleEmptyState()
                is IncomeUiState.Error -> ErrorState(
                    message = s.message,
                    onRetry = viewModel::refresh,
                )
                is IncomeUiState.Content -> IncomeContent(
                    entries = s.entries,
                    sources = s.sources,
                    cycleStart = s.cycle.startDate,
                    cycleEnd = s.cycle.endDate,
                    cycleLabel = s.cycle.label,
                    onAddEntry = { showAddSheet = true },
                    onConfirmReceive = { entry -> confirmingEntry = entry },
                )
            }
        }

        if (showAddSheet) {
            AddIncomeEntrySheet(
                onDismiss = { showAddSheet = false },
                onSaved = { viewModel.refresh() },
            )
        }

        confirmingEntry?.let { entry ->
            ConfirmIncomeSheet(
                entry = entry,
                onDismiss = { confirmingEntry = null },
                onSaved = { viewModel.refresh() },
            )
        }
    }
}

@Composable
private fun IncomeContent(
    entries: List<IncomeEntryResponse>,
    sources: List<IncomeSourceResponse>,
    cycleStart: String,
    cycleEnd: String,
    cycleLabel: String,
    onAddEntry: () -> Unit,
    onConfirmReceive: (IncomeEntryResponse) -> Unit,
) {
    // Total esperado: soma todos os entries do ciclo (exceto notOccurred)
    val expectedTotal = entries
        .filter { !it.status.equals("notOccurred", ignoreCase = true) }
        .sumOf { it.expectedAmount }

    // Recebido: só os que efetivamente foram received
    val receivedTotal = entries
        .filter { it.status.equals("received", ignoreCase = true) }
        .sumOf { it.actualAmount ?: it.expectedAmount }

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
                onPrevious = { /* TODO */ },
                onNext = { /* TODO */ },
            )
        }

        item {
            BillFolderTotalCard(
                total = expectedTotal,
                label = stringResource(R.string.income_total_label),
                subtitle = stringResource(
                    R.string.income_total_subtitle_format,
                    formatBrl(receivedTotal),
                    formatBrl(expectedTotal),
                ),
            )
        }

        if (entries.isEmpty() && sources.isEmpty()) {
            item { EmptyListState(onAddEntry = onAddEntry) }
        }

        if (entries.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.income_section_this_month)) }
            items(entries, key = { it.id }) { entry ->
                val canConfirm = entry.status.equals("expected", ignoreCase = true) ||
                    entry.status.equals("late", ignoreCase = true)
                IncomeEntryRow(
                    entry = entry,
                    onClick = if (canConfirm) {
                        { onConfirmReceive(entry) }
                    } else {
                        null
                    },
                )
            }
        }

        if (sources.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.income_section_recurring)) }
            items(sources, key = { it.id }) { source ->
                IncomeSourceRow(source = source)
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

// ----- States laterais -----

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
                text = stringResource(R.string.income_no_cycle_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.income_no_cycle_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun EmptyListState(onAddEntry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.income_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.income_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        BillFolderPrimaryButton(
            text = stringResource(R.string.common_add),
            onClick = onAddEntry,
            modifier = Modifier.fillMaxWidth(fraction = 0.7f),
        )
    }
}
