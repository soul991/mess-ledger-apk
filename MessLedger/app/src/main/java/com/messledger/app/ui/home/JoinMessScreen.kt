package com.messledger.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.messledger.app.ui.components.MessLedgerTopBar

@Composable
fun JoinMessScreen(
    messId: String?,
    onBack: () -> Unit,
    onSuccess: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    var inputMessId by remember { mutableStateOf(messId ?: "") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            MessLedgerTopBar(
                title = "Join a Mess",
                onBackClick = onBack
            )
        }
    ) { paddingValues ->
        if (isSuccess) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Request Sent!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Your request to join the mess has been sent. Please wait for the manager to approve it.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Back to Home")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp)
            ) {
                Text(
                    text = "Enter the Mess ID provided by your manager to request to join.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                OutlinedTextField(
                    value = inputMessId,
                    onValueChange = { inputMessId = it.uppercase(); errorMessage = null },
                    label = { Text("Mess ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        isSubmitting = true
                        viewModel.submitJoinRequest(
                            messId = inputMessId,
                            onSuccess = {
                                isSubmitting = false
                                isSuccess = true
                            },
                            onError = { err ->
                                isSubmitting = false
                                errorMessage = err
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = inputMessId.isNotBlank() && !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Request to Join")
                    }
                }
            }
        }
    }
}
