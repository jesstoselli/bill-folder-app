package com.billfolder.android.ui.screens.cards

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
import com.billfolder.android.data.dto.CardEntryResponse
import com.billfolder.android.data.dto.CreditCardAccountResponse
import com.billfolder.android.ui.components.BillFolderPullToRefresh
import com.billfolder.android.ui.components.BillFolderTotalCard
import com.billfolder.android.ui.components.SwipeToActionRow
import com.billfolder.android.ui.screens.cards.components.AddCardChip
import com.billfolder.android.ui.screens.cards.components.CardCarouselChip
import com.billfolder.android.ui.screens.cards.components.CardInstallmentRow
import com.billfolder.android.ui.screens.home.components.CycleNavigator
import com.billfolder.android.ui.theme.PillShape
import com.billfolder.android.ui.util.RefreshOnResume
import com.billfolder.android.ui.util.ptBrMonthYearOf

/**
 * Tela "despesas no cartão" — visão de consumo.
 *
 * Estrutura:
 *  - Top: Cycle navigator
 *  - Carousel horizontal de cartões (chips selecionáveis)
 *  - Hero card "total nesse cartão no ciclo"
 *  - Lista vertical de compras (CardEntry) do cartão selecionado
 *  - FAB → AddCardEntrySheet (registra nova compra)
 *
 * "gerenciar cartões" (CRUD) fica em outra tela acessada pelo
 * drawer "manage > cartões" (ManageCardsScreen).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(
    onMenuClick: () -> Unit,
    viewModel: CardsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    RefreshOnResume { viewModel.refresh() }
    var showAddEntrySheet by remember { mutableStateOf(false) }
    var showAddCardSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.cards_screen_title),
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
            // FAB só aparece quando há cartões cadastrados — sem cartões,
            // adicionar compra não faz sentido (o sheet exige cardId).
            if (state is CardsUiState.Content) {
                FloatingActionButton(
                    onClick = { showAddEntrySheet = true },
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
                CardsUiState.Loading -> CenteredLoading()
                CardsUiState.NoCards -> NoCardsEmptyState()
                is CardsUiState.Error -> ErrorState(
                    message = s.message,
                    onRetry = viewModel::refresh,
                )
                is CardsUiState.Content -> Content(
                    state = s,
                    onSelectCard = viewModel::onSelectCard,
                    onAddCard = { showAddCardSheet = true },
                    onRequestDeleteEntry = viewModel::requestDelete,
                    onRequestEditEntry = viewModel::requestEdit,
                    onPreviousCycle = viewModel::goToPreviousCycle,
                    onNextCycle = viewModel::goToNextCycle,
                    onPullRefresh = viewModel::pullRefresh,
                )
            }
        }

        if (showAddEntrySheet) {
            AddCardEntrySheet(
                onDismiss = { showAddEntrySheet = false },
                onSaved = { viewModel.refresh() },
            )
        }

        if (showAddCardSheet) {
            AddCreditCardSheet(
                onDismiss = { showAddCardSheet = false },
                onSaved = { viewModel.refresh() },
            )
        }

        // Sheet de editar entry (modo edit). Reusa AddCardEntrySheet com
        // `existing`. PATCH só toca em label/categoria/notes — campos
        // cartão/data/valor/parcelas ficam disabled.
        val current = state
        if (current is CardsUiState.Content && current.editing != null) {
            AddCardEntrySheet(
                existing = current.editing,
                onDismiss = viewModel::cancelEdit,
                onSaved = {
                    viewModel.cancelEdit()
                    viewModel.refresh()
                },
            )
        }

        // Dialog de confirmação de delete da entry — atrelado ao pendingDelete.
        if (current is CardsUiState.Content && current.pendingDelete != null) {
            DeleteCardEntryDialog(
                entryLabel = current.pendingDelete.label,
                onConfirm = viewModel::confirmDelete,
                onCancel = viewModel::cancelDelete,
            )
        }
    }
}

@Composable
private fun Content(
    state: CardsUiState.Content,
    onSelectCard: (String) -> Unit,
    onAddCard: () -> Unit,
    onRequestDeleteEntry: (com.billfolder.android.data.dto.CardEntryResponse) -> Unit,
    onRequestEditEntry: (com.billfolder.android.data.dto.CardEntryResponse) -> Unit,
    onPreviousCycle: () -> Unit,
    onNextCycle: () -> Unit,
    onPullRefresh: () -> Unit,
) {
    // Achata entries → installments filtradas pela fatura atual. Total é
    // a soma das parcelas dessa fatura (não do totalAmount da compra) —
    // reflete o que o user vai efetivamente pagar quando essa fatura vencer.
    val installments = state.installmentsForSelectedStatement()
    val total = installments.sumOf { it.amount }

    BillFolderPullToRefresh(
        isRefreshing = state.isRefreshing,
        onRefresh = onPullRefresh,
    ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Statement (fatura) do cartão selecionado — computado a partir da
        // data-âncora (referencePurchaseDate) + closingDay/dueDay do cartão.
        // Diferente das outras telas, aqui o "ciclo" mostrado é o do
        // CARTÃO (fechamento → vencimento), não o do BillFolder. O label
        // vem do mês/ano da dueDate: fatura fechada em 17/jul e vencendo
        // em 25/jul aparece como "julho/2026".
        val statement = state.currentStatement()
        item {
            if (statement != null) {
                CycleNavigator(
                    cycleLabel = ptBrMonthYearOf(statement.dueDate),
                    startIso = statement.periodStart.toString(),
                    endIso = statement.periodEnd.toString(),
                    onPrevious = onPreviousCycle,
                    onNext = onNextCycle,
                    headerLabelOverride = ptBrMonthYearOf(statement.dueDate),
                )
            }
        }

        item {
            CardCarousel(
                cards = state.cards,
                selectedCardId = state.selectedCardId,
                onSelectCard = onSelectCard,
                onAddCard = onAddCard,
            )
        }

        item {
            BillFolderTotalCard(
                total = total,
                label = stringResource(R.string.cards_total_label),
            )
        }

        if (installments.isEmpty()) {
            item { NoEntriesState() }
        } else {
            items(installments, key = { it.installmentId }) { installment ->
                // Swipe-delete/edit atua na COMPRA (entry) inteira — parcelas
                // isoladas não podem ser deletadas ou editadas separadamente.
                // Resolvemos o entry pai a partir do installment.entryId; se
                // não achar (state fora de sync, raro), pula a row.
                val parentEntry = state.allEntries.firstOrNull { it.id == installment.entryId }
                    ?: return@items
                SwipeToActionRow(
                    isPending = state.pendingDelete?.id == parentEntry.id ||
                        state.editing?.id == parentEntry.id,
                    onDelete = { onRequestDeleteEntry(parentEntry) },
                    onEdit = { onRequestEditEntry(parentEntry) },
                ) {
                    CardInstallmentRow(installment = installment)
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
    }
}

/**
 * Dialog de confirmação de delete de CardEntry. Mensagem deixa explícito
 * que parcelas associadas vão junto e que faturas serão recalculadas
 * (operação não-trivial, vale o aviso).
 */
