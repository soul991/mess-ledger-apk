package com.messledger.app.util

import com.messledger.app.data.model.Contribution
import com.messledger.app.data.model.Expense
import com.messledger.app.data.model.GuestMeal
import com.messledger.app.data.model.MealStatus
import com.messledger.app.data.model.Member
import java.util.Calendar

data class MemberSettlementRow(
    val member: Member,
    val ownMeals: Int,
    val guestMeals: Int,
    val effectiveMeals: Int,
    val share: Double,
    val contribPaid: Double,
    val directPaid: Double,
    val paid: Double,
    val balance: Double
)

data class SettlementResult(
    val monthKey: String,
    val effectiveDays: Int,
    val daysInMonth: Int,
    val totalMeals: Int,
    val mealRate: Double,
    val poolMeals: Double,
    val poolEqual: Double,
    val totalExpense: Double,
    val contribTotal: Double,
    val rows: List<MemberSettlementRow>,
    val orphanedMealPool: Double
)

object SettlementCalculator {

    /**
     * Ports calcSettlement(monthKey) faithfully from index.html (lines 994-1078).
     * Filters to active members (members.filter { it.isActive }) for counts and attendance.
     */
    fun calculateSettlement(
        monthKey: String, // "YYYY-MM"
        members: List<Member>,
        meals: Map<String, Map<String, MealStatus>>,
        guestMeals: List<GuestMeal>,
        expenses: List<Expense>,
        contributions: List<Contribution>,
        now: Calendar = Calendar.getInstance()
    ): SettlementResult {
        val parts = monthKey.split("-")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: now.get(Calendar.YEAR)
        val mo = parts.getOrNull(1)?.toIntOrNull() ?: (now.get(Calendar.MONTH) + 1)

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, y)
            set(Calendar.MONTH, mo - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val curYear = now.get(Calendar.YEAR)
        val curMonth = now.get(Calendar.MONTH) + 1
        val curDay = now.get(Calendar.DAY_OF_MONTH)

        val isCurrentMonth = (curYear == y && curMonth == mo)
        val isFutureMonth = (y > curYear || (y == curYear && mo > curMonth))

        val effectiveDays = when {
            isFutureMonth -> 0
            isCurrentMonth -> curDay
            else -> daysInMonth
        }

        val activeMembers = members.filter { it.isActive }
        val mealsByMember = mutableMapOf<String, Int>()

        activeMembers.forEach { m ->
            val joined = if (m.joinedAt.isNotBlank()) m.joinedAt else "0000-00-00"
            var daysPresent = 0
            for (day in 1..effectiveDays) {
                val dateStr = "$monthKey-${day.toString().padStart(2, '0')}"
                if (dateStr >= joined) {
                    daysPresent++
                }
            }
            mealsByMember[m.id] = daysPresent * 2
        }

        meals.forEach { (dateStr, dayData) ->
            if (dateStr.startsWith(monthKey)) {
                val partsDate = dateStr.split("-")
                val day = partsDate.getOrNull(2)?.toIntOrNull() ?: 0
                if (day in 1..effectiveDays) {
                    dayData.forEach { (memId, rec) ->
                        if (mealsByMember.containsKey(memId)) {
                            var current = mealsByMember[memId] ?: 0
                            if (rec.lunchAbsent) current--
                            if (rec.dinnerAbsent) current--
                            mealsByMember[memId] = current
                        }
                    }
                }
            }
        }

        val guestMealsByMember = mutableMapOf<String, Int>()
        activeMembers.forEach { m -> guestMealsByMember[m.id] = 0 }
        guestMeals.forEach { g ->
            if (g.date.startsWith(monthKey)) {
                val partsDate = g.date.split("-")
                val day = partsDate.getOrNull(2)?.toIntOrNull() ?: 0
                if (day in 1..effectiveDays) {
                    if (guestMealsByMember.containsKey(g.hostId)) {
                        guestMealsByMember[g.hostId] = (guestMealsByMember[g.hostId] ?: 0) + g.count
                    }
                }
            }
        }

        val effectiveMeals = mutableMapOf<String, Int>()
        var totalMeals = 0
        activeMembers.forEach { m ->
            val total = (mealsByMember[m.id] ?: 0) + (guestMealsByMember[m.id] ?: 0)
            effectiveMeals[m.id] = total
            totalMeals += total
        }

        val monthExpenses = expenses.filter { e ->
            if (!e.date.startsWith(monthKey)) return@filter false
            val partsDate = e.date.split("-")
            val day = partsDate.getOrNull(2)?.toIntOrNull() ?: 0
            day in 1..effectiveDays
        }

        val poolMeals = monthExpenses.filter { it.splitType == "meals" }.sumOf { it.amount }
        val poolEqual = monthExpenses.filter { it.splitType == "equal" }.sumOf { it.amount }
        val mealRate = if (totalMeals > 0) poolMeals / totalMeals else 0.0
        val orphanedMealPool = if (totalMeals == 0) poolMeals else 0.0
        val equalShare = if (activeMembers.isNotEmpty()) (poolEqual + orphanedMealPool) / activeMembers.size else 0.0

        val monthContribs = contributions.filter { c ->
            if (!c.date.startsWith(monthKey)) return@filter false
            val partsDate = c.date.split("-")
            val day = partsDate.getOrNull(2)?.toIntOrNull() ?: 0
            day in 1..effectiveDays
        }

        val rows = activeMembers.map { m ->
            val memEffectiveMeals = effectiveMeals[m.id] ?: 0
            val share = mealRate * memEffectiveMeals + equalShare
            val contribPaid = monthContribs.filter { it.memberId == m.id }.sumOf { it.amount }
            val directPaid = monthExpenses.filter { it.paidBy == m.id }.sumOf { it.amount }
            val paid = contribPaid + directPaid
            val balance = paid - share
            MemberSettlementRow(
                member = m,
                ownMeals = mealsByMember[m.id] ?: 0,
                guestMeals = guestMealsByMember[m.id] ?: 0,
                effectiveMeals = memEffectiveMeals,
                share = share,
                contribPaid = contribPaid,
                directPaid = directPaid,
                paid = paid,
                balance = balance
            )
        }

        return SettlementResult(
            monthKey = monthKey,
            effectiveDays = effectiveDays,
            daysInMonth = daysInMonth,
            totalMeals = totalMeals,
            mealRate = mealRate,
            poolMeals = poolMeals,
            poolEqual = poolEqual,
            totalExpense = poolMeals + poolEqual,
            contribTotal = monthContribs.sumOf { it.amount },
            rows = rows,
            orphanedMealPool = orphanedMealPool
        )
    }

    /**
     * Ports fundBalance() from index.html (lines 1079-1083):
     * All-time contributions minus all-time expenses paid by 'fund'.
     */
    fun calculateFundBalance(
        contributions: List<Contribution>,
        expenses: List<Expense>
    ): Double {
        val totalIn = contributions.sumOf { it.amount }
        val totalOut = expenses.filter { it.paidBy.equals("fund", ignoreCase = true) }.sumOf { it.amount }
        return totalIn - totalOut
    }
}
