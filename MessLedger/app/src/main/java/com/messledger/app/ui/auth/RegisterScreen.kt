package com.messledger.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
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
fun RegisterScreen(viewModel: AuthViewModel) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    val hasMinLength = password.length >= 8
    val hasLetter = password.any { it.isLetter() }
    val hasNumber = password.any { it.isDigit() }
    val passwordsMatch = password == confirmPassword && password.isNotEmpty()
    val isValidUsername = username.matches(Regex("^[a-z0-9_]{3,20}$"))

    val canRegister = name.isNotBlank() && isValidUsername && uiState.isUsernameAvailable != false &&
            hasMinLength && hasLetter && hasNumber && passwordsMatch && !uiState.isLoading

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
    ) {
        if (uiState.error != null) {
            Text(
                text = uiState.error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Display Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { 
                username = it
                if (it.matches(Regex("^[a-z0-9_]{0,20}$"))) {
                    viewModel.checkUsernameAvailability(it)
                }
            },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = username.isNotEmpty() && (!isValidUsername || uiState.isUsernameAvailable == false),
            trailingIcon = {
                when {
                    uiState.isCheckingUsername -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    uiState.isUsernameAvailable == true -> Icon(Icons.Filled.Check, contentDescription = "Available", tint = MaterialTheme.colorScheme.primary)
                    uiState.isUsernameAvailable == false -> Icon(Icons.Filled.Clear, contentDescription = "Taken", tint = MaterialTheme.colorScheme.error)
                }
            }
        )
        Text(
            text = "3-20 chars, lowercase letters, numbers, underscore only",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = null)
                }
            }
        )

        Column(modifier = Modifier.padding(start = 16.dp, top = 4.dp)) {
            ValidationRow(text = "At least 8 characters", isValid = hasMinLength)
            ValidationRow(text = "Contains a letter", isValid = hasLetter)
            ValidationRow(text = "Contains a number", isValid = hasNumber)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(imageVector = image, contentDescription = null)
                }
            }
        )

        if (confirmPassword.isNotEmpty() && !passwordsMatch) {
            Text(
                text = "Passwords do not match",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.register(name, username, password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = canRegister,
            shape = MaterialTheme.shapes.medium
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Create Account")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ValidationRow(text: String, isValid: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (isValid) Icons.Filled.Check else Icons.Filled.Clear,
            contentDescription = null,
            tint = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (isValid) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
