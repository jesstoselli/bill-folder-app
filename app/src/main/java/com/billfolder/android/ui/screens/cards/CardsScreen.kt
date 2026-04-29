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
import com.billfolder.android.data.dto.CardEntryResponse
import com.billfolder.android.data.dto.CreditCardAccountResponse
import com.billfolder.android.data.dto.CycleResponse
import com.billfolder.android.ui.components.BillFolderTotalCard
import com.billfolder.android.ui.screens.cards.components.AddCardChip
import com.billfolder.android.ui.screens.cards.components.CardCarouselChip
import com.billfolder.android.ui.screens.cards.components.CardEntryRow
import com.billfolder.android.ui.screens.home.components.CycleNavigator
import com.billfolder.android.ui.theme.PillShape

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
                CardsUiState.NoCycle -> NoCycleEmptyState()
                CardsUiState.NoCards -> NoCardsEmptyState()
                is CardsUiState.Error -> ErrorState(
                    message = s.message,
                    onRetry = viewModel::refresh,
                )
                is CardsUiState.Content -> Content(
                    state = s,
                    onSelectCard = viewModel::onSelectCard,
                    onAddCard = { showAddCardSheet = true },
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
    }
}

@Composable
private fun Content(
    state: CardsUiState.Content,
    onSelectCard: (String) -> Unit,
    onAddCard: () -> Unit,
) {
    val entries = state.entriesForSelectedCard()
    val total = entries.sumOf { it.totalAmount }

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
                onPrevious = { /* TODO */ },
                onNext = { /* TODO */ },
            )
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

        if (entries.isEmpty()) {
            item { NoEntriesState() }
        } else {
            items(entries, key = { it.id }) { entry ->
                CardEntryRow(entry = entry)
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
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
