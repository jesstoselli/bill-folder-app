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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.billfolder.android.R
import com.billfolder.android.ui.components.BillFolderPrimaryButton
import com.billfolder.android.ui.components.BillFolderTextField

@Composable
fun SignupScreen(
    onSignupSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        if (state is AuthUiState.Success) onSignupSuccess()
    }

    SignupContent(
        displayName = displayName,
        email = email,
        password = password,
        onNameChange = {
            displayName = it
            viewModel.consumeError()
        },
        onEmailChange = {
            email = it
            viewModel.consumeError()
        },
        onPasswordChange = {
            password = it
            viewModel.consumeError()
        },
        onSubmit = { viewModel.signup(email, password, displayName) },
        onNavigateToLogin = onNavigateToLogin,
        state = state,
    )
}

@Composable
private fun SignupContent(
    displayName: String,
    email: String,
    password: String,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onNavigateToLogin: () -> Unit,
    state: AuthUiState,
) {
    val isSubmitting = state is AuthUiState.Submitting
    val errorMessage = (state as? AuthUiState.Error)?.message

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
                contentDescription = "BillFolder",
                modifier = Modifier.width(220.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Crie sua conta",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(Modifier.height(40.dp))

            BillFolderTextField(
                value = displayName,
                onValueChange = onNameChange,
                label = "Nome",
                imeAction = ImeAction.Next,
                enabled = !isSubmitting,
            )

            Spacer(Modifier.height(16.dp))

            BillFolderTextField(
                value = email,
                onValueChange = onEmailChange,
                label = "E-mail",
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                enabled = !isSubmitting,
            )

            Spacer(Modifier.height(16.dp))

            BillFolderTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = "Senha (mínimo 8 caracteres)",
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
                text = "Criar conta",
                onClick = onSubmit,
                loading = isSubmitting,
            )

            Spacer(Modifier.height(20.dp))

            LoginLink(
                onClick = onNavigateToLogin,
                enabled = !isSubmitting,
            )
        }
    }
}

@Composable
private fun LoginLink(onClick: () -> Unit, enabled: Boolean) {
    val annotated: AnnotatedString = buildAnnotatedString {
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
            append("Já tem conta? ")
        }
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
            append("Entrar")
        }
    }

    ClickableText(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
        onClick = { if (enabled) onClick() },
    )
}
