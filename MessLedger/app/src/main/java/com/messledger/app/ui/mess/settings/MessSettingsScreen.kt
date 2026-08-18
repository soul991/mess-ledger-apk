package com.messledger.app.ui.mess.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.messledger.app.ui.components.ErrorDialog
import com.messledger.app.ui.components.LoadingOverlay
import com.messledger.app.ui.components.MessLedgerTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: MessSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var newCategory by remember { mutableStateOf("") }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            viewModel.resetSuccess()
            onNavigateBack()
        }
    }

    if (uiState.error != null) {
        ErrorDialog(
            message = uiState.error!!,
            onDismiss = viewModel::clearError
        )
    }

    Scaffold(
        topBar = {
            MessLedgerTopBar(
                title = "Mess Settings",
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = uiState.messName,
                        onValueChange = viewModel::updateMessName,
                        label = { Text("Mess Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    Text(
                        text = "Expense Categories",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newCategory,
                            onValueChange = { newCategory = it },
                            label = { Text("New Category") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                viewModel.addCategory(newCategory)
                                newCategory = ""
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Category")
                        }
                    }
                }

                items(uiState.categories) { category ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = category)
                            IconButton(onClick = { viewModel.removeCategory(category) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove Category")
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = viewModel::saveChanges,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.messName.isNotBlank() && !uiState.isSaving
                    ) {
                        Text("Save Changes")
                    }
                }
            }

            if (uiState.isLoading || uiState.isSaving) {
                LoadingOverlay()
            }
        }
    }
}
