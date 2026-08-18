package com.messledger.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messledger.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val isUsernameAvailable: Boolean? = null,
    val isCheckingUsername: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var usernameCheckJob: Job? = null

    fun checkUsernameAvailability(username: String) {
        if (username.length < 3) {
            _uiState.update { it.copy(isUsernameAvailable = null, isCheckingUsername = false) }
            return
        }
        
        _uiState.update { it.copy(isCheckingUsername = true, isUsernameAvailable = null) }
        usernameCheckJob?.cancel()
        usernameCheckJob = viewModelScope.launch {
            delay(500) // debounce
            try {
                val available = authRepository.checkUsernameAvailability(username)
                _uiState.update { 
                    it.copy(isUsernameAvailable = available, isCheckingUsername = false)
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(isCheckingUsername = false, isUsernameAvailable = null)
                }
            }
        }
    }

    fun register(name: String, username: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.register(name, username, password)
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            }.onFailure { e ->
                val errorMsg = when {
                    e.message?.contains("email", ignoreCase = true) == true || 
                    e.message?.contains("already in use", ignoreCase = true) == true -> "This username is already taken"
                    else -> e.message ?: "Registration failed"
                }
                _uiState.update { it.copy(isLoading = false, error = errorMsg) }
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.login(username, password)
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false, error = "Incorrect username or password") }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
