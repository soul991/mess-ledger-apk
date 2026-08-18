package com.messledger.app.ui.requests

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.messledger.app.data.model.JoinRequest
import com.messledger.app.data.model.LeaveRequest
import com.messledger.app.ui.components.EmptyState
import com.messledger.app.ui.components.ErrorDialog
import com.messledger.app.ui.components.LoadingOverlay
import com.messledger.app.ui.components.MessLedgerTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingRequestsScreen(
    onNavigateBack: () -> Unit,
    viewModel: RequestsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Join Requests", "Leave Requests")

    if (uiState.error != null) {
        ErrorDialog(
            message = uiState.error!!,
            onDismiss = viewModel::clearError
        )
    }

    Scaffold(
        topBar = {
            MessLedgerTopBar(
                title = "Pending Requests",
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }

                if (selectedTabIndex == 0) {
                    if (uiState.pendingJoinRequests.isEmpty()) {
                        EmptyState(message = "No pending join requests")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.pendingJoinRequests) { request ->
                                JoinRequestItem(
                                    request = request,
                                    onApprove = { viewModel.approveJoinRequest(request.uid) },
                                    onReject = { viewModel.rejectJoinRequest(request.uid) }
                                )
                            }
                        }
                    }
                } else {
                    if (uiState.pendingLeaveRequests.isEmpty()) {
                        EmptyState(message = "No pending leave requests")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.pendingLeaveRequests) { request ->
                                LeaveRequestItem(
                                    request = request,
                                    onApprove = { viewModel.approveLeaveRequest(request.uid) },
                                    onReject = { viewModel.rejectLeaveRequest(request.uid) }
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.isLoading || uiState.isProcessing) {
                LoadingOverlay()
            }
        }
    }
}

@Composable
fun JoinRequestItem(
    request: JoinRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = request.name, style = MaterialTheme.typography.titleMedium)
            Text(text = "Requested to join", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onReject) {
                    Text("Reject", color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onApprove) {
                    Text("Approve")
                }
            }
        }
    }
}

@Composable
fun LeaveRequestItem(
    request: LeaveRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = request.name, style = MaterialTheme.typography.titleMedium)
            Text(text = "Requested to leave", style = MaterialTheme.typography.bodyMedium)
            if (!request.reason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Reason: ${request.reason}", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onReject) {
                    Text("Reject", color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onApprove) {
                    Text("Approve")
                }
            }
        }
    }
}
