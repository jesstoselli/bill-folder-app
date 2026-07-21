package com.billfolder.android.ui.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.billfolder.android.R
import com.billfolder.android.ui.theme.BfBrandBill
import com.billfolder.android.ui.theme.DarkOnPrimary

/** As três seções da lista da Home, expostas como abas. */
enum class HomeSection { Upcoming, Recent, Overdue }

/**
 * Barra de abas da Home: próximos | últimas | atrasadas. Usa o mesmo
 * FilterChip do carousel de cartão (CardCarouselChip) — verde brand quando
 * selecionado, outline discreto quando não — pra ficar compacto e consistente
 * com o resto do app. A aba Atrasadas mostra o contador ("atrasadas · N")
 * quando há itens, pra não passar batido.
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
        SectionChip(
            label = stringResource(R.string.home_tab_upcoming),
            selected = selected == HomeSection.Upcoming,
            onClick = { onSelect(HomeSection.Upcoming) },
        )
        SectionChip(
            label = stringResource(R.string.home_tab_recent),
            selected = selected == HomeSection.Recent,
            onClick = { onSelect(HomeSection.Recent) },
        )
        val overdueChip = @Composable {
            SectionChip(
                label = stringResource(R.string.home_tab_overdue),
                selected = selected == HomeSection.Overdue,
                onClick = { onSelect(HomeSection.Overdue) },
            )
        }
        // Contador de atrasadas como badge (bolinha no topo-direito). O Badge
        // do M3 já é vermelho (error), casando com a urgência. Sem itens, a
        // pílula fica igual às outras.
        if (overdueCount > 0) {
            BadgedBox(
                badge = { Badge { Text(overdueCount.toString()) } },
            ) {
                overdueChip()
            }
        } else {
            overdueChip()
        }
    }
}

@Composable
private fun SectionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = BfBrandBill,
            selectedLabelColor = DarkOnPrimary,
        ),
        border = if (selected) {
            null
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
    )
}
