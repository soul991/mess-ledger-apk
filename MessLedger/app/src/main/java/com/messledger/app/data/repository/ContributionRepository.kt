package com.messledger.app.data.repository

import com.messledger.app.data.model.Contribution
import com.messledger.app.data.remote.FirestoreService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class ContributionRepository @Inject constructor(
    private val firestoreService: FirestoreService
) {
    fun getContributionsFlow(messId: String): Flow<List<Contribution>> {
        return firestoreService.getContributionsFlow(messId)
    }

    suspend fun addContribution(messId: String, contribution: Contribution): Result<Unit> {
        return try {
            firestoreService.addContribution(messId, contribution)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateContribution(messId: String, contribution: Contribution): Result<Unit> {
        return try {
            firestoreService.updateContribution(messId, contribution)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteContribution(messId: String, contributionId: String): Result<Unit> {
        return try {
            firestoreService.deleteContribution(messId, contributionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
