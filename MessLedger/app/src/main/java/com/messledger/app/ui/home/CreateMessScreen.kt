package com.messledger.app.ui.home

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.messledger.app.ui.components.MessLedgerTopBar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateMessScreen(
    onBack: () -> Unit,
    onMessCreated: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    var messName by remember { mutableStateOf("") }
    var currentCategory by remember { mutableStateOf("") }
    val categories = remember { mutableStateListOf<String>("Meals", "Rent", "Utilities") }
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var createdMessId by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            MessLedgerTopBar(
                title = "Create a Mess",
                onBackClick = onBack
            )
        }
    ) { paddingValues ->
        if (createdMessId != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Mess Created Successfully!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Share this ID with members to join:")
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = createdMessId!!,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "Join my mess on Mess Ledger! Mess ID: $createdMessId\nOr use this link: messledger://invite/$createdMessId")
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Invite Link")
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { onMessCreated(createdMessId!!) }) {
                    Text("Go to Mess")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                OutlinedTextField(
                    value = messName,
                    onValueChange = { messName = it; errorMessage = null },
                    label = { Text("Mess Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.forEach { category ->
                        InputChip(
                            selected = false,
                            onClick = { },
                            label = { Text(category) },
                            trailingIcon = {
                                IconButton(onClick = { categories.remove(category) }, modifier = Modifier.size(16.dp)) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove", modifier = Modifier.size(12.dp))
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = currentCategory,
                    onValueChange = { currentCategory = it },
                    label = { Text("Add Category") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (currentCategory.isNotBlank() && !categories.contains(currentCategory.trim())) {
                                categories.add(currentCategory.trim())
                                currentCategory = ""
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        isCreating = true
                        viewModel.createMess(
                            name = messName,
                            categories = categories.toList(),
                            onSuccess = { id ->
                                isCreating = false
                                createdMessId = id
                            },
                            onError = { err ->
                                isCreating = false
                                errorMessage = err
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = messName.isNotBlank() && categories.isNotEmpty() && !isCreating
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Create Mess")
                    }
                }
            }
        }
    }
}
