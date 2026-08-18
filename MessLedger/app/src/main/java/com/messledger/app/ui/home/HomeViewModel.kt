package com.messledger.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messledger.app.data.model.Mess
import com.messledger.app.data.model.User
import com.messledger.app.data.repository.AuthRepository
import com.messledger.app.data.repository.MessRepository
import com.messledger.app.data.repository.RequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val user: User? = null,
    val messes: List<Mess> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val messRepository: MessRepository,
    private val requestRepository: RequestRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val user = authRepository.getCurrentUser()
        if (user == null) {
            _uiState.update { it.copy(isLoading = false, error = "User not logged in") }
            return
        }

        viewModelScope.launch {
            try {
                authRepository.getAuthStateFlow().combine(messRepository.getUserMessesFlow(user.uid)) { currentUser, userMesses ->
                    HomeUiState(
                        user = currentUser,
                        messes = userMesses,
                        isLoading = false
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun createMess(name: String, categories: List<String>, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = messRepository.createMess(name, categories)
            result.onSuccess(onSuccess).onFailure { e -> onError(e.message ?: "Failed to create mess") }
        }
    }
    
    fun submitJoinRequest(messId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = requestRepository.submitJoinRequest(messId)
            result.onSuccess { onSuccess() }.onFailure { e -> onError(e.message ?: "Failed to submit request") }
        }
    }
}
