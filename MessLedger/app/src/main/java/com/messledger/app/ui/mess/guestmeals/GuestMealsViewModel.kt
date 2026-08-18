package com.messledger.app.ui.mess.guestmeals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messledger.app.data.model.GuestMeal
import com.messledger.app.data.model.Member
import com.messledger.app.data.repository.GuestMealRepository
import com.messledger.app.data.repository.MemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class GuestMealsUiState(
    val guestMeals: List<GuestMeal> = emptyList(),
    val members: List<Member> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class GuestMealsViewModel @Inject constructor(
    private val guestMealRepository: GuestMealRepository,
    private val memberRepository: MemberRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val messId: String = checkNotNull(savedStateHandle["messId"])

    private val _uiState = MutableStateFlow(GuestMealsUiState())
    val uiState: StateFlow<GuestMealsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                combine(
                    guestMealRepository.getGuestMealsFlow(messId),
                    memberRepository.getMembersFlow(messId)
                ) { guestMeals, members ->
                    GuestMealsUiState(
                        guestMeals = guestMeals,
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

    fun getGuestMeal(id: String): GuestMeal? {
        return _uiState.value.guestMeals.find { it.id == id }
    }

    fun addGuestMeal(messId: String, hostId: String, meal: String, count: Int, note: String) {
        viewModelScope.launch {
            try {
                val guestMeal = GuestMeal(
                    id = UUID.randomUUID().toString(),
                    hostId = hostId,
                    date = System.currentTimeMillis().toString(), // Should format properly
                    meal = meal,
                    count = count,
                    note = note
                )
                guestMealRepository.addGuestMeal(messId, guestMeal)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateGuestMeal(id: String, hostId: String, meal: String, count: Int, note: String) {
        viewModelScope.launch {
            try {
                val existing = getGuestMeal(id) ?: return@launch
                val updated = existing.copy(
                    hostId = hostId,
                    meal = meal,
                    count = count,
                    note = note
                )
                guestMealRepository.updateGuestMeal(messId, updated)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteGuestMeal(id: String) {
        viewModelScope.launch {
            try {
                guestMealRepository.deleteGuestMeal(messId, id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
