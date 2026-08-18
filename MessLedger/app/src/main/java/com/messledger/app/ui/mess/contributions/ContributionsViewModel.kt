package com.messledger.app.ui.mess.contributions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messledger.app.data.model.Contribution
import com.messledger.app.data.model.Member
import com.messledger.app.data.repository.ContributionRepository
import com.messledger.app.data.repository.MemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ContributionsUiState(
    val contributions: List<Contribution> = emptyList(),
    val members: List<Member> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ContributionsViewModel @Inject constructor(
    private val contributionRepository: ContributionRepository,
    private val memberRepository: MemberRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val messId: String = checkNotNull(savedStateHandle["messId"])

    private val _uiState = MutableStateFlow(ContributionsUiState())
    val uiState: StateFlow<ContributionsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                combine(
                    contributionRepository.getContributionsFlow(messId),
                    memberRepository.getMembersFlow(messId)
                ) { contributions, members ->
                    ContributionsUiState(
                        contributions = contributions,
                        members = members,
                        isLoading = false
                    )
                }.catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun getContribution(id: String): Contribution? {
        return _uiState.value.contributions.find { it.id == id }
    }

    fun addContribution(messId: String, memberId: String, amount: Double, note: String) {
        viewModelScope.launch {
            try {
                val contribution = Contribution(
                    id = UUID.randomUUID().toString(),
                    memberId = memberId,
                    amount = amount,
                    date = System.currentTimeMillis().toString(), // Should format properly
                    note = note
                )
                contributionRepository.addContribution(messId, contribution)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateContribution(id: String, memberId: String, amount: Double, note: String) {
        viewModelScope.launch {
            try {
                val existing = getContribution(id) ?: return@launch
                val updated = existing.copy(
                    memberId = memberId,
                    amount = amount,
                    note = note
                )
                contributionRepository.updateContribution(messId, updated)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteContribution(id: String) {
        viewModelScope.launch {
            try {
                contributionRepository.deleteContribution(messId, id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
