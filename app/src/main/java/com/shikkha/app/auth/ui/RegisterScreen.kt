package com.shikkha.app.auth.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shikkha.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onEmailConfirmationNotice: () -> Unit,
    onNavigateLogin: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    if (viewModel.registered) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onRegistered() }
    }
    if (viewModel.emailConfirmationRequired) {
        AlertDialog(
            onDismissRequest = onEmailConfirmationNotice,
            confirmButton = {
                TextButton(onClick = onEmailConfirmationNotice) { Text(stringResource(R.string.login_title)) }
            },
            title = { Text(stringResource(R.string.register_title)) },
            text = { Text(stringResource(R.string.email_confirmation_sent)) },
        )
    }

    AuthScaffold(title = stringResource(R.string.register_title)) {
        AuthTextField(viewModel.fullName, { viewModel.fullName = it }, stringResource(R.string.full_name_label))
        Spacer(Modifier.height(12.dp))
        AuthTextField(viewModel.username, { viewModel.username = it }, stringResource(R.string.username_label))
        Spacer(Modifier.height(12.dp))
        AuthTextField(
            viewModel.email, { viewModel.email = it }, stringResource(R.string.email_label),
            keyboardType = KeyboardType.Email,
        )
        Spacer(Modifier.height(12.dp))
        AuthTextField(
            viewModel.password, { viewModel.password = it }, stringResource(R.string.password_label),
            keyboardType = KeyboardType.Password,
            visualTransformation = PasswordVisualTransformation(),
        )
        Spacer(Modifier.height(12.dp))

        // Class dropdown (grades 6-10 per NCTB secondary scope)
        var classExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = classExpanded, onExpandedChange = { classExpanded = it }) {
            OutlinedTextField(
                value = viewModel.classLevel.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.class_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(classExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(expanded = classExpanded, onDismissRequest = { classExpanded = false }) {
                (6..10).forEach { level ->
                    DropdownMenuItem(
                        text = { Text(level.toString()) },
                        onClick = { viewModel.classLevel = level; classExpanded = false },
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // Medium dropdown
        var mediumExpanded by remember { mutableStateOf(false) }
        val mediumLabel = if (viewModel.medium == "bangla") {
            stringResource(R.string.medium_bangla)
        } else {
            stringResource(R.string.medium_english)
        }
        ExposedDropdownMenuBox(expanded = mediumExpanded, onExpandedChange = { mediumExpanded = it }) {
            OutlinedTextField(
                value = mediumLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.medium_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(mediumExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(expanded = mediumExpanded, onDismissRequest = { mediumExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.medium_bangla)) },
                    onClick = { viewModel.medium = "bangla"; mediumExpanded = false },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.medium_english)) },
                    onClick = { viewModel.medium = "english"; mediumExpanded = false },
                )
            }
        }

        viewModel.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { viewModel.register() },
            enabled = !viewModel.loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (viewModel.loading) stringResource(R.string.loading) else stringResource(R.string.register_button))
        }
        TextButton(onClick = onNavigateLogin, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.go_to_login))
        }
    }
}

@Composable
internal fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        modifier = modifier.fillMaxWidth(),
    )
}
