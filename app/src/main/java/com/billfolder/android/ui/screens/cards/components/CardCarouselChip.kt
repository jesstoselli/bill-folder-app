package com.billfolder.android.ui.screens.cards.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.billfolder.android.data.dto.CreditCardAccountResponse
import com.billfolder.android.ui.theme.BfBrandBill
import com.billfolder.android.ui.theme.DarkOnPrimary

/**
 * Chip de cartão pro carousel da tela "despesas no cartão". Quando
 * selecionado fica em verde brand (BfBrandBill); quando não, é um
 * outline discreto.
 */
@Composable
fun CardCarouselChip(
    card: CreditCardAccountResponse,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = card.name,
                style = MaterialTheme.typography.labelLarge,
            )
        },
        modifier = modifier.padding(end = 8.dp),
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
