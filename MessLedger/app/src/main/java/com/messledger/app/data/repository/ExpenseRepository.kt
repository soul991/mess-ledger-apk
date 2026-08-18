package com.messledger.app.data.repository

import com.messledger.app.data.model.Expense
import com.messledger.app.data.remote.FirestoreService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class ExpenseRepository @Inject constructor(
    private val firestoreService: FirestoreService
) {
    fun getExpensesFlow(messId: String): Flow<List<Expense>> {
        return firestoreService.getExpensesFlow(messId)
    }

    suspend fun addExpense(messId: String, expense: Expense): Result<Unit> {
        return try {
            firestoreService.addExpense(messId, expense)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateExpense(messId: String, expense: Expense): Result<Unit> {
        return try {
            firestoreService.updateExpense(messId, expense)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(messId: String, expenseId: String): Result<Unit> {
        return try {
            firestoreService.deleteExpense(messId, expenseId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
