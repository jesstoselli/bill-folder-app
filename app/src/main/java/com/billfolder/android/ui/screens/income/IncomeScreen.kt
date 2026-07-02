package com.billfolder.android.ui.screens.income

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import com.billfolder.android.data.dto.IncomeEntryResponse
import com.billfolder.android.data.dto.IncomeSourceResponse
import com.billfolder.android.ui.components.BillFolderPrimaryButton
import com.billfolder.android.ui.components.BillFolderTotalCard
import com.billfolder.android.ui.components.SwipeToActionRow
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
    onMenuClick: () -> Unit,
    viewModel: IncomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var showAddSourceSheet by remember { mutableStateOf(false) }
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
                IncomeUiState.Loading -> CenteredLoading()
                IncomeUiState.NoCycle -> NoCycleEmptyState()
                is IncomeUiState.Error -> ErrorState(
                    message = s.message,
                    onRetry = viewModel::refresh,
                )
                is IncomeUiState.Content -> IncomeContent(
                    state = s,
                    onAddEntry = { showAddSheet = true },
                    onAddSource = { showAddSourceSheet = true },
                    onConfirmReceive = { entry -> confirmingEntry = entry },
                    onRequestDelete = viewModel::requestDelete,
                    onRequestEdit = viewModel::requestEdit,
                    onRequestDeleteSource = viewModel::requestDeleteSource,
                    onRequestEditSource = viewModel::requestEditSource,
                    onPreviousCycle = viewModel::goToPreviousCycle,
                    onNextCycle = viewModel::goToNextCycle,
                )
            }
        }

        if (showAddSheet) {
            AddIncomeEntrySheet(
                onDismiss = { showAddSheet = false },
                onSaved = { viewModel.refresh() },
            )
        }

        // Sheet de criar fonte recorrente — Onda 6, Passo 2.
        if (showAddSourceSheet) {
            AddIncomeSourceSheet(
                onDismiss = { showAddSourceSheet = false },
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

        // Sheet de editar entry (modo edit) — visível enquanto editing != null.
        // Reusa AddIncomeEntrySheet com `existing` preenchido. PATCH não toca
        // em status/actual* (esse fluxo é do ConfirmIncomeSheet acima).
        val current = state
        if (current is IncomeUiState.Content && current.editing != null) {
            AddIncomeEntrySheet(
                existing = current.editing,
                onDismiss = viewModel::cancelEdit,
                onSaved = {
                    viewModel.cancelEdit()
                    viewModel.refresh()
                },
            )
        }

        // Dialog de confirmação de delete da entry — atrelado ao pendingDelete.
        if (current is IncomeUiState.Content && current.pendingDelete != null) {
            DeleteIncomeEntryDialog(
                onConfirm = viewModel::confirmDelete,
                onCancel = viewModel::cancelDelete,
            )
        }

        // ---- Source overlays (Onda 6) ----

        // Sheet de editar fonte (modo edit) — visível enquanto editingSource != null.
        if (current is IncomeUiState.Content && current.editingSource != null) {
            AddIncomeSourceSheet(
                existing = current.editingSource,
                onDismiss = viewModel::cancelEditSource,
                onSaved = {
                    viewModel.cancelEditSource()
                    viewModel.refresh()
                },
            )
        }

        // Dialog de confirmação de delete da fonte — registra na mensagem
        // que entries históricas vão preservadas (backend SET NULL).
        if (current is IncomeUiState.Content && current.pendingDeleteSource != null) {
            DeleteIncomeSourceDialog(
                sourceOrigin = current.pendingDeleteSource.origin,
                onConfirm = viewModel::confirmDeleteSource,
                onCancel = viewModel::cancelDeleteSource,
            )
        }
    }
}

