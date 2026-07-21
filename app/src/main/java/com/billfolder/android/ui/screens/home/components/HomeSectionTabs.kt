package com.billfolder.android.ui.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.billfolder.android.R
import com.billfolder.android.ui.theme.PillShape

/** As três seções da lista da Home, expostas como abas. */
enum class HomeSection { Upcoming, Recent, Overdue }

/**
 * Barra de 3 abas (pills) da Home: próximos | últimas | atrasadas. A aba
 * Atrasadas destaca a urgência com um contador e cor de erro quando há itens
 * atrasados (é a aba menos proeminente, então o contador evita que passe
 * batido).
 */
@Composable
fun HomeSectionTabs(
    selected: HomeSection,
    onSelect: (HomeSection) -> Unit,
    overdueCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionTab(
            label = stringResource(R.string.home_tab_upcoming),
            selected = selected == HomeSection.Upcoming,
            onClick = { onSelect(HomeSection.Upcoming) },
            modifier = Modifier.weight(1f),
        )
        SectionTab(
            label = stringResource(R.string.home_tab_recent),
            selected = selected == HomeSection.Recent,
            onClick = { onSelect(HomeSection.Recent) },
            modifier = Modifier.weight(1f),
        )
        SectionTab(
            label = if (overdueCount > 0) {
                stringResource(R.string.home_tab_overdue_count, overdueCount)
            } else {
                stringResource(R.string.home_tab_overdue)
            },
            selected = selected == HomeSection.Overdue,
            onClick = { onSelect(HomeSection.Overdue) },
            isAlert = overdueCount > 0,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SectionTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isAlert: Boolean = false,
) {
    val container = when {
        selected && isAlert -> MaterialTheme.colorScheme.errorContainer
        selected            -> MaterialTheme.colorScheme.secondaryContainer
        else                -> MaterialTheme.colorScheme.surfaceContainer
    }
    val content = when {
        selected && isAlert -> MaterialTheme.colorScheme.onErrorContainer
        selected            -> MaterialTheme.colorScheme.onSecondaryContainer
        isAlert             -> MaterialTheme.colorScheme.error
        else                -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        onClick = onClick,
        shape = PillShape,
        color = container,
        contentColor = content,
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
        )
    }
}
