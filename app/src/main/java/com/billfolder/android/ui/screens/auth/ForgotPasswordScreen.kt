package com.billfolder.android.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.billfolder.android.R
import com.billfolder.android.ui.components.BillFolderPrimaryButton
import com.billfolder.android.ui.components.BillFolderTextField

/**
 * "Esqueci minha senha" — passo 1 de 2. Recebe o email do user, dispara
 * o envio do código e navega pra ResetPasswordScreen passando o email
 * como argumento pro passo 2 não pedir de novo.
 *
 * Não revela se o email existe — o VM sempre marca Sent em qualquer 200
 * do backend, e a navegação segue mesmo se o email for inválido no banco.
 * A validação real do código acontece no reset-password (que retorna
 * "código inválido" tanto pra código errado quanto pra email inexistente).
 */
@Composable
fun ForgotPasswordScreen(
    onCodeSent: (email: String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    var email by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        if (state is ForgotPasswordUiState.Sent) {
            onCodeSent(email.trim())
        }
    }

    ForgotPasswordContent(
        email = email,
        onEmailChange = {
            email = it
            viewModel.consumeError()
        },
        onSubmit = { viewModel.submit(email) },
        onNavigateBack = onNavigateBack,
        state = state,
    )
}

@Composable
private fun ForgotPasswordContent(
    email: String,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onNavigateBack: () -> Unit,
    state: ForgotPasswordUiState,
) {
    val isSubmitting = state is ForgotPasswordUiState.Submitting
    val errorMessage = (state as? ForgotPasswordUiState.Error)?.message

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_billfolder),
                contentDescription = stringResource(R.string.app_logo_content_description),
                modifier = Modifier.width(220.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.auth_forgot_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.auth_forgot_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(40.dp))

            BillFolderTextField(
                value = email,
                onValueChange = onEmailChange,
                label = stringResource(R.string.auth_email),
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
                enabled = !isSubmitting,
            )

            if (errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            Spacer(Modifier.height(28.dp))

            BillFolderPrimaryButton(
                text = stringResource(R.string.auth_forgot_cta),
                onClick = onSubmit,
                loading = isSubmitting,
            )

            Spacer(Modifier.height(20.dp))

            BackToLoginLink(
                onClick = onNavigateBack,
                enabled = !isSubmitting,
            )
        }
    }
}

@Composable
private fun BackToLoginLink(onClick: () -> Unit, enabled: Boolean) {
    val prefix = stringResource(R.string.auth_forgot_back_prefix)
    val link   = stringResource(R.string.auth_forgot_back_link)
    val annotated: AnnotatedString = buildAnnotatedString {
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
            append("$prefix ")
        }
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
            append(link)
        }
    }

    ClickableText(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
        onClick = { if (enabled) onClick() },
    )
}
