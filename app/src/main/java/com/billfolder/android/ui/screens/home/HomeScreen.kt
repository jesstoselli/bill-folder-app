package com.billfolder.android.ui.screens.home

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.billfolder.android.R
import com.billfolder.android.data.dto.HomeCardStatementDto
import com.billfolder.android.data.dto.HomeResponse
import com.billfolder.android.data.dto.HomeUpcomingExpenseDto
import com.billfolder.android.ui.components.BillFolderTopBar
import com.billfolder.android.ui.screens.home.components.CycleNavigator
import com.billfolder.android.ui.screens.home.components.HomeHeroCard
import com.billfolder.android.ui.screens.home.components.HomeListRow
import com.billfolder.android.ui.theme.PillShape

/**
 * Home V2 — fiel ao wireframe do BillFolder-InventarioTelas v0.1 §5.
 *
 * Estrutura:
 *  - Top bar: ☰ + wordmark + avatar
 *  - Cycle navigator (label + range + setas)
 *  - Hero card "available amount" com hatching
 *  - Seção "next due" (despesas/faturas pending dentro do ciclo)
 *  - Seção "overdue" (despesas/faturas atrasadas)
 *  - FAB pílula "adicionar" — placeholder, Speed Dial entra na fase 3
 */
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BillFolderTopBar(
                onMenuClick  = { /* TODO drawer — fase 3 */ },
                onAvatarClick = { viewModel.logout(onDone = onLogout) },
            )
        },
        floatingActionButton = { AddFab(onClick = { /* TODO Speed Dial */ }) },
    ) { innerPadding ->
        when (val s = state) {
            HomeUiState.Loading  -> CenteredLoading(innerPadding)
            HomeUiState.NoCycle  -> NoCycleEmptyState(innerPadding)
            is HomeUiState.Error -> ErrorState(
                message = s.message,
                onRetry = viewModel::refresh,
                paddingValues = innerPadding,
            )
            is HomeUiState.Content -> HomeContent(
                data = s.data,
                paddingValues = innerPadding,
                onPreviousCycle = { /* TODO */ },
                onNextCycle = { /* TODO */ },
            )
        }
    }
}

@Composable
private fun HomeContent(
    data: HomeResponse,
    paddingValues: PaddingValues,
    onPreviousCycle: () -> Unit,
    onNextCycle: () -> Unit,
) {
    val cardStatementSubtitle = stringResource(R.string.home_list_card_statement_subtitle)
    val nextDue = collectNextDue(data, cardStatementSubtitle)
    val overdue = collectOverdue(data)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            CycleNavigator(
                cycleLabel = data.cycle.label,
                startIso = data.cycle.startDate,
                endIso = data.cycle.endDate,
                onPrevious = onPreviousCycle,
                onNext = onNextCycle,
            )
        }

        item {
            HomeHeroCard(balance = data.balance)
        }

        if (nextDue.isNotEmpty()) {
            item { SectionHeader(text = stringResource(R.string.home_section_next_due)) }
            items(nextDue, key = { it.id }) { row ->
                HomeListRow(
                    title    = row.title,
                    subtitle = row.subtitle,
                    amount   = row.amount,
                    isoDate  = row.dueDate,
                    status   = row.status,
                )
            }
        }

        if (overdue.isNotEmpty()) {
            item {
                SectionHeader(
                    text = stringResource(R.string.home_section_overdue),
                    trailingIcon = true,
                )
            }
            items(overdue, key = { it.id }) { row ->
                HomeListRow(
                    title    = row.title,
                    subtitle = row.subtitle,
                    amount   = row.amount,
                    isoDate  = row.dueDate,
                    status   = row.status,
                )
            }
        }

        // Espaço extra no fim pra FAB não cobrir o último item.
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun SectionHeader(text: String, trailingIcon: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (trailingIcon) {
            Spacer(Modifier.size(8.dp))
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = stringResource(R.string.home_overdue_warning_content_description),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun AddFab(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = PillShape,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(R.string.common_add),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = stringResource(R.string.common_add),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

// ----------------------------------------------------------------------------
// Helpers — unifica HomeUpcomingExpenseDto + HomeCardStatementDto numa
// projeção comum, e separa em "next due" vs "overdue" baseado no status.
// ----------------------------------------------------------------------------

private data class HomeRowProjection(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val dueDate: String,
    val status: String,
)

private fun HomeUpcomingExpenseDto.toRow() = HomeRowProjection(
    id = id,
    title = label,
    subtitle = categoryName,
    amount = expectedAmount,
    dueDate = dueDate,
    status = status,
)

private fun HomeCardStatementDto.toRow(subtitle: String) = HomeRowProjection(
    id = id,
    title = cardName,
    subtitle = subtitle,
    amount = totalAmount,
    dueDate = dueDate,
    status = status,
)

private fun collectNextDue(data: HomeResponse, cardStatementSubtitle: String): List<HomeRowProjection> {
    val expenses = data.upcomingExpenses
        .filter { !it.status.equals("overdue", ignoreCase = true) }
        .map { it.toRow() }
    val statements = data.cardStatementsInCycle
        .filter { !it.status.equals("paid", ignoreCase = true) }
        .map { it.toRow(cardStatementSubtitle) }
    return (expenses + statements).sortedBy { it.dueDate }
}

private fun collectOverdue(data: HomeResponse): List<HomeRowProjection> =
    data.upcomingExpenses
        .filter { it.status.equals("overdue", ignoreCase = true) }
        .map { it.toRow() }
        .sortedBy { it.dueDate }

// ----------------------------------------------------------------------------
// Loading / Error / Empty states
// ----------------------------------------------------------------------------

@Composable
private fun CenteredLoading(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    paddingValues: PaddingValues,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
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
private fun NoCycleEmptyState(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.home_no_cycle_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_no_cycle_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
