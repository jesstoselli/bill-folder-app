package com.billfolder.android.ui.screens.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.billfolder.android.R
import com.billfolder.android.data.dto.HomeCategoryBreakdownDto
import com.billfolder.android.ui.theme.MoneyRow
import com.billfolder.android.ui.util.formatBrl

/**
 * Card "para onde vai o dinheiro?" — colapsável.
 *
 * Decisões:
 *  - Default fechado: o usuário só vê o título + chevron. Clicando no
 *    header expande pra mostrar donut + legenda. Estado preservado em
 *    rotação/recomposição via rememberSaveable.
 *  - Donut chart com 6 cores fixas atribuídas por ordem do ranking
 *    (slice 0 = mais saturada). Top 6 categorias visíveis; resto agrupado
 *    em "outros" com cor neutra reservada.
 */
@Composable
fun WhereMoneyGoingCard(
    breakdown: List<HomeCategoryBreakdownDto>,
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "chevron-rotation",
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Header(
                isExpanded = isExpanded,
                chevronRotation = rotation,
                onToggle = { isExpanded = !isExpanded },
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(durationMillis = 200)) +
                    expandVertically(animationSpec = tween(durationMillis = 250)),
                exit = fadeOut(animationSpec = tween(durationMillis = 150)) +
                    shrinkVertically(animationSpec = tween(durationMillis = 200)),
            ) {
                Column {
                    Spacer(Modifier.height(20.dp))
                    if (breakdown.isEmpty()) {
                        EmptyState()
                    } else {
                        val othersLabel = stringResource(R.string.home_money_going_others)
                        ChartContent(
                            breakdown = breakdown,
                            othersLabel = othersLabel,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(
    isExpanded: Boolean,
    chevronRotation: Float,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.home_money_going_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = if (isExpanded) {
                stringResource(R.string.common_close)
            } else {
                stringResource(R.string.common_add)
            },
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.rotate(chevronRotation),
        )
    }
}

@Composable
private fun EmptyState() {
    Text(
        text = stringResource(R.string.home_money_going_empty),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
    )
}

@Composable
private fun ChartContent(
    breakdown: List<HomeCategoryBreakdownDto>,
    othersLabel: String,
) {
    val total = breakdown.sumOf { it.amount }
    val slices = remember(breakdown, othersLabel) { buildSlices(breakdown, othersLabel) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center,
    ) {
        DonutChart(slices = slices, total = total)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.home_money_going_total),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatBrl(total),
                style = MoneyRow,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }

    Spacer(Modifier.height(16.dp))

    slices.forEach { slice ->
        LegendRow(slice = slice)
    }
}

@Composable
private fun DonutChart(slices: List<ChartSlice>, total: Double) {
    val strokeWidthDp = 28.dp
    Canvas(modifier = Modifier.size(160.dp)) {
        val strokePx = strokeWidthDp.toPx()
        val arcSize = Size(size.width - strokePx, size.height - strokePx)
        val topLeft = Offset(strokePx / 2, strokePx / 2)

        var startAngle = -90f
        slices.forEach { slice ->
            val sweep = ((slice.amount / total) * 360.0).toFloat()
            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx),
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun LegendRow(slice: ChartSlice) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color = slice.color, shape = CircleShape),
        )
        Spacer(Modifier.size(10.dp))
        Text(
            text = slice.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatBrl(slice.amount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private data class ChartSlice(
    val label: String,
    val amount: Double,
    val color: Color,
)

/**
 * Top 6 + "outros". Cor é decidida pelo INDEX (não pela categoria) —
 * primeira slice usa BfChart1, segunda BfChart2, …, sétima usa cinza
 * claro reservado.
 */
private fun buildSlices(
    breakdown: List<HomeCategoryBreakdownDto>,
    othersLabel: String,
): List<ChartSlice> {
    val visibleCount = 6
    if (breakdown.size <= visibleCount) {
        return breakdown.mapIndexed { index, item ->
            ChartSlice(
                label = item.categoryName,
                amount = item.amount,
                color = colorForRank(index),
            )
        }
    }

    val top = breakdown.take(visibleCount).mapIndexed { index, item ->
        ChartSlice(
            label = item.categoryName,
            amount = item.amount,
            color = colorForRank(index),
        )
    }
    val othersAmount = breakdown.drop(visibleCount).sumOf { it.amount }
    return top + ChartSlice(
        label = othersLabel,
        amount = othersAmount,
        color = OthersChartColor,
    )
}
