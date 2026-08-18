package com.messledger.app.ui.mess

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messledger.app.data.model.Mess
import com.messledger.app.data.repository.AuthRepository
import com.messledger.app.data.repository.MemberRepository
import com.messledger.app.data.repository.MessRepository
import com.messledger.app.data.repository.RequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MessViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val messRepository: MessRepository,
    private val memberRepository: MemberRepository,
    private val requestRepository: RequestRepository
) : ViewModel() {

    private val messId: String = checkNotNull(savedStateHandle["messId"])

    val uiState: StateFlow<MessDetailUiState> = combine(
        authRepository.getAuthStateFlow(),
        messRepository.getMessFlow(messId),
        memberRepository.getMembersFlow(messId),
        requestRepository.getJoinRequestsFlow(messId)
    ) { currentUser, mess, members, joinRequests ->
        if (currentUser == null || mess == null) return@combine MessDetailUiState.Loading

        val currentMember = members.find { it.id == currentUser.uid }
        if (currentMember == null) return@combine MessDetailUiState.Error("Not a member")

        val pendingRequestsCount = joinRequests.count { it.status == "PENDING" }

        MessDetailUiState.Success(
            mess = mess,
            currentUserRole = currentMember.role,
            isManager = currentMember.isManager,
            pendingRequestsCount = pendingRequestsCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MessDetailUiState.Loading
    )
}

sealed interface MessDetailUiState {
    data object Loading : MessDetailUiState
    data class Success(
        val mess: Mess,
        val currentUserRole: String,
        val isManager: Boolean,
        val pendingRequestsCount: Int
    ) : MessDetailUiState
    data class Error(val message: String) : MessDetailUiState
}
