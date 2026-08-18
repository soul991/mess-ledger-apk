package com.messledger.app.ui.requests

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.messledger.app.ui.components.ErrorDialog
import com.messledger.app.ui.components.LoadingOverlay
import com.messledger.app.ui.components.MessLedgerTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestStatusScreen(
    onNavigateBack: () -> Unit,
    viewModel: RequestsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var leaveReason by remember { mutableStateOf("") }

    if (uiState.error != null) {
        ErrorDialog(
            message = uiState.error!!,
            onDismiss = viewModel::clearError
        )
    }

    Scaffold(
        topBar = {
            MessLedgerTopBar(
                title = "Leave Status",
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val myRequest = uiState.myLeaveRequest

                if (myRequest != null) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Current Request Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Status: ${myRequest.status}")
                            if (!myRequest.reason.isNullOrBlank()) {
                                Text(text = "Reason: ${myRequest.reason}")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (myRequest.status == "PENDING") {
                                Button(
                                    onClick = { viewModel.withdrawLeaveRequest() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Withdraw Request")
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Submit a request to leave the mess. Manager approval is required.",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    OutlinedTextField(
                        value = leaveReason,
                        onValueChange = { leaveReason = it },
                        label = { Text("Reason (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    Button(
                        onClick = { viewModel.submitLeaveRequest(leaveReason.ifBlank { null }) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Submit Leave Request")
                    }
                }
            }

            if (uiState.isLoading || uiState.isProcessing) {
                LoadingOverlay()
            }
        }
    }
}
