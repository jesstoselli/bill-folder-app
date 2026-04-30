package com.billfolder.android.ui.screens.savings

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
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
import com.billfolder.android.data.dto.SavingsAccountResponse
import com.billfolder.android.data.dto.SavingsTransactionResponse
import com.billfolder.android.ui.components.BillFolderTotalCard
import com.billfolder.android.ui.components.SwipeToActionRow
import com.billfolder.android.ui.screens.home.components.CycleNavigator
import com.billfolder.android.ui.screens.managesavings.AddSavingsAccountSheet
import com.billfolder.android.ui.screens.savings.components.AddSavingsChip
import com.billfolder.android.ui.screens.savings.components.SavingsAccountCarouselChip
import com.billfolder.android.ui.screens.savings.components.SavingsTransactionRow
import com.billfolder.android.ui.theme.PillShape
import com.billfolder.android.ui.util.formatBrl

/**
 * Tela "poupança" — visão de consumo.
 *
 * Estrutura (espelho de CardsScreen):
 *  - Top: Cycle navigator
 *  - Carousel horizontal de poupanças (chips selecionáveis) + chip "+ nova"
 *  - Hero card "movimentado nessa poupança no ciclo" — net flow signed
 *  - Lista vertical de movimentações da poupança selecionada no ciclo
 *  - FAB → AddSavingsTransactionSheet (Passo 4) que registra nova
 *    movimentação. Em Fase B, ainda sem o sheet ativo aqui — o FAB
 *    permanece visível mas o handler é placeholder até Passo 4 conectar.
 *
 * "gerenciar poupanças" (CRUD) fica na ManageSavingsScreen, acessada via
 * drawer "manage > poupanças". O "+ nova" do carousel é atalho pra abrir
 * o AddSavingsAccountSheet em modo create direto daqui — mesma convenção
 * do CardsScreen.
 *
 * Estados sealed:
 *  - Loading
 *  - NoCycle      → mensagem "você não tem ciclo aberto" (usa strings
 *    income_no_cycle_* — já existem e são genéricas)
 *  - NoAccounts   → empty state pra usuário sem poupanças cadastradas;
 *    sem CTA aqui, manda pro drawer "gerenciar > poupanças"
 *  - Content      → fluxo normal com carousel + lista
 *  - Error        → mensagem + botão "tentar de novo"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen(
    onMenuClick: () -> Unit,
    viewModel: SavingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showAddTransactionSheet by remember { mutableStateOf(false) }
    var showAddAccountSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.savings_screen_title),
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
            // FAB só aparece quando há poupanças cadastradas — sem
            // poupanças, registrar movimentação não faz sentido (sheet
            // exige savingsAccountId).
            if (state is SavingsUiState.Content) {
                FloatingActionButton(
                    onClick = { showAddTransactionSheet = true },
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
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val s = state) {
                SavingsUiState.Loading    -> CenteredLoading()
                SavingsUiState.NoCycle    -> NoCycleEmptyState()
                SavingsUiState.NoAccounts -> NoAccountsEmptyState()
                is SavingsUiState.Error   -> ErrorState(
                    message = s.message,
                    onRetry = viewModel::refresh,
                )
                is SavingsUiState.Content -> Content(
                    state = s,
                    onSelectAccount = viewModel::onSelectAccount,
                    onAddAccount = { showAddAccountSheet = true },
                    onRequestDeleteTransaction = viewModel::requestDelete,
                    onRequestEditTransaction = viewModel::requestEdit,
                )
            }
        }

        // FAB → AddSavingsTransactionSheet em modo create. Pré-seleciona a
        // poupança que tá no carousel agora (preferredAccountId), pra
        // evitar atrito com a expectativa do usuário ("estou vendo a
        // poupança X, vou registrar nela").
        val current = state
        if (showAddTransactionSheet && current is SavingsUiState.Content) {
            AddSavingsTransactionSheet(
                onDismiss = { showAddTransactionSheet = false },
                onSaved = { viewModel.refresh() },
                preferredAccountId = current.selectedAccountId,
            )
        }

        // "+ nova" do carousel abre o AddSavingsAccountSheet em modo create.
        // Reusa o mesmo sheet do ManageSavingsScreen — atalho pra criar
        // poupança sem precisar sair pra "gerenciar > poupanças".
        if (showAddAccountSheet) {
            AddSavingsAccountSheet(
                onDismiss = { showAddAccountSheet = false },
                onSaved = { viewModel.refresh() },
            )
        }

        // Sheet de editar movimentação (modo edit). Reusa
        // AddSavingsTransactionSheet com `existing`. PATCH não toca em
        // savingsAccountId; tipo fica disabled se a tx for Transfer*.
        if (current is SavingsUiState.Content && current.editing != null) {
            AddSavingsTransactionSheet(
                existing = current.editing,
                onDismiss = viewModel::cancelEdit,
                onSaved = {
                    viewModel.cancelEdit()
                    viewModel.refresh()
                },
            )
        }

        // Dialog de confirmação de delete da movimentação — atrelado ao
        // pendingDelete do VM. Title genérico (não usamos label da tx no
        // título porque label é opcional; a mensagem cita o tipo de
        // movimentação pra dar contexto).
        if (current is SavingsUiState.Content && current.pendingDelete != null) {
            DeleteTransactionDialog(
                onConfirm = viewModel::confirmDelete,
                onCancel = viewModel::cancelDelete,
            )
        }
    }
}

@Composable
private fun Content(
    state: SavingsUiState.Content,
    onSelectAccount: (String) -> Unit,
    onAddAccount: () -> Unit,
    onRequestDeleteTransaction: (SavingsTransactionResponse) -> Unit,
    onRequestEditTransaction: (SavingsTransactionResponse) -> Unit,
) {
    val transactions = state.transactionsForSelectedAccount()
    val netFlow = state.netFlowForSelectedAccount()

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
                onPrevious = { /* TODO multi-cycle nav */ },
                onNext = { /* TODO multi-cycle nav */ },
            )
        }

        item {
            SavingsCarousel(
                accounts = state.accounts,
                selectedAccountId = state.selectedAccountId,
                onSelectAccount = onSelectAccount,
                onAddAccount = onAddAccount,
            )
        }

        item {
            // Movimentado no ciclo. Total card mostra o |netFlow| em verde
            // (cor do dinheiro), e o subtitle dá o sinal explícito —
            // "+R$ 500" se entrou líquido, "−R$ 200" se saiu líquido,
            // "neutro" se zero.
            BillFolderTotalCard(
                total = kotlin.math.abs(netFlow),
                label = stringResource(R.string.savings_total_label),
                subtitle = formatNetFlowSubtitle(netFlow),
            )
        }

        if (transactions.isEmpty()) {
            item { NoTransactionsState() }
        } else {
            items(transactions, key = { it.id }) { tx ->
                SwipeToActionRow(
                    isPending = state.pendingDelete?.id == tx.id ||
                        state.editing?.id == tx.id,
                    onDelete = { onRequestDeleteTransaction(tx) },
                    onEdit = { onRequestEditTransaction(tx) },
                ) {
                    SavingsTransactionRow(transaction = tx)
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun SavingsCarousel(
    accounts: List<SavingsAccountResponse>,
    selectedAccountId: String,
    onSelectAccount: (String) -> Unit,
    onAddAccount: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        accounts.forEach { account ->
            SavingsAccountCarouselChip(
                account = account,
                selected = account.id == selectedAccountId,
                onClick = { onSelectAccount(account.id) },
            )
        }
        AddSavingsChip(onClick = onAddAccount)
    }
}

@Composable
private fun DeleteTransactionDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(text = stringResource(R.string.savings_transaction_delete_dialog_title))
        },
        text = {
            Text(
                text = stringResource(R.string.savings_transaction_delete_dialog_message),
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

/**
 * Sem ciclo aberto. Mesmo copy do CardsScreen — copy genérico de
 * "ciclo" funciona aqui também, não precisa string dedicada.
 */
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

/**
 * Empty state quando o user ainda não tem poupanças cadastradas. Sem CTA
 * inline — manda pra "gerenciar > poupanças" via drawer (caminho da IA).
 * Mesma convenção do NoCardsEmptyState do CardsScreen.
 */
@Composable
private fun NoAccountsEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.savings_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.savings_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Poupança selecionada existe, mas ainda não tem movimentações nesse
 * ciclo. Diferente do NoAccountsEmptyState — aqui o FAB já tá visível e
 * funciona (após Passo 4 conectar a sheet).
 */
@Composable
private fun NoTransactionsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.savings_no_transactions_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.savings_no_transactions_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Subtitle do total card quando o usuário tem netFlow. Templates:
 *  - net > 0  → "entrou +R$ X no ciclo"
 *  - net < 0  → "saiu −R$ X no ciclo"
 *  - net == 0 → "movimentação neutra no ciclo"
 *
 * Mantemos o |netFlow| no número e o sinal no copy pra evitar
 * "−R$ -200" caso BRL formatter desse algum dia gerar negativo.
 */
@Composable
private fun formatNetFlowSubtitle(netFlow: Double): String {
    val tolerance = 0.005 // sub-centavo
    return when {
        netFlow > tolerance  -> stringResource(
            R.string.savings_net_flow_inflow_format,
            formatBrl(netFlow),
        )
        netFlow < -tolerance -> stringResource(
            R.string.savings_net_flow_outflow_format,
            formatBrl(-netFlow),
        )
        else -> stringResource(R.string.savings_net_flow_neutral)
    }
}
