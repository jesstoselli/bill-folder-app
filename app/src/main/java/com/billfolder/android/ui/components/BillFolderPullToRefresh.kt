package com.billfolder.android.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Wrapper de PullToRefreshBox com o styling do BillFolder (indicator na
 * cor primária, container transparente pra caber sobre o fundo escuro
 * da tela).
 *
 * Uso típico numa Screen com CycleNavigator:
 *
 *   BillFolderPullToRefresh(
 *       isRefreshing = state.isRefreshing,
 *       onRefresh = viewModel::pullRefresh,
 *   ) {
 *       LazyColumn { ... }
 *   }
 *
 * A VM deve expor `isRefreshing: Boolean` no state e um método
 * `pullRefresh()` que faz o reload SEM flipar a tela pra Loading — pull
 * é um refresh sutil, sem apagar dados existentes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillFolderPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val pullState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = modifier.fillMaxSize(),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        content = { content() },
    )
}
