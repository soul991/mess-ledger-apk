package com.messledger.app.ui.mess.guestmeals

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
fun AddEditGuestMealScreen(
    messId: String,
    guestMealId: String?,
    onNavigateBack: () -> Unit,
    viewModel: GuestMealsViewModel = hiltViewModel()
) {
    var count by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedHostId by remember { mutableStateOf("") }
    var selectedMeal by remember { mutableStateOf("Lunch") }

    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(guestMealId) {
        if (guestMealId != null) {
            val guestMeal = viewModel.getGuestMeal(guestMealId)
            guestMeal?.let {
                count = it.count.toString()
                note = it.note ?: ""
                selectedHostId = it.hostId
                selectedMeal = it.meal
            }
        }
    }

    Scaffold(
        topBar = {
            MessLedgerTopBar(
                title = if (guestMealId == null) "Add Guest Meal" else "Edit Guest Meal",
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
                value = selectedHostId,
                onValueChange = { selectedHostId = it },
                label = { Text("Host Member ID") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = selectedMeal,
                onValueChange = { selectedMeal = it },
                label = { Text("Meal (e.g. Lunch/Dinner)") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = count,
                onValueChange = { count = it },
                label = { Text("Count") },
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
                    val countInt = count.toIntOrNull() ?: 1
                    if (guestMealId == null) {
                        viewModel.addGuestMeal(messId, selectedHostId, selectedMeal, countInt, note)
                    } else {
                        viewModel.updateGuestMeal(guestMealId, selectedHostId, selectedMeal, countInt, note)
                    }
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
            
            if (guestMealId != null) {
                OutlinedButton(
                    onClick = {
                        viewModel.deleteGuestMeal(guestMealId)
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
