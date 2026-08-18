package com.messledger.app.ui.mess.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.messledger.app.ui.components.ConfirmationDialog
import com.messledger.app.ui.components.MessLedgerTopBar
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseScreen(
    messId: String,
    expenseId: String?,
    onNavigateBack: () -> Unit,
    viewModel: ExpensesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isEditing = expenseId != null

    var paidBy by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))) }
    var splitType by remember { mutableStateOf("meals") }
    var note by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var paidByExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val defaultCategories = listOf("Groceries", "Vegetables", "Fish & Meat", "Rice & Oil", "Spices", "Gas & Utility", "Other")

    LaunchedEffect(uiState.expenses, expenseId) {
        if (expenseId != null) {
            val existing = viewModel.getExpense(expenseId)
            if (existing != null) {
                paidBy = existing.paidBy
                category = existing.category
                amount = existing.amount.toString()
                date = existing.date
                splitType = existing.splitType
                note = existing.note ?: ""
            }
        } else if (paidBy.isEmpty() && uiState.members.isNotEmpty()) {
            paidBy = uiState.members.first().id
            if (category.isEmpty()) category = defaultCategories.first()
        }
    }

    Scaffold(
        topBar = {
            MessLedgerTopBar(
                title = if (isEditing) "Edit Expense" else "Add Expense",
                onNavigateBack = onNavigateBack,
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Expense",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Paid By Dropdown
            ExposedDropdownMenuBox(
                expanded = paidByExpanded,
                onExpandedChange = { paidByExpanded = !paidByExpanded }
            ) {
                val selectedMemberName = uiState.members.find { it.id == paidBy }?.name ?: "Select Member"
                OutlinedTextField(
                    value = selectedMemberName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Paid By") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paidByExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = paidByExpanded,
                    onDismissRequest = { paidByExpanded = false }
                ) {
                    uiState.members.filter { it.isActive }.forEach { member ->
                        DropdownMenuItem(
                            text = { Text(member.name) },
                            onClick = {
                                paidBy = member.id
                                paidByExpanded = false
                            }
                        )
                    }
                }
            }

            // Category Dropdown
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    defaultCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                category = cat
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            // Amount
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount (₹)") },
                prefix = { Text("₹ ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Date
            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Split Type Selection
            Text(
                text = "Expense Split Method",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = splitType == "meals",
                        onClick = { splitType = "meals" }
                    )
                    Text("By Meals (Default)")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = splitType == "equal",
                        onClick = { splitType = "equal" }
                    )
                    Text("Equally")
                }
            }

            // Notes
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note / Item Description (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val amountVal = amount.toDoubleOrNull() ?: 0.0
                    if (amountVal > 0 && paidBy.isNotBlank()) {
                        if (isEditing && expenseId != null) {
                            viewModel.updateExpense(
                                id = expenseId,
                                paidBy = paidBy,
                                category = category.ifBlank { "General" },
                                amount = amountVal,
                                date = date,
                                splitType = splitType,
                                note = note.ifBlank { null }
                            )
                        } else {
                            viewModel.addExpense(
                                paidBy = paidBy,
                                category = category.ifBlank { "General" },
                                amount = amountVal,
                                date = date,
                                splitType = splitType,
                                note = note.ifBlank { null }
                            )
                        }
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = amount.toDoubleOrNull() != null && amount.toDoubleOrNull()!! > 0 && paidBy.isNotBlank()
            ) {
                Text(if (isEditing) "Save Changes" else "Add Expense")
            }
        }
    }

    if (showDeleteConfirm && expenseId != null) {
        ConfirmationDialog(
            title = "Delete Expense",
            message = "Are you sure you want to delete this expense?",
            onConfirm = {
                viewModel.deleteExpense(expenseId)
                showDeleteConfirm = false
                onNavigateBack()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
