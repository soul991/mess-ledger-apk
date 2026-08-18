package com.messledger.app.ui.mess.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messledger.app.data.repository.ContributionRepository
import com.messledger.app.data.repository.ExpenseRepository
import com.messledger.app.data.repository.MemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val expenseRepository: ExpenseRepository,
    private val contributionRepository: ContributionRepository,
    private val memberRepository: MemberRepository
) : ViewModel() {

    private val messId: String = checkNotNull(savedStateHandle["messId"])

    val uiState: StateFlow<SummaryUiState> = combine(
        expenseRepository.getExpensesFlow(messId),
        contributionRepository.getContributionsFlow(messId),
        memberRepository.getMembersFlow(messId)
    ) { expenses, contributions, members ->
        val totalExpenses = expenses.sumOf { it.amount }
        val totalContributions = contributions.sumOf { it.amount }
        val memberCount = members.size
        
        SummaryUiState.Success(
            totalExpenses = totalExpenses,
            totalContributions = totalContributions,
            memberCount = memberCount,
            balance = totalContributions - totalExpenses
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SummaryUiState.Loading
    )
}

sealed interface SummaryUiState {
    data object Loading : SummaryUiState
    data class Success(
        val totalExpenses: Double,
        val totalContributions: Double,
        val memberCount: Int,
        val balance: Double
    ) : SummaryUiState
    data class Error(val message: String) : SummaryUiState
}
