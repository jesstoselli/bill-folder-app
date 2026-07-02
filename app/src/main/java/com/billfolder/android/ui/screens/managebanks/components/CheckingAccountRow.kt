package com.billfolder.android.ui.screens.managebanks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.billfolder.android.R
import com.billfolder.android.data.dto.CheckingAccountResponse
import com.billfolder.android.ui.theme.PillShape
import com.billfolder.android.ui.util.formatBrl

/**
 * Linha de conta corrente na ManageBanksScreen. Mostra banco + agência/
 * conta + saldo inicial + chip "principal" quando isPrimary.
 */
@Composable
fun CheckingAccountRow(
    account: CheckingAccountResponse,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = account.bankName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (account.isPrimary) {
                        Spacer(Modifier.width(8.dp))
                        PrimaryChip()
                    }
                }
                if (!account.branch.isNullOrBlank() || !account.accountNumber.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    val branch = account.branch.orEmpty()
                    val number = account.accountNumber.orEmpty()
                    Text(
                        text = stringResource(
                            R.string.checking_account_subtitle_format,
                            branch,
                            number,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.checking_account_initial_balance_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                )
                Text(
                    text = formatBrl(account.initialBalance),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun PrimaryChip() {
    Text(
        text = stringResource(R.string.checking_account_primary_chip),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = PillShape,
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

