package com.billfolder.android.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.billfolder.android.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * PLACEHOLDER. Vai ser substituído pela Home real (hero card + listas)
 * na §7 do plano. Por ora, prova que o fluxo de login funciona end-to-end
 * e oferece um botão de logout pra fechar o ciclo.
 */
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    viewModel: HomePlaceholderViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Você está logado",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "A Home real chega na próxima etapa.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(36.dp))
            OutlinedButton(
                onClick = {
                    scope.launch {
                        viewModel.logout()
                        onLogout()
                    }
                },
            ) {
                Text("Sair")
            }
        }
    }
}

@HiltViewModel
class HomePlaceholderViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    suspend fun logout() = authRepository.logout()
}