@Composable
private fun IncomeContent(
    state: IncomeUiState.Content,
    onAddEntry: () -> Unit,
    onAddSource: () -> Unit,
    onConfirmReceive: (IncomeEntryResponse) -> Unit,
    onRequestDelete: (IncomeEntryResponse) -> Unit,
    onRequestEdit: (IncomeEntryResponse) -> Unit,
    onRequestDeleteSource: (IncomeSourceResponse) -> Unit,
    onRequestEditSource: (IncomeSourceResponse) -> Unit,
    onPreviousCycle: () -> Unit,
    onNextCycle: () -> Unit,
) {
    val entries = state.entries
    val sources = state.sources

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
                cycleLabel = state.cycle.label,
                startIso = state.cycle.startDate,
                endIso = state.cycle.endDate,
                onPrevious = onPreviousCycle,
                onNext = onNextCycle,
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
                SwipeToActionRow(
                    isPending = state.pendingDelete?.id == entry.id ||
                        state.editing?.id == entry.id,
                    onDelete = { onRequestDelete(entry) },
                    onEdit = { onRequestEdit(entry) },
                ) {
                    // Tap em entries expected/late continua abrindo
                    // ConfirmIncomeSheet — swipes são ortogonais ao tap.
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
        }

        // Header da seção "fontes recorrentes" sempre visível (mesmo sem
        // sources cadastradas) — assim o user descobre que dá pra criar
        // uma fonte. Antes da Onda 6, a seção sumia se sources estava
        // vazio; agora sempre tem o CTA "+ adicionar fonte".
        item {
            SectionHeader(
                text = stringResource(R.string.income_section_recurring),
                actionLabel = stringResource(R.string.income_section_recurring_add),
                onAction = onAddSource,
            )
        }

        if (sources.isNotEmpty()) {
            items(sources, key = { it.id }) { source ->
                // Resolve a entry vinculada à source no ciclo atual. entries
                // já vem filtrada pelo ciclo (backend + client-side), então
                // basta match por sourceId. Uma fonte pode ter no máximo
                // uma entry por ciclo (materializada pelo IncomeSourceExpansion),
                // firstOrNull cobre o caso.
                val linkedEntry = state.entries.firstOrNull { it.sourceId == source.id }
                val canConfirmSource = linkedEntry != null && (
                    linkedEntry.status.equals("expected", ignoreCase = true) ||
                        linkedEntry.status.equals("late", ignoreCase = true)
                )

                SwipeToActionRow(
                    isPending = state.pendingDeleteSource?.id == source.id ||
                        state.editingSource?.id == source.id,
                    onDelete = { onRequestDeleteSource(source) },
                    onEdit = { onRequestEditSource(source) },
                ) {
                    // Tap na source abre o mesmo ConfirmIncomeSheet do tap em
                    // entry expected/late. Se não há entry pendente vinculada
                    // (edge case ou já foi confirmada), onClick fica null e a
                    // row não é tappable — evita reconfirmação acidental.
                    IncomeSourceRow(
                        source = source,
                        onClick = if (canConfirmSource) {
                            { onConfirmReceive(linkedEntry!!) }
                        } else {
                            null
                        },
                        // Chip mostra o status da entry vinculada — usuário
                        // vê se aquela fonte já entrou no ciclo (recebido) ou
                        // ainda tá pendente (esperado/em atraso).
                        linkedEntryStatus = linkedEntry?.status,
                    )
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

/**
 * Dialog de delete de IncomeSource. Mensagem registra que recebimentos
 * passados ficam preservados (backend usa ON DELETE SET NULL em
 * income_entries.source_id) — info importante porque sem isso o user
 * pode hesitar achando que vai apagar o histórico.
 */
@Composable
private fun DeleteIncomeSourceDialog(
    sourceOrigin: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(text = stringResource(R.string.income_source_delete_dialog_title))
        },
        text = {
            Text(
                text = stringResource(
                    R.string.income_source_delete_dialog_message,
                    sourceOrigin,
                ),
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

/**
 * Dialog de confirmação de delete de IncomeEntry. Usa mensagem genérica
 * (sem interpolar label/source) — IncomeEntry não tem um nome curto
 * óbvio; o user já tem contexto do swipe pra saber qual é. Mesmo padrão
 * visual dos outros delete dialogs.
 */
@Composable
private fun DeleteIncomeEntryDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(text = stringResource(R.string.income_entry_delete_dialog_title))
        },
        text = {
            Text(text = stringResource(R.string.income_entry_delete_dialog_message))
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

/**
 * Header de seção com action opcional alinhada à direita. Quando
 * `actionLabel` + `onAction` são fornecidos, mostra um TextButton
 * pequeno (ex: "+ adicionar fonte") sem competir com a tipografia do
 * título.
 */
@Composable
private fun SectionHeader(
    text: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (actionLabel != null && onAction != null) {
            TextButton(
                onClick = onAction,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(
                    text = "+ $actionLabel",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
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
