package com.messledger.app.ui.mess.activity

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messledger.app.data.model.ActivityLogEntry
import com.messledger.app.data.repository.ActivityLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActivityUiState(
    val activities: List<ActivityLogEntry> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val hasMore: Boolean = false
)

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val activityLogRepository: ActivityLogRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val messId: String = checkNotNull(savedStateHandle["messId"])
    private val limit = 20

    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()

    init {
        loadActivities()
    }

    private fun loadActivities() {
        viewModelScope.launch {
            try {
                activityLogRepository.getActivityLogFlow(messId, limit)
                    .catch { e ->
                        _uiState.update { it.copy(isLoading = false, error = e.message) }
                    }
                    .collect { entries ->
                        _uiState.update {
                            it.copy(
                                activities = entries,
                                isLoading = false,
                                hasMore = entries.size >= limit
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun loadMore() {
        viewModelScope.launch {
            val lastTimestamp = _uiState.value.activities.lastOrNull()?.timestamp ?: return@launch
            try {
                val newEntries = activityLogRepository.loadMoreActivities(messId, lastTimestamp, limit)
                _uiState.update {
                    it.copy(
                        activities = it.activities + newEntries,
                        hasMore = newEntries.size >= limit
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
