package com.billfolder.android.ui.screens.savings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.billfolder.android.R
import com.billfolder.android.data.dto.SavingsTransactionResponse
import com.billfolder.android.data.dto.SavingsTransactionTypes
import com.billfolder.android.ui.theme.BfBrandBill
import com.billfolder.android.ui.theme.MoneyRow
import com.billfolder.android.ui.util.formatBrl
import com.billfolder.android.ui.util.formatShortDate

/**
 * Linha de movimentação de poupança. Renderiza os 5 tipos
 * (deposit/withdrawal/yield/transferOut/transferIn) — mesmo que a UI
 * de create/edit em Fase B só ofereça os 3 primeiros, o read precisa
 * exibir todos.
 *
 * Layout:
 *  - Title: label da movimentação se existir, senão o nome do tipo
 *    (ex: "Salário poupado" se label preenchido, ou "depósito" se null).
 *  - Subtitle: tipo + data, formato "{tipo} · {data}". Quando o tipo
 *    já virou title (label vazio), o subtitle vira só a data.
 *  - Amount à direita com sinal e cor:
 *      Deposit / Yield / TransferIn  → "+R$ X" em verde brand
 *      Withdrawal / TransferOut      → "−R$ X" em error
 *
 * O `amount` no DTO é sempre positivo (convenção do backend); a UI
 * aplica sinal via SavingsTransactionTypes.isInflow.
 */
@Composable
fun SavingsTransactionRow(
    transaction: SavingsTransactionResponse,
    modifier: Modifier = Modifier,
) {
    val typeLabel = transactionTypeLabel(transaction.type)
    val title = transaction.label?.takeIf { it.isNotBlank() } ?: typeLabel
    val subtitle = if (transaction.label.isNullOrBlank()) {
        // tipo já virou title — subtitle = só data
        formatShortDate(transaction.date)
    } else {
        stringResource(
            R.string.savings_transaction_subtitle_format,
            typeLabel,
            formatShortDate(transaction.date),
        )
    }

    val isInflow = SavingsTransactionTypes.isInflow(transaction.type)
    val amountText = formatSignedBrl(transaction.amount, isInflow)
    val amountColor = if (isInflow) BfBrandBill else MaterialTheme.colorScheme.error

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
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = amountText,
                style = MoneyRow,
                color = amountColor,
            )
        }
    }
}

/**
 * Mapeia o type string do backend pro label localizado em PT.
 * Fallback genérico pra defender contra valores novos eventualmente
 * adicionados ao enum no backend antes do app subir junto.
 */
@Composable
private fun transactionTypeLabel(type: String): String = when (type) {
    SavingsTransactionTypes.DEPOSIT      -> stringResource(R.string.savings_transaction_type_deposit)
    SavingsTransactionTypes.WITHDRAWAL   -> stringResource(R.string.savings_transaction_type_withdrawal)
    SavingsTransactionTypes.YIELD        -> stringResource(R.string.savings_transaction_type_yield)
    SavingsTransactionTypes.TRANSFER_OUT -> stringResource(R.string.savings_transaction_type_transfer_out)
    SavingsTransactionTypes.TRANSFER_IN  -> stringResource(R.string.savings_transaction_type_transfer_in)
    else                                 -> type
}

/**
 * "1234.5" + true → "+R$ 1.234,50"; false → "−R$ 1.234,50".
 *
 * Usa o minus sign Unicode (U+2212) em vez do hífen ASCII pra ficar
 * tipograficamente alinhado com o "+" e melhorar leitura. Mesma
 * convenção das telas de extrato em apps de banco BR.
 */
private fun formatSignedBrl(amount: Double, isInflow: Boolean): String {
    val sign = if (isInflow) "+" else "−" // minus sign U+2212
    return "$sign${formatBrl(amount)}"
}
