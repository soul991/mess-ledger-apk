package com.messledger.app.ui.mess.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messledger.app.data.model.Mess
import com.messledger.app.data.repository.MessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessSettingsViewModel @Inject constructor(
    private val messRepository: MessRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val messId: String = checkNotNull(savedStateHandle["messId"])

    private val _uiState = MutableStateFlow(MessSettingsUiState())
    val uiState: StateFlow<MessSettingsUiState> = _uiState.asStateFlow()

    init {
        loadMess()
    }

    private fun loadMess() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            messRepository.getMessFlow(messId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load mess settings"
                    )
                }
                .collect { mess ->
                    if (mess != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            messName = mess.messName,
                            categories = mess.categories
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Mess not found"
                        )
                    }
                }
        }
    }

    fun updateMessName(name: String) {
        _uiState.value = _uiState.value.copy(messName = name)
    }

    fun addCategory(category: String) {
        val currentCategories = _uiState.value.categories.toMutableList()
        if (category.isNotBlank() && !currentCategories.contains(category)) {
            currentCategories.add(category.trim())
            _uiState.value = _uiState.value.copy(categories = currentCategories)
        }
    }

    fun removeCategory(category: String) {
        val currentCategories = _uiState.value.categories.toMutableList()
        currentCategories.remove(category)
        _uiState.value = _uiState.value.copy(categories = currentCategories)
    }

    fun saveChanges() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            try {
                val currentState = _uiState.value
                val mess = messRepository.getMess(messId)
                if (mess != null) {
                    val updatedMess = mess.copy(
                        messName = currentState.messName,
                        categories = currentState.categories
                    )
                    messRepository.updateMess(updatedMess)
                    _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: "Failed to save settings"
                )
            }
        }
    }

    fun resetSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class MessSettingsUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val messName: String = "",
    val categories: List<String> = emptyList(),
    val error: String? = null,
    val saveSuccess: Boolean = false
)
