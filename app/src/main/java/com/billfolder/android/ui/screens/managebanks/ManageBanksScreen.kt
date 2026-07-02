package com.billfolder.android.ui.screens.managebanks

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
import com.billfolder.android.data.dto.CheckingAccountResponse
import com.billfolder.android.ui.components.BillFolderPrimaryButton
import com.billfolder.android.ui.components.SwipeToActionRow
import com.billfolder.android.ui.screens.managebanks.components.CheckingAccountRow
import com.billfolder.android.ui.theme.PillShape

/**
 * Tela "gerenciar > bancos". Mesmo molde de ManageSavings/ManageCards:
 * lista de contas correntes com swipe-left pra deletar, swipe-right pra
 * editar, FAB pra criar.
 *
 * Deletar uma conta: backend faz CASCADE em savings 1:1 vinculada e SET
 * NULL em income entries que apontam pra ela (histórico preservado).
 * Dialog de delete cita isso.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageBanksScreen(
    onMenuClick: () -> Unit,
    viewModel: ManageBanksViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.manage_banks_screen_title),
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
                ManageBanksUiState.Loading -> CenteredLoading()
                is ManageBanksUiState.Error -> ErrorState(
                    message = s.message,
                    onRetry = viewModel::refresh,
                )
                is ManageBanksUiState.Content -> Content(
                    state = s,
                    onAddAccount = { showAddSheet = true },
                    onRequestDelete = viewModel::requestDelete,
                    onRequestEdit = viewModel::requestEdit,
                )
            }
        }

        if (showAddSheet) {
            AddCheckingAccountSheet(
                onDismiss = { showAddSheet = false },
                onSaved = { viewModel.refresh() },
            )
        }

        val current = state
        if (current is ManageBanksUiState.Content && current.pendingDelete != null) {
            DeleteBankDialog(
                bankName = current.pendingDelete.bankName,
                onConfirm = viewModel::confirmDelete,
                onCancel = viewModel::cancelDelete,
            )
        }

        if (current is ManageBanksUiState.Content && current.editing != null) {
            AddCheckingAccountSheet(
                existing = current.editing,
                onDismiss = viewModel::cancelEdit,
                onSaved = {
                    viewModel.cancelEdit()
                    viewModel.refresh()
                },
            )
        }
    }
}

@Composable
private fun Content(
    state: ManageBanksUiState.Content,
    onAddAccount: () -> Unit,
    onRequestDelete: (CheckingAccountResponse) -> Unit,
    onRequestEdit: (CheckingAccountResponse) -> Unit,
) {
    if (state.accounts.isEmpty()) {
        EmptyListState(onAddAccount = onAddAccount)
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(state.accounts, key = { it.id }) { account ->
            SwipeToActionRow(
                isPending = state.pendingDelete?.id == account.id ||
                    state.editing?.id == account.id,
                onDelete = { onRequestDelete(account) },
                onEdit = { onRequestEdit(account) },
            ) {
                CheckingAccountRow(account = account)
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun DeleteBankDialog(
    bankName: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(text = stringResource(R.string.manage_banks_delete_dialog_title))
        },
        text = {
            Text(
                text = stringResource(R.string.manage_banks_delete_dialog_message, bankName),
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
private fun EmptyListState(onAddAccount: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 60.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.manage_banks_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.manage_banks_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        BillFolderPrimaryButton(
            text = stringResource(R.string.common_add),
            onClick = onAddAccount,
            modifier = Modifier.fillMaxWidth(fraction = 0.7f),
        )
    }
}
