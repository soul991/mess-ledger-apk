package com.messledger.app.ui.mess.members

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messledger.app.data.model.Member
import com.messledger.app.data.repository.AuthRepository
import com.messledger.app.data.repository.MemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MembersViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val memberRepository: MemberRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val messId: String = checkNotNull(savedStateHandle["messId"])
    
    val uiState: StateFlow<MembersUiState> = combine(
        authRepository.getAuthStateFlow(),
        memberRepository.getMembersFlow(messId)
    ) { currentUser, members ->
        if (currentUser == null) return@combine MembersUiState.Error("Not authenticated")
        
        val activeMembers = members.filter { it.isActive }
        val currentMember = activeMembers.find { it.id == currentUser.uid }
        val isManager = currentMember?.isManager == true
        
        MembersUiState.Success(
            members = activeMembers,
            isManager = isManager,
            currentUserId = currentUser.uid
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MembersUiState.Loading
    )

    fun removeMember(memberId: String) {
        viewModelScope.launch {
            try {
                memberRepository.removeMember(messId, memberId)
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun transferManager(newManagerId: String) {
        viewModelScope.launch {
            val state = uiState.value as? MembersUiState.Success ?: return@launch
            try {
                memberRepository.transferManager(messId, state.currentUserId, newManagerId)
            } catch (e: Exception) {
                // handle error
            }
        }
    }
}

sealed interface MembersUiState {
    data object Loading : MembersUiState
    data class Success(
        val members: List<Member>,
        val isManager: Boolean,
        val currentUserId: String
    ) : MembersUiState
    data class Error(val message: String) : MembersUiState
}
