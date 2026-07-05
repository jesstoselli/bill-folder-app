package com.billfolder.android.ui.screens.adjustments

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
import com.billfolder.android.data.dto.CycleAdjustmentResponse
import com.billfolder.android.ui.components.BillFolderPullToRefresh
import com.billfolder.android.ui.components.BillFolderTotalCard
import com.billfolder.android.ui.components.SwipeToActionRow
import com.billfolder.android.ui.screens.adjustments.components.AdjustmentRow
import com.billfolder.android.ui.screens.home.components.CycleNavigator
import com.billfolder.android.ui.theme.PillShape
import com.billfolder.android.ui.util.formatBrl

/**
 * Tela "ajustes do ciclo" — lista os inflows/outflows avulsos do ciclo
 * atual. Mesma cara das outras telas (CycleNavigator no topo, hero card
 * com total, lista com swipe-delete/edit, FAB pra adicionar).
 *
 * O "total" no hero é o NET FLOW (inflows − outflows) — reflete o impacto
 * líquido dos ajustes no ciclo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustmentsScreen(
    onMenuClick: () -> Unit,
    viewModel: AdjustmentsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.adjustments_screen_title),
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
                AdjustmentsUiState.Loading -> CenteredLoading()
                AdjustmentsUiState.NoCycle -> NoCycleEmptyState()
                is AdjustmentsUiState.Error -> ErrorState(
                    message = s.message,
                    onRetry = viewModel::refresh,
                )
                is AdjustmentsUiState.Content -> AdjustmentsContent(
                    state = s,
                    onRequestDelete = viewModel::requestDelete,
                    onRequestEdit = viewModel::requestEdit,
                    onPreviousCycle = viewModel::goToPreviousCycle,
                    onNextCycle = viewModel::goToNextCycle,
                    onPullRefresh = viewModel::pullRefresh,
                )
            }
        }

        if (showAddSheet) {
            AddAdjustmentSheet(
                onDismiss = { showAddSheet = false },
                onSaved = { viewModel.refresh() },
            )
        }

        val current = state
        if (current is AdjustmentsUiState.Content && current.editing != null) {
            AddAdjustmentSheet(
                existing = current.editing,
                onDismiss = viewModel::cancelEdit,
                onSaved = {
                    viewModel.cancelEdit()
                    viewModel.refresh()
                },
            )
        }

        if (current is AdjustmentsUiState.Content && current.pendingDelete != null) {
            DeleteAdjustmentDialog(
                adjustmentLabel = current.pendingDelete.label,
                onConfirm = viewModel::confirmDelete,
                onCancel = viewModel::cancelDelete,
            )
        }
    }
}

@Composable
private fun AdjustmentsContent(
    state: AdjustmentsUiState.Content,
    onRequestDelete: (CycleAdjustmentResponse) -> Unit,
    onRequestEdit: (CycleAdjustmentResponse) -> Unit,
    onPreviousCycle: () -> Unit,
    onNextCycle: () -> Unit,
    onPullRefresh: () -> Unit,
) {
    val netFlow = state.netAmount()

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
                // Total é NET FLOW do ciclo (inflows − outflows). formatBrl
                // da BR_CURRENCY já formata negativos com "-R$" próprio;
                // subtitle diferencia inflows/outflows pra clareza extra.
                val inflowsSum = state.adjustments
                    .filter { it.type.equals("inflow", ignoreCase = true) }
                    .sumOf { it.amount }
                val outflowsSum = state.adjustments
                    .filter { it.type.equals("outflow", ignoreCase = true) }
                    .sumOf { it.amount }
                val subtitle = stringResource(
                    R.string.adjustments_total_subtitle_format,
                    formatBrl(inflowsSum),
                    formatBrl(outflowsSum),
                )
                BillFolderTotalCard(
                    total = netFlow,
                    label = stringResource(R.string.adjustments_total_label),
                    subtitle = subtitle,
                )
            }

            if (state.adjustments.isEmpty()) {
                item { NoAdjustmentsState() }
            } else {
                items(state.adjustments, key = { it.id }) { adjustment ->
                    SwipeToActionRow(
                        isPending = state.pendingDelete?.id == adjustment.id ||
                            state.editing?.id == adjustment.id,
                        onDelete = { onRequestDelete(adjustment) },
                        onEdit = { onRequestEdit(adjustment) },
                    ) {
                        AdjustmentRow(adjustment = adjustment)
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun DeleteAdjustmentDialog(
    adjustmentLabel: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(text = stringResource(R.string.adjustment_delete_dialog_title)) },
        text = {
            Text(
                text = stringResource(R.string.adjustment_delete_dialog_message, adjustmentLabel),
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

// ---- side states ----

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
private fun NoAdjustmentsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.adjustments_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.adjustments_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
