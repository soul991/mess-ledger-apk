package com.messledger.app.data.repository

import com.messledger.app.data.model.GuestMeal
import com.messledger.app.data.remote.FirestoreService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class GuestMealRepository @Inject constructor(
    private val firestoreService: FirestoreService
) {
    fun getGuestMealsFlow(messId: String): Flow<List<GuestMeal>> {
        return firestoreService.getGuestMealsFlow(messId)
    }

    suspend fun addGuestMeal(messId: String, guestMeal: GuestMeal): Result<Unit> {
        return try {
            firestoreService.addGuestMeal(messId, guestMeal)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGuestMeal(messId: String, guestMeal: GuestMeal): Result<Unit> {
        return try {
            firestoreService.updateGuestMeal(messId, guestMeal)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteGuestMeal(messId: String, guestMealId: String): Result<Unit> {
        return try {
            firestoreService.deleteGuestMeal(messId, guestMealId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
