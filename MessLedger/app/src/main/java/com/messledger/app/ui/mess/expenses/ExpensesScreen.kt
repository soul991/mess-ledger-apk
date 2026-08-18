package com.messledger.app.ui.mess.expenses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.messledger.app.data.model.Expense
import com.messledger.app.data.model.Member
import com.messledger.app.ui.components.ConfirmationDialog
import com.messledger.app.ui.components.EmptyState
import com.messledger.app.ui.components.LoadingOverlay
import com.messledger.app.ui.components.MessLedgerTopBar

@Composable
fun ExpensesScreen(
    messId: String,
    onNavigateBack: () -> Unit,
    onAddExpense: () -> Unit,
    onEditExpense: (String) -> Unit,
    viewModel: ExpensesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    val categories = remember(uiState.expenses) {
        listOf("All") + uiState.expenses.map { it.category }.distinct()
    }

    val filteredExpenses = remember(uiState.expenses, selectedCategory) {
        if (selectedCategory == null || selectedCategory == "All") {
            uiState.expenses.sortedByDescending { it.date }
        } else {
            uiState.expenses.filter { it.category == selectedCategory }.sortedByDescending { it.date }
        }
    }

    val totalAmount = remember(filteredExpenses) {
        filteredExpenses.sumOf { it.amount }
    }

    Scaffold(
        topBar = {
            MessLedgerTopBar(
                title = "Expenses",
                onNavigateBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddExpense,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingOverlay()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Header total
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Total Expenses",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "₹${"%.2f".format(totalAmount)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Filter chips
                if (categories.size > 2) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = (selectedCategory ?: "All") == cat,
                                onClick = { selectedCategory = if (cat == "All") null else cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (filteredExpenses.isEmpty()) {
                    EmptyState(
                        title = "No Expenses Found",
                        message = "Tap the + button to record a new expense.",
                        actionText = "Add Expense",
                        onActionClick = onAddExpense
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredExpenses, key = { it.id }) { expense ->
                            val member = uiState.members.find { it.id == expense.paidBy }
                            ExpenseCard(
                                expense = expense,
                                paidByName = member?.name ?: "Unknown",
                                onClick = { onEditExpense(expense.id) },
                                onDelete = { expenseToDelete = expense }
                            )
                        }
                    }
                }
            }
        }
    }

    if (expenseToDelete != null) {
        ConfirmationDialog(
            title = "Delete Expense",
            message = "Are you sure you want to delete this expense of ₹${expenseToDelete?.amount}?",
            onConfirm = {
                expenseToDelete?.let { viewModel.deleteExpense(it.id) }
                expenseToDelete = null
            },
            onDismiss = { expenseToDelete = null }
        )
    }
}

@Composable
fun ExpenseCard(
    expense: Expense,
    paidByName: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.category.ifBlank { "General Expense" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Paid by $paidByName • ${expense.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!expense.note.isNullOrBlank()) {
                    Text(
                        text = expense.note,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${"%.2f".format(expense.amount)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (expense.splitType == "equal") "Split Equally" else "By Meals",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