@Composable
private fun DeleteCardEntryDialog(
    entryLabel: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(text = stringResource(R.string.card_entry_delete_dialog_title))
        },
        text = {
            Text(
                text = stringResource(R.string.card_entry_delete_dialog_message, entryLabel),
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
private fun CardCarousel(
    cards: List<CreditCardAccountResponse>,
    selectedCardId: String,
    onSelectCard: (String) -> Unit,
    onAddCard: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        cards.forEach { card ->
            CardCarouselChip(
                card = card,
                selected = card.id == selectedCardId,
                onClick = { onSelectCard(card.id) },
            )
        }
        // "+ novo" sempre como último chip — atalho pra criar cartão
        // sem precisar sair pra "gerenciar > cartões".
        AddCardChip(onClick = onAddCard)
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

/**
 * Empty state quando o user ainda não tem cartões. Não tem CTA pra
 * cadastrar aqui — manda pra "gerenciar > cartões" (caminho da IA),
 * onde a feature de criar cartão vive.
 */
@Composable
private fun NoCardsEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.cards_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.cards_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Cartão selecionado existe, mas ainda não tem compras nesse ciclo.
 * Diferente do NoCardsEmptyState — aqui o FAB já tá visível e funciona.
 */
@Composable
private fun NoEntriesState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.cards_no_entries_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.cards_no_entries_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
