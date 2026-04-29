package com.billfolder.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.billfolder.android.ui.util.formatShortDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Campo de data com DatePicker M3. Caller passa `value` como ISO
 * "yyyy-MM-dd" e recebe ISO de volta no callback. Visual interno mostra
 * "29 de abr" (via formatShortDate) — bem mais legível.
 *
 * Implementação: Box com clickable abre o picker. O OutlinedTextField
 * dentro fica em readOnly só pra renderização — Box é quem captura tap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillFolderDateField(
    isoDate: String,
    onIsoDateChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var showPicker by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = { showPicker = true },
            ),
    ) {
        OutlinedTextField(
            value = formatShortDate(isoDate),
            onValueChange = { /* read-only */ },
            label = { Text(label) },
            readOnly = true,
            // Desabilitamos pra não capturar tap — Box pai cuida disso.
            // Mas em readOnly o texto fica visível normalmente.
            enabled = false,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            // Mantém aparência de field ativo mesmo com enabled=false
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor       = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor     = MaterialTheme.colorScheme.outline,
                disabledLabelColor      = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (showPicker) {
        val initialMillis = remember(isoDate) {
            runCatching {
                LocalDate.parse(isoDate)
                    .atStartOfDay()
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()
            }.getOrDefault(System.currentTimeMillis())
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        onIsoDateChange(date.toString()) // "yyyy-MM-dd"
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("cancelar") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
