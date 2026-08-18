package com.messledger.app.ui.mess.contributions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.messledger.app.ui.components.MessLedgerTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditContributionScreen(
    messId: String,
    contributionId: String?,
    onNavigateBack: () -> Unit,
    viewModel: ContributionsViewModel = hiltViewModel()
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedMemberId by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(contributionId) {
        if (contributionId != null) {
            val contribution = viewModel.getContribution(contributionId)
            contribution?.let {
                amount = it.amount.toString()
                note = it.note ?: ""
                selectedMemberId = it.memberId
            }
        }
    }

    Scaffold(
        topBar = {
            MessLedgerTopBar(
                title = if (contributionId == null) "Add Contribution" else "Edit Contribution",
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = selectedMemberId,
                onValueChange = { selectedMemberId = it },
                label = { Text("Member ID") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull() ?: 0.0
                    if (contributionId == null) {
                        viewModel.addContribution(messId, selectedMemberId, amountDouble, note)
                    } else {
                        viewModel.updateContribution(contributionId, selectedMemberId, amountDouble, note)
                    }
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
            
            if (contributionId != null) {
                OutlinedButton(
                    onClick = {
                        viewModel.deleteContribution(contributionId)
                        onNavigateBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            }
        }
    }
}
