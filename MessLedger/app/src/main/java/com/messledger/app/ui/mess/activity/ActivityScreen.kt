package com.messledger.app.ui.mess.activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.messledger.app.data.model.ActivityLogEntry
import com.messledger.app.ui.components.EmptyState
import com.messledger.app.ui.components.ErrorDialog
import com.messledger.app.ui.components.LoadingOverlay
import com.messledger.app.ui.components.MessLedgerTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    messId: String,
    onNavigateBack: () -> Unit,
    viewModel: ActivityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            MessLedgerTopBar(
                title = "Activity Log",
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            val errorMessage = uiState.error
            when {
                uiState.isLoading -> LoadingOverlay()
                errorMessage != null -> ErrorDialog(
                    message = errorMessage,
                    onDismiss = viewModel::clearError
                )
                uiState.activities.isEmpty() -> EmptyState(
                    title = "No Activity",
                    message = "No activities have been recorded yet."
                )
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.activities) { activity ->
                            ActivityLogCard(activity = activity)
                        }
                        
                        item {
                            if (uiState.hasMore) {
                                Button(
                                    onClick = { viewModel.loadMore() },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    Text("Load More")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityLogCard(activity: ActivityLogEntry) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${activity.actorName} - ${activity.action}",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = activity.summary,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Time: ${activity.timestamp}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
