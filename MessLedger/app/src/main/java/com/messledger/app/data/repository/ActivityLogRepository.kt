package com.messledger.app.data.repository

import com.messledger.app.data.model.ActivityLogEntry
import com.messledger.app.data.remote.FirestoreService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class ActivityLogRepository @Inject constructor(
    private val firestoreService: FirestoreService
) {
    fun getActivityLogFlow(messId: String, limit: Int = 50): Flow<List<ActivityLogEntry>> {
        return firestoreService.getActivityLogFlow(messId, limit)
    }

    suspend fun loadMoreActivities(messId: String, lastTimestamp: Long, limit: Int = 50): List<ActivityLogEntry> {
        return try {
            firestoreService.getMoreActivityLogs(messId, lastTimestamp, limit)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
