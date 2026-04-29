package com.billfolder.android.ui.screens.cards.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.billfolder.android.data.dto.CreditCardAccountResponse

/**
 * Wrapper de CreditCardRow com swipe-to-delete (left).
 *
 * Comportamento:
 *  - Swipe da direita pra esquerda revela um background vermelho com
 *    ícone de lixeira.
 *  - Quando o swipe completa o threshold (~50%), dispara `onSwipeToDelete`
 *    e o caller mostra o dialog de confirmação.
 *  - Se o user cancelar no dialog, chamamos `state.reset()` no LaunchedEffect
 *    pra trazer a row de volta pra posição original.
 *
 * pendingDeleteForThis: se igual ao card atual, significa que o dialog
 * tá aberto pedindo confirmação. Se mudar pra null (cancel) ou pra outro,
 * resetamos o swipe.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableCreditCardRow(
    card: CreditCardAccountResponse,
    pendingDelete: CreditCardAccountResponse?,
    onSwipeToDelete: (CreditCardAccountResponse) -> Unit,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val swipeState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            // Só aceita swipe-to-end-start (pra esquerda).
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onSwipeToDelete(card)
                // Retorna false: não DESCARTA visualmente — só pede
                // confirmação. Se confirmar, a remoção da lista virá
                // do VM (a row some porque sai do `cards`).
                false
            } else {
                false
            }
        },
        // Threshold mais alto pra evitar swipe acidental (~50% da largura)
        positionalThreshold = { totalDistance -> totalDistance * 0.5f },
    )

    // Se o user cancelar o dialog (pendingDelete vira null pra esse card),
    // ou se um swipe foi iniciado mas não disparou o callback, reseta a
    // posição visual da row.
    LaunchedEffect(pendingDelete) {
        if (pendingDelete?.id != card.id) {
            swipeState.reset()
        }
    }

    SwipeToDismissBox(
        state = swipeState,
        modifier = modifier,
        backgroundContent = { DeleteBackground() },
        // Só permite swipe pra esquerda
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
    ) {
        CreditCardRow(card = card, onClick = onClick)
    }
}

@Composable
private fun DeleteBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.large,
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}
