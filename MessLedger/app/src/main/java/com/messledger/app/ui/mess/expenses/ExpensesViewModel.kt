package com.messledger.app.ui.mess.expenses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messledger.app.data.model.Expense
import com.messledger.app.data.model.Member
import com.messledger.app.data.repository.ExpenseRepository
import com.messledger.app.data.repository.MemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ExpensesUiState(
    val isLoading: Boolean = true,
    val expenses: List<Expense> = emptyList(),
    val members: List<Member> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val memberRepository: MemberRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val messId: String = checkNotNull(savedStateHandle["messId"])

    val uiState: StateFlow<ExpensesUiState> = combine(
        expenseRepository.getExpensesFlow(messId),
        memberRepository.getMembersFlow(messId)
    ) { expenses, members ->
        ExpensesUiState(
            isLoading = false,
            expenses = expenses,
            members = members,
            error = null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExpensesUiState()
    )

    fun getExpense(id: String): Expense? {
        return uiState.value.expenses.find { it.id == id }
    }

    fun addExpense(
        paidBy: String,
        category: String,
        amount: Double,
        date: String,
        splitType: String,
        note: String?
    ) {
        viewModelScope.launch {
            val expense = Expense(
                id = UUID.randomUUID().toString(),
                paidBy = paidBy,
                category = category,
                amount = amount,
                date = date,
                splitType = splitType,
                note = note
            )
            expenseRepository.addExpense(messId, expense)
        }
    }

    fun updateExpense(
        id: String,
        paidBy: String,
        category: String,
        amount: Double,
        date: String,
        splitType: String,
        note: String?
    ) {
        viewModelScope.launch {
            val updated = Expense(
                id = id,
                paidBy = paidBy,
                category = category,
                amount = amount,
                date = date,
                splitType = splitType,
                note = note
            )
            expenseRepository.updateExpense(messId, updated)
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(messId, expenseId)
        }
    }
}
