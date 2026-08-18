package com.messledger.app.ui.requests

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messledger.app.data.model.JoinRequest
import com.messledger.app.data.model.LeaveRequest
import com.messledger.app.data.repository.RequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RequestsViewModel @Inject constructor(
    private val requestRepository: RequestRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val messId: String = checkNotNull(savedStateHandle["messId"])

    private val _uiState = MutableStateFlow(RequestsUiState())
    val uiState: StateFlow<RequestsUiState> = _uiState.asStateFlow()

    init {
        loadRequests()
    }

    private fun loadRequests() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            combine(
                requestRepository.getJoinRequestsFlow(messId),
                requestRepository.getLeaveRequestsFlow(messId),
                requestRepository.getMyLeaveRequestFlow(messId)
            ) { joinReqs, leaveReqs, myLeaveReq ->
                RequestsUiState(
                    isLoading = false,
                    pendingJoinRequests = joinReqs.filter { it.status == "PENDING" },
                    pendingLeaveRequests = leaveReqs.filter { it.status == "PENDING" },
                    myLeaveRequest = myLeaveReq
                )
            }.catch { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load requests"
                )
            }.collect { state ->
                _uiState.value = state.copy(
                    isProcessing = _uiState.value.isProcessing,
                    error = _uiState.value.error
                )
            }
        }
    }

    fun approveJoinRequest(uid: String) {
        viewModelScope.launch {
            processAction { requestRepository.approveJoinRequest(messId, uid) }
        }
    }

    fun rejectJoinRequest(uid: String) {
        viewModelScope.launch {
            processAction { requestRepository.rejectJoinRequest(messId, uid) }
        }
    }

    fun approveLeaveRequest(uid: String) {
        viewModelScope.launch {
            processAction { requestRepository.approveLeaveRequest(messId, uid) }
        }
    }

    fun rejectLeaveRequest(uid: String, reason: String? = null) {
        viewModelScope.launch {
            processAction { requestRepository.rejectLeaveRequest(messId, uid, reason) }
        }
    }

    fun submitLeaveRequest(reason: String?) {
        viewModelScope.launch {
            processAction { requestRepository.submitLeaveRequest(messId, reason) }
        }
    }

    fun withdrawLeaveRequest() {
        viewModelScope.launch {
            processAction { requestRepository.withdrawLeaveRequest(messId) }
        }
    }

    private suspend fun processAction(action: suspend () -> Unit) {
        _uiState.value = _uiState.value.copy(isProcessing = true, error = null)
        try {
            action()
            _uiState.value = _uiState.value.copy(isProcessing = false)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                error = e.message ?: "Action failed"
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class RequestsUiState(
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val pendingJoinRequests: List<JoinRequest> = emptyList(),
    val pendingLeaveRequests: List<LeaveRequest> = emptyList(),
    val myLeaveRequest: LeaveRequest? = null,
    val error: String? = null
)
