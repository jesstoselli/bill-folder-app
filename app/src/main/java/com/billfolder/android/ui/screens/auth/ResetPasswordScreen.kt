package com.billfolder.android.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
 * "Redefinir senha" — passo 2 de 2. Recebe email da tela anterior (via
 * SavedStateHandle no VM), pede código + nova senha + confirmação.
 * Em sucesso, navega pra Login com a expectativa do user re-logar com a
 * senha nova.
 *
 * Campo do email é readonly — user chegou aqui pelo fluxo, não faz
 * sentido re-editar. Tem CTA "recomeçar" pra voltar pra ForgotPassword
 * caso ele queira trocar o email ou não recebeu o código.
 */
@Composable
fun ResetPasswordScreen(
    onPasswordReset: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ResetPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        if (state is ResetPasswordUiState.Success) onPasswordReset()
    }

    ResetPasswordContent(
        email = viewModel.email,
        code = code,
        newPassword = newPassword,
        confirmPassword = confirmPassword,
        onCodeChange = {
            // Limita a 6 dígitos e só permite números — evita atrito
            // no keyboard e faz auto-submit visual (input "cheio").
            if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                code = it
                viewModel.consumeError()
            }
        },
        onNewPasswordChange = {
            newPassword = it
            viewModel.consumeError()
        },
        onConfirmPasswordChange = {
            confirmPassword = it
            viewModel.consumeError()
        },
        onSubmit = { viewModel.submit(code, newPassword, confirmPassword) },
        onNavigateBack = onNavigateBack,
        state = state,
    )
}

@Composable
private fun ResetPasswordContent(
    email: String,
    code: String,
    newPassword: String,
    confirmPassword: String,
    onCodeChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onNavigateBack: () -> Unit,
    state: ResetPasswordUiState,
) {
    val isSubmitting = state is ResetPasswordUiState.Submitting
    val errorMessage = (state as? ResetPasswordUiState.Error)?.message

    Box(
        modifier = Modifier
            .fillMaxSize()
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
                text = stringResource(R.string.auth_reset_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.auth_reset_subtitle, email),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(40.dp))

            BillFolderTextField(
                value = code,
                onValueChange = onCodeChange,
                label = stringResource(R.string.auth_reset_field_code),
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Next,
                enabled = !isSubmitting,
            )

            Spacer(Modifier.height(16.dp))

            BillFolderTextField(
                value = newPassword,
                onValueChange = onNewPasswordChange,
                label = stringResource(R.string.auth_reset_field_new_password),
                isPassword = true,
                imeAction = ImeAction.Next,
                enabled = !isSubmitting,
            )

            Spacer(Modifier.height(16.dp))

            BillFolderTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = stringResource(R.string.auth_reset_field_confirm_password),
                isPassword = true,
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
                text = stringResource(R.string.auth_reset_cta),
                onClick = onSubmit,
                loading = isSubmitting,
            )

            Spacer(Modifier.height(20.dp))

            RestartLink(
                onClick = onNavigateBack,
                enabled = !isSubmitting,
            )
        }
    }
}

@Composable
private fun RestartLink(onClick: () -> Unit, enabled: Boolean) {
    val prefix = stringResource(R.string.auth_reset_restart_prefix)
    val link   = stringResource(R.string.auth_reset_restart_link)
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
