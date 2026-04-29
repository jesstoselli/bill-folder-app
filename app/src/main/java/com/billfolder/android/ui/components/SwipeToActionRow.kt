package com.billfolder.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Wrapper genérico que adiciona swipe-to-delete (esquerda) e
 * swipe-to-edit (direita, opcional) numa row qualquer do app.
 *
 * Padrão "confirm sem dismiss visual": ambos os swipes não removem
 * a row visualmente — apenas disparam callbacks. O caller costuma
 * abrir um AlertDialog (delete) ou um ModalBottomSheet (edit), e
 * mantém um booleano `isPending` enquanto a ação está sendo confirmada.
 * Quando o pending acaba (cancel ou confirm), `isPending = false`
 * faz a row resetar pra posição original via LaunchedEffect.
 *
 * Direções e backgrounds:
 *  - StartToEnd (esquerda → direita) revela `primaryContainer` com
 *    ícone Edit. Disponível só quando `onEdit != null`; caso contrário
 *    o swipe pra direita fica desabilitado.
 *  - EndToStart (direita → esquerda) revela `errorContainer` com ícone
 *    Delete. Sempre habilitado.
 *
 * Threshold em 50% da largura pra evitar swipe acidental.
 *
 * Composição típica:
 * ```
 * SwipeToActionRow(
 *     isPending = vmState.pendingDelete?.id == item.id ||
 *                 vmState.editing?.id == item.id,
 *     onDelete = { vm.requestDelete(item) },
 *     onEdit = { vm.requestEdit(item) },
 * ) {
 *     ItemRow(item = item, onClick = { ... })
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToActionRow(
    isPending: Boolean,
    onDelete: () -> Unit,
    onEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    backgroundShape: Shape = MaterialTheme.shapes.large,
    content: @Composable () -> Unit,
) {
    val swipeState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    // Não DESCARTA visualmente — só pede confirmação.
                    // Quando isPending virar false, o LaunchedEffect reseta.
                    false
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onEdit?.invoke()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.5f },
    )

    LaunchedEffect(isPending) {
        if (!isPending) {
            swipeState.reset()
        }
    }

    SwipeToDismissBox(
        state = swipeState,
        modifier = modifier,
        backgroundContent = {
            SwipeBackground(
                direction = swipeState.dismissDirection,
                hasEdit = onEdit != null,
                shape = backgroundShape,
            )
        },
        // Direita só habilita se houver onEdit. Esquerda sempre habilitada.
        enableDismissFromStartToEnd = onEdit != null,
        enableDismissFromEndToStart = true,
        content = { content() },
    )
}

/**
 * Background renderizado atrás da row enquanto o user arrasta. Mostra
 * o ícone do lado oposto da direção do swipe (delete na ponta direita
 * porque o user puxa pra esquerda; edit na ponta esquerda porque o
 * user puxa pra direita). Em Settled (sem swipe), retorna um Spacer
 * invisível pra não vazar cor por baixo da row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeBackground(
    direction: SwipeToDismissBoxValue,
    hasEdit: Boolean,
    shape: Shape,
) {
    when (direction) {
        SwipeToDismissBoxValue.EndToStart -> SwipeBackgroundContent(
            color = MaterialTheme.colorScheme.errorContainer,
            iconColor = MaterialTheme.colorScheme.onErrorContainer,
            icon = Icons.Default.Delete,
            alignment = Alignment.CenterEnd,
            shape = shape,
        )
        SwipeToDismissBoxValue.StartToEnd -> {
            if (hasEdit) {
                SwipeBackgroundContent(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    icon = Icons.Default.Edit,
                    alignment = Alignment.CenterStart,
                    shape = shape,
                )
            } else {
                Spacer(Modifier.fillMaxSize())
            }
        }
        SwipeToDismissBoxValue.Settled -> Spacer(Modifier.fillMaxSize())
    }
}

@Composable
private fun SwipeBackgroundContent(
    color: Color,
    iconColor: Color,
    icon: ImageVector,
    alignment: Alignment,
    shape: Shape,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = color, shape = shape)
            .padding(horizontal = 24.dp),
        contentAlignment = alignment,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // ação é comunicada pela row em si
            tint = iconColor,
        )
    }
}
