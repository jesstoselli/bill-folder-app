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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.billfolder.android.ui.components.BillFolderDrawer
import com.billfolder.android.ui.components.BillFolderSpeedDialFab
import com.billfolder.android.ui.components.BillFolderTopBar
import com.billfolder.android.ui.components.DrawerDestination
import com.billfolder.android.ui.components.SpeedDialItem
import com.billfolder.android.ui.screens.dailyexpenses.AddDailyExpenseSheet
import com.billfolder.android.ui.screens.expenses.AddExpenseSheet
import com.billfolder.android.ui.screens.home.components.CycleNavigator
import com.billfolder.android.ui.screens.home.components.HomeHeroCard
import com.billfolder.android.ui.screens.home.components.HomeListRow
import com.billfolder.android.ui.screens.home.components.WhereMoneyGoingCard
import com.billfolder.android.ui.theme.PillShape
import kotlinx.coroutines.launch

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
    onNavigateFromDrawer: (DrawerDestination) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Sheets disparados pelo Speed Dial. Quando virarem 5 (income, card,
    // savings também), refatora pra sealed class "qual sheet abrir"
    // controlada por uma única var, evitando proliferação de booleans.
    var showAddDailySheet by remember { mutableStateOf(false) }
    var showAddExpenseSheet by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            BillFolderDrawer(
                selected = DrawerDestination.Home,
                onNavigate = { destination ->
                    scope.launch { drawerState.close() }
                    if (destination != DrawerDestination.Home) {
                        // Home → no-op (já estamos aqui).
                        // Outras telas: o NavHost decide a rota.
                        onNavigateFromDrawer(destination)
                    }
                },
            )
        },
    ) {
        HomeScaffold(
            state = state,
            onMenuClick = { scope.launch { drawerState.open() } },
            onAvatarClick = { viewModel.logout(onDone = onLogout) },
            onRefresh = viewModel::refresh,
            onSpeedDialDaily = { showAddDailySheet = true },
            onSpeedDialExpense = { showAddExpenseSheet = true },
        )
    }

    // Sheets renderizados fora do drawer pra ficar em cima de tudo.
    if (showAddDailySheet) {
        AddDailyExpenseSheet(
            onDismiss = { showAddDailySheet = false },
            onSaved = { viewModel.refresh() },
        )
    }
    if (showAddExpenseSheet) {
        AddExpenseSheet(
            onDismiss = { showAddExpenseSheet = false },
            onSaved = { viewModel.refresh() },
        )
    }
}

@Composable
private fun HomeScaffold(
    state: HomeUiState,
    onMenuClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onRefresh: () -> Unit,
    onSpeedDialDaily: () -> Unit,
    onSpeedDialExpense: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BillFolderTopBar(
                onMenuClick  = onMenuClick,
                onAvatarClick = onAvatarClick,
            )
        },
    ) { innerPadding ->
        // Box raiz pra que o Speed Dial possa sobrepor o conteúdo principal
        // quando aberto. Tudo abaixo do topBar respeita innerPadding.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val s = state) {
                HomeUiState.Loading  -> CenteredLoading()
                HomeUiState.NoCycle  -> NoCycleEmptyState()
                is HomeUiState.Error -> ErrorState(
                    message = s.message,
                    onRetry = onRefresh,
                )
                is HomeUiState.Content -> HomeContent(
                    data = s.data,
                    onPreviousCycle = { /* TODO */ },
                    onNextCycle = { /* TODO */ },
                )
            }

            // Speed Dial — fica em cima de tudo. Quando fechado, mostra só
            // o FAB pílula. Quando aberto, scrim cobre o conteúdo.
            BillFolderSpeedDialFab(
                items = rememberSpeedDialItems(
                    onDaily = onSpeedDialDaily,
                    onExpense = onSpeedDialExpense,
                ),
            )
        }
    }
}

@Composable
private fun rememberSpeedDialItems(
    onDaily: () -> Unit,
    onExpense: () -> Unit,
): List<SpeedDialItem> {
    val daily   = stringResource(R.string.speed_dial_daily)
    val income  = stringResource(R.string.speed_dial_income)
    val expense = stringResource(R.string.speed_dial_expense)
    val card    = stringResource(R.string.speed_dial_card)
    val savings = stringResource(R.string.speed_dial_savings)

    // Ordem por proximidade do dedo (Fitts): o item mais frequente fica
    // por último na lista — é o que renderiza mais próximo do main FAB
    // quando o Speed Dial expande. Avulsa é o mais usado no dia-a-dia,
    // poupança o menos.
    return listOf(
        SpeedDialItem(
            label = savings,
            icon = Icons.Default.Savings,
            onClick = { /* TODO sheet de savings transaction */ },
        ),
        SpeedDialItem(
            label = income,
            icon = Icons.Default.AttachMoney,
            onClick = { /* TODO sheet de income */ },
        ),
        SpeedDialItem(
            label = card,
            icon = Icons.Default.CreditCard,
            onClick = { /* TODO sheet de card entry */ },
        ),
        SpeedDialItem(
            label = expense,
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            onClick = onExpense,
        ),
        SpeedDialItem(
            label = daily,
            icon = Icons.Default.ShoppingBag,
            onClick = onDaily,
        ),
    )
}

@Composable
private fun HomeContent(
    data: HomeResponse,
    onPreviousCycle: () -> Unit,
    onNextCycle: () -> Unit,
) {
    val cardStatementSubtitle = stringResource(R.string.home_list_card_statement_subtitle)
    val nextDue = collectNextDue(data, cardStatementSubtitle)
    val overdue = collectOverdue(data)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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

        if (data.categoryBreakdown.isNotEmpty()) {
            item {
                WhereMoneyGoingCard(breakdown = data.categoryBreakdown)
            }
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
private fun CenteredLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
) {
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
