package com.billfolder.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.billfolder.android.R
import com.billfolder.android.ui.theme.PillShape

/**
 * Item do Speed Dial: ícone + label + ação. Cada um vira um mini-FAB
 * com label em pílula à esquerda.
 *
 * Quando `enabled = false`, o item renderiza com opacity reduzida (visual
 * de "atalho indisponível") e o tap vira no-op. Útil pra atalhos que
 * dependem de pré-condição — ex: "poupança" só faz sentido se o user já
 * cadastrou ao menos uma SavingsAccount.
 */
data class SpeedDialItem(
    val label: String,
    val icon: ImageVector,
    val contentDescription: String = label,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
)

/**
 * Speed Dial FAB — main FAB pílula que abre uma stack vertical de mini-FABs
 * com labels. Backdrop com scrim por trás pra dar foco e fechar ao tocar fora.
 *
 * Convenção do DS v0.2: tudo interativo é pílula, FAB principal usa primary.
 * Mini-FABs também usam primary pra reforçar a hierarquia ("são todos atalhos
 * de adicionar"); contraste do texto vem do onPrimary.
 *
 * Animação: stagger de baixo pra cima — primeiro item da lista é o que fica
 * mais próximo do main FAB. Entrada com slide vertical + fade.
 */
@Composable
fun BillFolderSpeedDialFab(
    items: List<SpeedDialItem>,
    modifier: Modifier = Modifier,
) {
    var isOpen by remember { mutableStateOf(false) }

    // Backdrop ocupa a tela inteira pra capturar tap fora; renderizado
    // por baixo do FAB graças à ordem dos elementos no Box pai.
    Box(modifier = modifier.fillMaxSize()) {
        // Scrim — só aparece quando aberto. Captura tap em qualquer lugar fora
        // e fecha. Usa MutableInteractionSource sem ripple pro tap ser invisível.
        AnimatedVisibility(
            visible = isOpen,
            enter = fadeIn(animationSpec = tween(durationMillis = 200)),
            exit = fadeOut(animationSpec = tween(durationMillis = 150)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { isOpen = false },
                    ),
            )
        }

        // Stack do FAB + items à direita-baixo. Items renderizados de cima
        // pra baixo; o último é o main FAB.
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items.forEachIndexed { index, item ->
                AnimatedVisibility(
                    visible = isOpen,
                    enter = slideInVertically(
                        animationSpec = tween(durationMillis = 200, delayMillis = index * 25),
                        initialOffsetY = { it / 2 },
                    ) + fadeIn(animationSpec = tween(durationMillis = 200, delayMillis = index * 25)),
                    exit = slideOutVertically(
                        animationSpec = tween(durationMillis = 150),
                        targetOffsetY = { it / 2 },
                    ) + fadeOut(animationSpec = tween(durationMillis = 150)),
                ) {
                    SpeedDialMiniItem(
                        item = item,
                        onClick = {
                            // Disabled item → tap é no-op. Não fechamos o
                            // Speed Dial: o user vê os outros atalhos
                            // disponíveis e pode escolher outro.
                            if (item.enabled) {
                                isOpen = false
                                item.onClick()
                            }
                        },
                    )
                }
            }

            MainFab(
                isOpen = isOpen,
                onToggle = { isOpen = !isOpen },
            )
        }
    }
}

@Composable
private fun SpeedDialMiniItem(item: SpeedDialItem, onClick: () -> Unit) {
    // Single source of truth pra opacity. M3 não tem token oficial pra
    // "disabled state" do FloatingActionButton (ele não suporta enabled
    // nativamente), então aplicamos via alpha no Modifier do Row inteiro
    // — atinge label E mini-FAB de uma vez. Valor 0.38 vem do M3 spec
    // pra estados disabled (mesmo do TextButton.colors disabled).
    val itemAlpha = if (item.enabled) 1f else 0.38f

    Row(
        modifier = Modifier.alpha(itemAlpha),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Label em pílula surfaceContainerHigh (subtle) à esquerda do mini-FAB
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = PillShape,
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        // Mini-FAB redondo, primary. Tap continua chamando `onClick`
        // mesmo quando disabled — o handler no caller (Speed Dial) é
        // que checa item.enabled antes de propagar. Mantemos assim pra
        // que o tap em item disabled não pareça "morto" (ripple ainda
        // dispara), só não abra a sheet.
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp),
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.contentDescription,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun MainFab(isOpen: Boolean, onToggle: () -> Unit) {
    // FAB redondo nos dois estados — só o ícone muda. Quando fechado:
    // primary (verde de marca), atrai a atenção. Quando aberto: surface
    // alta (cinza), apaga o brilho pra deixar foco nos mini-FABs verdes.
    val containerColor =
        if (isOpen) MaterialTheme.colorScheme.surfaceContainerHigh
        else MaterialTheme.colorScheme.primary
    val contentColor =
        if (isOpen) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onPrimary

    FloatingActionButton(
        onClick = onToggle,
        shape = CircleShape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp),
    ) {
        Icon(
            imageVector = if (isOpen) Icons.Default.Close else Icons.Default.Add,
            contentDescription = stringResource(
                if (isOpen) R.string.common_close else R.string.common_add,
            ),
        )
    }
}
