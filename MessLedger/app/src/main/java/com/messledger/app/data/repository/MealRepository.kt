package com.messledger.app.data.repository

import com.messledger.app.data.model.MealStatus
import com.messledger.app.data.remote.FirestoreService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class MealRepository @Inject constructor(
    private val firestoreService: FirestoreService
) {
    fun getMeals(messId: String, monthYear: String): Flow<Map<String, Map<String, MealStatus>>> {
        return firestoreService.getMeals(messId, monthYear)
    }

    suspend fun setMealStatus(messId: String, date: String, uid: String, status: MealStatus): Result<Unit> {
        return try {
            firestoreService.setMealStatus(messId, date, uid, status)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleMeal(messId: String, date: String, memberId: String, mealType: String, absent: Boolean): Result<Unit> {
        return try {
            firestoreService.toggleMeal(messId, date, memberId, mealType, absent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
