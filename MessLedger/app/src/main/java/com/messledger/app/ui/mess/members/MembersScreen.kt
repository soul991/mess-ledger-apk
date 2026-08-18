package com.messledger.app.ui.mess.members

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.messledger.app.data.model.Member
import com.messledger.app.ui.components.ConfirmationDialog
import com.messledger.app.ui.components.ErrorDialog
import com.messledger.app.ui.components.LoadingOverlay
import com.messledger.app.ui.components.ManagerBadge
import com.messledger.app.ui.components.MemberAvatar
import com.messledger.app.ui.components.MessLedgerTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(
    messId: String,
    onBackClick: () -> Unit,
    viewModel: MembersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var memberToRemove by remember { mutableStateOf<Member?>(null) }
    var memberToTransfer by remember { mutableStateOf<Member?>(null) }

    Scaffold(
        topBar = {
            MessLedgerTopBar(
                title = "Members",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is MembersUiState.Loading -> LoadingOverlay(isLoading = true)
                is MembersUiState.Error -> ErrorDialog(message = state.message, onDismiss = onBackClick)
                is MembersUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(state.members, key = { it.id }) { member ->
                            MemberItem(
                                member = member,
                                isManager = state.isManager,
                                isCurrentUser = member.id == state.currentUserId,
                                onRemoveClick = { memberToRemove = member },
                                onTransferClick = { memberToTransfer = member }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    if (memberToRemove != null) {
        ConfirmationDialog(
            title = "Remove Member",
            message = "Are you sure you want to remove ${memberToRemove?.name} from the mess?",
            onConfirm = {
                memberToRemove?.let { viewModel.removeMember(it.id) }
                memberToRemove = null
            },
            onDismiss = { memberToRemove = null }
        )
    }

    if (memberToTransfer != null) {
        ConfirmationDialog(
            title = "Transfer Manager",
            message = "Are you sure you want to transfer manager role to ${memberToTransfer?.name}?",
            onConfirm = {
                memberToTransfer?.let { viewModel.transferManager(it.id) }
                memberToTransfer = null
            },
            onDismiss = { memberToTransfer = null }
        )
    }
}

@Composable
fun MemberItem(
    member: Member,
    isManager: Boolean,
    isCurrentUser: Boolean,
    onRemoveClick: () -> Unit,
    onTransferClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MemberAvatar(name = member.name)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = member.name, style = MaterialTheme.typography.bodyLarge)
            if (member.isManager) {
                ManagerBadge()
            }
        }
        
        if (isManager && !isCurrentUser && !member.isManager) {
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Transfer Manager") },
                        onClick = {
                            expanded = false
                            onTransferClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Remove") },
                        onClick = {
                            expanded = false
                            onRemoveClick()
                        }
                    )
                }
            }
        }
    }
}
