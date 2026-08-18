package com.messledger.app.ui.mess.meals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messledger.app.data.model.Member
import com.messledger.app.data.model.User
import com.messledger.app.data.repository.AuthRepository
import com.messledger.app.data.repository.MealRepository
import com.messledger.app.data.repository.MemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class MealsUiState(
    val isLoading: Boolean = true,
    val members: List<Member> = emptyList(),
    val currentUser: User? = null,
    val currentManagerId: String? = null,
    val currentMonth: YearMonth = YearMonth.now(),
    val error: String? = null
)

@HiltViewModel
class MealsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val memberRepository: MemberRepository,
    private val mealRepository: MealRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val messId: String = checkNotNull(savedStateHandle["messId"])
    
    private val _currentMonth = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<MealsUiState> = combine(
        authRepository.getAuthStateFlow(),
        memberRepository.getMembersFlow(messId),
        _currentMonth
    ) { user, members, month ->
        MealsUiState(
            isLoading = false,
            members = members,
            currentUser = user,
            currentManagerId = members.find { it.isManager }?.id,
            currentMonth = month,
            error = null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MealsUiState()
    )

    fun previousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    fun toggleMeal(date: LocalDate, memberId: String, mealType: String, absent: Boolean) {
        viewModelScope.launch {
            try {
                mealRepository.toggleMeal(messId, date.toString(), memberId, mealType, absent)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
