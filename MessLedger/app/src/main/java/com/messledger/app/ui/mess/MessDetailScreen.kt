package com.messledger.app.ui.mess

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.messledger.app.ui.components.ErrorDialog
import com.messledger.app.ui.components.LoadingOverlay
import com.messledger.app.ui.components.MessLedgerTopBar
import com.messledger.app.ui.mess.activity.ActivityScreen
import com.messledger.app.ui.mess.contributions.ContributionsScreen
import com.messledger.app.ui.mess.expenses.ExpensesScreen
import com.messledger.app.ui.mess.meals.MealsScreen
import com.messledger.app.ui.mess.summary.SummaryScreen

enum class MessBottomTab(val title: String) {
    SUMMARY("Summary"),
    MEALS("Meals"),
    EXPENSES("Expenses"),
    CONTRIBUTIONS("Funds"),
    ACTIVITY("Activity")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessDetailScreen(
    messId: String,
    onNavigateToSettings: () -> Unit,
    onNavigateToRequests: () -> Unit,
    onNavigateToMembers: (() -> Unit)? = null,
    onNavigateToAddExpense: (() -> Unit)? = null,
    onNavigateToEditExpense: ((String) -> Unit)? = null,
    onNavigateToAddContribution: (() -> Unit)? = null,
    onNavigateToEditContribution: ((String) -> Unit)? = null,
    onNavigateToGuestMeals: (() -> Unit)? = null,
    onBackClick: () -> Unit,
    viewModel: MessViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(MessBottomTab.SUMMARY) }
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            MessLedgerTopBar(
                title = (uiState as? MessDetailUiState.Success)?.mess?.messName ?: "Mess Details",
                onBackClick = onBackClick,
                actions = {
                    if (uiState is MessDetailUiState.Success) {
                        val successState = uiState as MessDetailUiState.Success
                        if (successState.isManager) {
                            BadgedBox(
                                badge = {
                                    val count = successState.pendingRequestsCount
                                    if (count > 0) {
                                        Badge { Text("$count") }
                                    }
                                }
                            ) {
                                IconButton(onClick = onNavigateToRequests) {
                                    Icon(Icons.Default.Notifications, contentDescription = "Requests")
                                }
                            }
                        }

                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Members") },
                                leadingIcon = { Icon(Icons.Default.People, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToMembers?.invoke()
                                }
                            )
                            if (onNavigateToGuestMeals != null) {
                                DropdownMenuItem(
                                    text = { Text("Guest Meals") },
                                    leadingIcon = { Icon(Icons.Default.GroupAdd, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onNavigateToGuestMeals()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Share Invite ID") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Join my mess on Mess Ledger!\nMess ID: $messId\nInvite Link: messledger://invite/$messId"
                                        )
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Mess Invite"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToSettings()
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == MessBottomTab.SUMMARY,
                    onClick = { selectedTab = MessBottomTab.SUMMARY },
                    icon = { Icon(if (selectedTab == MessBottomTab.SUMMARY) Icons.Filled.Dashboard else Icons.Outlined.Dashboard, contentDescription = "Summary") },
                    label = { Text(MessBottomTab.SUMMARY.title) }
                )
                NavigationBarItem(
                    selected = selectedTab == MessBottomTab.MEALS,
                    onClick = { selectedTab = MessBottomTab.MEALS },
                    icon = { Icon(if (selectedTab == MessBottomTab.MEALS) Icons.Filled.Restaurant else Icons.Outlined.Restaurant, contentDescription = "Meals") },
                    label = { Text(MessBottomTab.MEALS.title) }
                )
                NavigationBarItem(
                    selected = selectedTab == MessBottomTab.EXPENSES,
                    onClick = { selectedTab = MessBottomTab.EXPENSES },
                    icon = { Icon(if (selectedTab == MessBottomTab.EXPENSES) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong, contentDescription = "Expenses") },
                    label = { Text(MessBottomTab.EXPENSES.title) }
                )
                NavigationBarItem(
                    selected = selectedTab == MessBottomTab.CONTRIBUTIONS,
                    onClick = { selectedTab = MessBottomTab.CONTRIBUTIONS },
                    icon = { Icon(if (selectedTab == MessBottomTab.CONTRIBUTIONS) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet, contentDescription = "Funds") },
                    label = { Text(MessBottomTab.CONTRIBUTIONS.title) }
                )
                NavigationBarItem(
                    selected = selectedTab == MessBottomTab.ACTIVITY,
                    onClick = { selectedTab = MessBottomTab.ACTIVITY },
                    icon = { Icon(if (selectedTab == MessBottomTab.ACTIVITY) Icons.Filled.History else Icons.Outlined.History, contentDescription = "Activity") },
                    label = { Text(MessBottomTab.ACTIVITY.title) }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is MessDetailUiState.Loading -> {
                    LoadingOverlay(isLoading = true)
                }
                is MessDetailUiState.Error -> {
                    ErrorDialog(
                        message = state.message,
                        onDismiss = onBackClick
                    )
                }
                is MessDetailUiState.Success -> {
                    when (selectedTab) {
                        MessBottomTab.SUMMARY -> {
                            SummaryScreen(
                                messId = messId,
                                onBackClick = onBackClick
                            )
                        }
                        MessBottomTab.MEALS -> {
                            MealsScreen(
                                messId = messId,
                                onNavigateBack = onBackClick
                            )
                        }
                        MessBottomTab.EXPENSES -> {
                            ExpensesScreen(
                                messId = messId,
                                onNavigateBack = onBackClick,
                                onAddExpense = { onNavigateToAddExpense?.invoke() },
                                onEditExpense = { id -> onNavigateToEditExpense?.invoke(id) }
                            )
                        }
                        MessBottomTab.CONTRIBUTIONS -> {
                            ContributionsScreen(
                                messId = messId,
                                onNavigateBack = onBackClick,
                                onNavigateToAddContribution = { onNavigateToAddContribution?.invoke() },
                                onNavigateToEditContribution = { _, contribId -> onNavigateToEditContribution?.invoke(contribId) }
                            )
                        }
                        MessBottomTab.ACTIVITY -> {
                            ActivityScreen(
                                messId = messId,
                                onNavigateBack = onBackClick
                            )
                        }
                    }
                }
            }
        }
    }
}
