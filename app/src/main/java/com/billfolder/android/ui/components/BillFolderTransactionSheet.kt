package com.billfolder.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.billfolder.android.R

/**
 * Sheet genérico de transação. Único container pra TODOS os formulários
 * de "nova X" (avulsa, despesa, recebimento, cartão, poupança).
 *
 * Layout vertical:
 *  ┌─────────────────────────────────────┐
 *  │   ▔▔▔        (drag handle)          │  ← do M3
 *  │  título           ✕                  │  ← header (this composable)
 *  │  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
 *  │  [conteúdo]                         │  ← slot `content`
 *  │  ...                                │
 *  │  [erro inline opcional]             │  ← se tiver
 *  │  [CTA pílula]                       │  ← slot `footer`
 *  └─────────────────────────────────────┘
 *
 * Decisões:
 *  - Conteúdo é scrollable: forms longos (5-7 campos) cabem em telas
 *    pequenas sem cortar o CTA.
 *  - Loading state: quando `isSaving=true`, sobrepõe conteúdo com spinner
 *    e desabilita interações no caller (pelo lambda do CTA).
 *  - Erro: mensagem inline acima do CTA, em error color.
 *  - Dismiss: tap fora, drag pra baixo OU ✕ no header.
 *  - Shape: extraLarge (28dp) só nos cantos superiores — convenção M3.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillFolderTransactionSheet(
    title: String,
    onDismiss: () -> Unit,
    isSaving: Boolean = false,
    errorMessage: String? = null,
    footer: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        properties = ModalBottomSheetProperties(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp)
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            // Header
            SheetHeader(title = title, onClose = onDismiss)

            Spacer(Modifier.height(20.dp))

            // Conteúdo scrollable. weight(1f, fill = false) é ESSENCIAL: limita
            // esta Column ao espaço que sobra (após header + erro + footer) em vez
            // de crescer até a altura intrínseca do conteúdo. Sem isso, um form alto
            // estoura a altura do sheet e empurra o footer (CTA) pra fora da tela,
            // sem rolar. `fill = false` mantém o sheet curto pra forms pequenos —
            // só ocupa o necessário; quando o conteúdo passa do disponível, rola.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                content()
            }

            if (errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(20.dp))

            // Footer (CTA)
            if (isSaving) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(28.dp),
                )
            } else {
                footer()
            }
        }
    }
}

@Composable
private fun SheetHeader(title: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.sheet_close_content_description),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
