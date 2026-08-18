package com.messledger.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(viewModel: AuthViewModel) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        if (uiState.error != null) {
            Text(
                text = uiState.error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        OutlinedTextField(
            value = username,
            onValueChange = { username = it; viewModel.clearError() },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; viewModel.clearError() },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                val description = if (passwordVisible) "Hide password" else "Show password"
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.login(username, password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = username.isNotBlank() && password.isNotBlank() && !uiState.isLoading,
            shape = MaterialTheme.shapes.medium
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Log In")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { showForgotPasswordDialog = true },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Forgot password?")
        }

        if (showForgotPasswordDialog) {
            AlertDialog(
                onDismissRequest = { showForgotPasswordDialog = false },
                title = { Text("Forgot Password") },
                text = { Text("Contact your mess manager to reset your password.") },
                confirmButton = {
                    TextButton(onClick = { showForgotPasswordDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}
