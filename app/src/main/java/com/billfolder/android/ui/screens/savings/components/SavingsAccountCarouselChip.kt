package com.billfolder.android.ui.screens.savings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.billfolder.android.data.dto.SavingsAccountResponse
import com.billfolder.android.ui.theme.BfBrandBill
import com.billfolder.android.ui.theme.DarkOnPrimary

/**
 * Chip de poupança pro carousel da SavingsScreen. Selecionado fica em
 * verde brand (BfBrandBill); não selecionado é outline discreto. Mesmo
 * molde de CardCarouselChip — varia só o label (bankName em vez de
 * nome do cartão).
 *
 * Optei por mostrar só o bankName no chip (sem agência/conta) pra não
 * estourar a largura quando o usuário tem 2-3 poupanças. Detalhe completo
 * (banco · agência · conta) já aparece no row da ManageSavingsScreen e na
 * sheet de criação. Aqui o chip é só pra TROCAR de poupança no carousel.
 */
@Composable
fun SavingsAccountCarouselChip(
    account: SavingsAccountResponse,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = account.bankName,
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
