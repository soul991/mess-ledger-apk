package com.messledger.app.ui.mess.meals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.messledger.app.data.model.Member
import com.messledger.app.ui.components.EmptyState
import com.messledger.app.ui.components.LoadingOverlay
import com.messledger.app.ui.components.MemberAvatar
import com.messledger.app.ui.components.MessLedgerTopBar
import com.messledger.app.ui.theme.LedgerSuccess
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun MealsScreen(
    messId: String,
    onNavigateBack: () -> Unit,
    viewModel: MealsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    Scaffold(
        topBar = {
            MessLedgerTopBar(
                title = "Meal Tracker",
                onNavigateBack = onNavigateBack
            )
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
                // Date Selector Header
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedDate = selectedDate.minusDays(1) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Day")
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (selectedDate == LocalDate.now()) "Today" else selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM dd")),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { selectedDate = selectedDate.plusDays(1) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day")
                        }
                    }
                }

                if (uiState.members.isEmpty()) {
                    EmptyState(
                        title = "No Members",
                        message = "Invite members to start tracking daily meals."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.members.filter { it.isActive }, key = { it.id }) { member ->
                            val isCurrentUser = member.id == uiState.currentUser?.uid
                            val isManager = uiState.currentUser?.uid == uiState.currentManagerId
                            val canToggle = isCurrentUser || isManager

                            MealMemberRow(
                                member = member,
                                canToggle = canToggle,
                                onToggleLunch = { absent ->
                                    viewModel.toggleMeal(selectedDate, member.id, "lunch", absent)
                                },
                                onToggleDinner = { absent ->
                                    viewModel.toggleMeal(selectedDate, member.id, "dinner", absent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MealMemberRow(
    member: Member,
    canToggle: Boolean,
    onToggleLunch: (Boolean) -> Unit,
    onToggleDinner: (Boolean) -> Unit
) {
    var lunchAbsent by remember { mutableStateOf(false) }
    var dinnerAbsent by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MemberAvatar(name = member.name)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (member.isManager) {
                    Text(
                        text = "Manager",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Lunch Toggle
            MealStatusChip(
                label = "Lunch",
                isAbsent = lunchAbsent,
                enabled = canToggle,
                onClick = {
                    lunchAbsent = !lunchAbsent
                    onToggleLunch(lunchAbsent)
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Dinner Toggle
            MealStatusChip(
                label = "Dinner",
                isAbsent = dinnerAbsent,
                enabled = canToggle,
                onClick = {
                    dinnerAbsent = !dinnerAbsent
                    onToggleDinner(dinnerAbsent)
                }
            )
        }
    }
}

@Composable
fun MealStatusChip(
    label: String,
    isAbsent: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isAbsent) MaterialTheme.colorScheme.errorContainer else LedgerSuccess.copy(alpha = 0.15f)
    val contentColor = if (isAbsent) MaterialTheme.colorScheme.error else LedgerSuccess

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isAbsent) Icons.Default.Close else Icons.Default.Check,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}
