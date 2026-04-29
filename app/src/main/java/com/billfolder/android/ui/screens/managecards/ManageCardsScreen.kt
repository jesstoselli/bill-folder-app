package com.billfolder.android.ui.screens.managecards

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
import com.billfolder.android.data.dto.CreditCardAccountResponse
import com.billfolder.android.ui.components.BillFolderPrimaryButton
import com.billfolder.android.ui.screens.cards.AddCreditCardSheet
import com.billfolder.android.ui.screens.cards.components.SwipeableCreditCardRow
import com.billfolder.android.ui.theme.PillShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCardsScreen(
    onMenuClick: () -> Unit,
    onNavigateToCardEntries: (cardId: String) -> Unit,
    viewModel: ManageCardsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.manage_cards_screen_title),
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
                ManageCardsUiState.Loading -> CenteredLoading()
                is ManageCardsUiState.Error -> ErrorState(
                    message = s.message,
                    onRetry = viewModel::refresh,
                )
                is ManageCardsUiState.Content -> Content(
                    state = s,
                    onAddCard = { showAddSheet = true },
                    onRequestDelete = viewModel::requestDelete,
                    onCardClick = onNavigateToCardEntries,
                )
            }
        }

        if (showAddSheet) {
            AddCreditCardSheet(
                onDismiss = { showAddSheet = false },
                onSaved = { viewModel.refresh() },
            )
        }

        // Dialog de confirmação de delete — atrelado ao pendingDelete do VM.
        val current = state
        if (current is ManageCardsUiState.Content && current.pendingDelete != null) {
            DeleteCardDialog(
                cardName = current.pendingDelete.name,
                onConfirm = viewModel::confirmDelete,
                onCancel = viewModel::cancelDelete,
            )
        }
    }
}

@Composable
private fun Content(
    state: ManageCardsUiState.Content,
    onAddCard: () -> Unit,
    onRequestDelete: (CreditCardAccountResponse) -> Unit,
    onCardClick: (cardId: String) -> Unit,
) {
    if (state.cards.isEmpty()) {
        EmptyListState(onAddCard = onAddCard)
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(state.cards, key = { it.id }) { card ->
            SwipeableCreditCardRow(
                card = card,
                pendingDelete = state.pendingDelete,
                onSwipeToDelete = onRequestDelete,
                // Tap na row → "despesas no cartão" daquele cartão.
                onClick = { onCardClick(card.id) },
            )
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun DeleteCardDialog(
    cardName: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(text = stringResource(R.string.manage_cards_delete_dialog_title))
        },
        text = {
            Text(
                text = stringResource(R.string.manage_cards_delete_dialog_message, cardName),
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
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
private fun EmptyListState(onAddCard: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 60.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.manage_cards_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.manage_cards_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        BillFolderPrimaryButton(
            text = stringResource(R.string.common_add),
            onClick = onAddCard,
            modifier = Modifier.fillMaxWidth(fraction = 0.7f),
        )
    }
}
