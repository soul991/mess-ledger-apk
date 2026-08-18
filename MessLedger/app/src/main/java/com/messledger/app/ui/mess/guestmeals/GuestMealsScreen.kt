package com.messledger.app.ui.mess.guestmeals

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
import com.messledger.app.data.model.GuestMeal
import com.messledger.app.data.model.Member
import com.messledger.app.ui.components.EmptyState
import com.messledger.app.ui.components.ErrorDialog
import com.messledger.app.ui.components.LoadingOverlay
import com.messledger.app.ui.components.MessLedgerTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestMealsScreen(
    messId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAddGuestMeal: (String) -> Unit,
    onNavigateToEditGuestMeal: (String, String) -> Unit,
    viewModel: GuestMealsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            MessLedgerTopBar(
                title = "Guest Meals",
                onNavigateBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToAddGuestMeal(messId) }
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Guest Meal")
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
                uiState.guestMeals.isEmpty() -> EmptyState(
                    title = "No Guest Meals",
                    message = "Add guest meals here."
                )
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.guestMeals) { guestMeal ->
                            val host = uiState.members.find { it.id == guestMeal.hostId }
                            GuestMealCard(
                                guestMeal = guestMeal,
                                host = host,
                                onClick = { onNavigateToEditGuestMeal(messId, guestMeal.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GuestMealCard(
    guestMeal: GuestMeal,
    host: Member?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Host: ${host?.name ?: "Unknown"}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${guestMeal.count} ${guestMeal.meal} meals",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Date: ${guestMeal.date}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (!guestMeal.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Note: ${guestMeal.note}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
