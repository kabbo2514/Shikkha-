package com.shikkha.app.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shikkha.app.R
import com.shikkha.app.auth.data.RootState

@Composable
fun LoginScreen(
    onLoginResult: (RootState) -> Unit,
    onNavigateRegister: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    LaunchedEffect(viewModel.error) { /* error surfaced below; hook for future snackbar */ }

    AuthScaffold(title = stringResource(R.string.login_title)) {
        AuthTextField(
            value = viewModel.email,
            onValueChange = { viewModel.email = it },
            label = stringResource(R.string.email_label),
            keyboardType = KeyboardType.Email,
        )
        Spacer(Modifier.height(12.dp))
        AuthTextField(
            value = viewModel.password,
            onValueChange = { viewModel.password = it },
            label = stringResource(R.string.password_label),
            keyboardType = KeyboardType.Password,
            visualTransformation = PasswordVisualTransformation(),
        )
        viewModel.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.login_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { viewModel.signIn(onLoginResult) },
            enabled = !viewModel.loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (viewModel.loading) stringResource(R.string.loading) else stringResource(R.string.login_button))
        }
        TextButton(onClick = onNavigateRegister, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.go_to_register))
        }
    }
}
