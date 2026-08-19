package com.messledger.app.ui.mess.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messledger.app.data.model.Expense
import com.messledger.app.data.model.MealStatus
import com.messledger.app.data.model.Member
import com.messledger.app.data.repository.ContributionRepository
import com.messledger.app.data.repository.ExpenseRepository
import com.messledger.app.data.repository.GuestMealRepository
import com.messledger.app.data.repository.MealRepository
import com.messledger.app.data.repository.MemberRepository
import com.messledger.app.data.repository.MessRepository
import com.messledger.app.util.DateUtils
import com.messledger.app.util.SettlementCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val messRepository: MessRepository,
    private val memberRepository: MemberRepository,
    private val expenseRepository: ExpenseRepository,
    private val contributionRepository: ContributionRepository,
    private val mealRepository: MealRepository,
    private val guestMealRepository: GuestMealRepository
) : ViewModel() {

    private val messId: String = checkNotNull(savedStateHandle["messId"])

    private val currentMonthKey: String
        get() {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.ENGLISH)
            return sdf.format(Date())
        }

    val uiState: StateFlow<SummaryUiState> = combine(
        messRepository.getMessFlow(messId),
        memberRepository.getMembersFlow(messId),
        expenseRepository.getExpensesFlow(messId),
        contributionRepository.getContributionsFlow(messId),
        combine(
            mealRepository.getMeals(messId, currentMonthKey),
            guestMealRepository.getGuestMealsFlow(messId)
        ) { meals, guestMeals -> Pair(meals, guestMeals) }
    ) { mess, members, expenses, contributions, (meals, guestMeals) ->
        val todayStr = DateUtils.today()
        val monthKey = currentMonthKey

        // Active members filter for counts, attendance, and settlement
        val activeMembers = members.filter { it.isActive }

        // Settlement calculation for current month — pass the FULL member list, not
        // active-only: calculateSettlement needs a removed member's historical
        // footprint for months they were genuinely active in. It clamps their accrual
        // to [joinedAt, deletedAt] internally, so it stays correct without needing an
        // active-only filter here.
        val settlement = SettlementCalculator.calculateSettlement(
            monthKey = monthKey,
            members = members,
            meals = meals,
            guestMeals = guestMeals,
            expenses = expenses,
            contributions = contributions
        )

        // Fund balance: all-time contributions minus all-time expenses paid by fund
        val fundBalance = SettlementCalculator.calculateFundBalance(
            contributions = contributions,
            expenses = expenses
        )

        // Today's meals attendance (active members only)
        val dayData = meals[todayStr] ?: emptyMap()
        var lunchPresent = 0
        var dinnerPresent = 0
        activeMembers.forEach { m ->
            val rec = dayData[m.id] ?: MealStatus()
            if (!rec.lunchAbsent) lunchPresent++
            if (!rec.dinnerAbsent) dinnerPresent++
        }

        val guestsToday = guestMeals.filter { it.date == todayStr }.sumOf { it.count }

        // Recent expenses (last 5 sorted by date descending)
        val recentExpenses = expenses.sortedByDescending { it.date }.take(5)

        val totalExpenses = expenses.sumOf { it.amount }
        val totalContributions = contributions.sumOf { it.amount }

        SummaryUiState.Success(
            messId = messId,
            messName = mess?.messName ?: "",
            activeMemberCount = activeMembers.size,
            memberCount = activeMembers.size,
            balance = fundBalance,
            thisMonthExpenses = settlement.totalExpense,
            mealRate = settlement.mealRate,
            lunchPresent = lunchPresent,
            dinnerPresent = dinnerPresent,
            guestsToday = guestsToday,
            recentExpenses = recentExpenses,
            allMembers = members, // Full member list (including removed) for name lookup
            totalExpenses = totalExpenses,
            totalContributions = totalContributions,
            todayDateStr = todayStr
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
        val messId: String,
        val messName: String,
        val activeMemberCount: Int,
        val memberCount: Int,
        val balance: Double,
        val thisMonthExpenses: Double,
        val mealRate: Double,
        val lunchPresent: Int,
        val dinnerPresent: Int,
        val guestsToday: Int,
        val recentExpenses: List<Expense>,
        val allMembers: List<Member>,
        val totalExpenses: Double,
        val totalContributions: Double,
        val todayDateStr: String
    ) : SummaryUiState
    data class Error(val message: String) : SummaryUiState
}
