package com.billfolder.android.ui.screens.savings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.billfolder.android.R

/**
 * Chip "+" nova poupança pro carousel da SavingsScreen. SuggestionChip
 * (não FilterChip) porque é uma ação, não um filtro selecionável. Mesma
 * convenção do AddCardChip da CardsScreen.
 *
 * Tap abre AddSavingsAccountSheet em modo create, igual o "+ novo" do
 * carousel de cards.
 */
@Composable
fun AddSavingsChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SuggestionChip(
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.savings_chip_add_new_label),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        modifier = modifier.padding(end = 8.dp),
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = Color.Transparent,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            iconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    )
}
