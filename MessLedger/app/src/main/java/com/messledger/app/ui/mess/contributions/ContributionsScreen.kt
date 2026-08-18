package com.messledger.app.ui.mess.contributions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.messledger.app.data.model.Contribution
import com.messledger.app.data.model.Member
import com.messledger.app.ui.components.EmptyState
import com.messledger.app.ui.components.ErrorDialog
import com.messledger.app.ui.components.LoadingOverlay
import com.messledger.app.ui.components.MessLedgerTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContributionsScreen(
    messId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAddContribution: (String) -> Unit,
    onNavigateToEditContribution: (String, String) -> Unit,
    viewModel: ContributionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            MessLedgerTopBar(
                title = "Contributions",
                onNavigateBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToAddContribution(messId) }
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Contribution")
            }
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
                uiState.contributions.isEmpty() -> EmptyState(
                    title = "No Contributions",
                    message = "Add contributions to keep track of payments."
                )
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.contributions) { contribution ->
                            val member = uiState.members.find { it.id == contribution.memberId }
                            ContributionCard(
                                contribution = contribution,
                                member = member,
                                onClick = { onNavigateToEditContribution(messId, contribution.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContributionCard(
    contribution: Contribution,
    member: Member?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = member?.name ?: "Unknown Member",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Amount: $${contribution.amount}",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Date: ${contribution.date}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (!contribution.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Note: ${contribution.note}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
