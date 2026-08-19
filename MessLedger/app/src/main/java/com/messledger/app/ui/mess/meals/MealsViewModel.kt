package com.messledger.app.ui.mess.meals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messledger.app.data.model.MealStatus
import com.messledger.app.data.model.Member
import com.messledger.app.data.repository.MealRepository
import com.messledger.app.data.repository.MemberRepository
import com.messledger.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class MealMemberUiModel(
    val member: Member,
    val lunchAbsent: Boolean,
    val dinnerAbsent: Boolean
)

sealed interface MealsUiState {
    data object Loading : MealsUiState
    data class Success(
        val messId: String,
        val selectedDate: String, // "YYYY-MM-DD"
        val isToday: Boolean,
        val members: List<MealMemberUiModel>
    ) : MealsUiState
    data class Error(val message: String) : MealsUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MealsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val memberRepository: MemberRepository,
    private val mealRepository: MealRepository
) : ViewModel() {

    private val messId: String = checkNotNull(savedStateHandle["messId"])

    private val _selectedDate = MutableStateFlow(DateUtils.today())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    val uiState: StateFlow<MealsUiState> = _selectedDate
        .flatMapLatest { date ->
            val monthKey = date.take(7)
            combine(
                memberRepository.getMembersFlow(messId),
                mealRepository.getMeals(messId, monthKey)
            ) { members, meals ->
                val today = DateUtils.today()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                // Clamped to [joinedAt, deletedAt] for the selected date
                val eligibleMembers = members.filter { m ->
                    val joined = if (m.joinedAt.isNotBlank()) m.joinedAt else "0000-00-00"
                    val removedAt = m.deletedAt?.let { dateFormat.format(Date(it)) }
                    date >= joined && (removedAt == null || date <= removedAt)
                }

                val dayData = meals[date] ?: emptyMap()
                val memberRows = eligibleMembers.map { m ->
                    val rec = dayData[m.id] ?: MealStatus()
                    MealMemberUiModel(
                        member = m,
                        lunchAbsent = rec.lunchAbsent,
                        dinnerAbsent = rec.dinnerAbsent
                    )
                }

                MealsUiState.Success(
                    messId = messId,
                    selectedDate = date,
                    isToday = (date == today),
                    members = memberRows
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MealsUiState.Loading
        )

    fun setSelectedDate(date: String) {
        val today = DateUtils.today()
        if (date <= today) {
            _selectedDate.value = date
        }
    }

    fun previousDay() {
        val cal = Calendar.getInstance()
        val parts = _selectedDate.value.split("-")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.YEAR)
        val mo = parts.getOrNull(1)?.toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)
        val d = parts.getOrNull(2)?.toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)

        cal.set(y, mo - 1, d)
        cal.add(Calendar.DAY_OF_MONTH, -1)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        _selectedDate.value = sdf.format(cal.time)
    }

    fun nextDay() {
        val today = DateUtils.today()
        if (_selectedDate.value < today) {
            val cal = Calendar.getInstance()
            val parts = _selectedDate.value.split("-")
            val y = parts.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.YEAR)
            val mo = parts.getOrNull(1)?.toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)
            val d = parts.getOrNull(2)?.toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)

            cal.set(y, mo - 1, d)
            cal.add(Calendar.DAY_OF_MONTH, 1)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            val next = sdf.format(cal.time)
            if (next <= today) {
                _selectedDate.value = next
            }
        }
    }

    fun toggleMeal(memberId: String, slot: String, currentAbsent: Boolean) {
        viewModelScope.launch {
            val newAbsent = !currentAbsent
            val date = _selectedDate.value
            try {
                mealRepository.toggleMeal(messId, date, memberId, slot, newAbsent)
                _toastMessage.emit("Meal record updated")
            } catch (e: Exception) {
                _toastMessage.emit("Could not save. Check your connection.")
            }
        }
    }
}
