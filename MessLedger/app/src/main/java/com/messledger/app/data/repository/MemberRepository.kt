package com.messledger.app.data.repository

import com.messledger.app.data.local.dao.MemberDao
import com.messledger.app.data.model.Member
import com.messledger.app.data.remote.FirestoreService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class MemberRepository @Inject constructor(
    private val firestoreService: FirestoreService,
    private val memberDao: MemberDao
) {
    fun getMembersFlow(messId: String): Flow<List<Member>> {
        return firestoreService.getMembers(messId)
    }

    suspend fun removeMember(messId: String, uid: String): Result<Unit> {
        return try {
            firestoreService.removeMember(messId, uid)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun transferManager(messId: String, currentManagerId: String, newManagerId: String): Result<Unit> {
        return try {
            firestoreService.transferManager(messId, currentManagerId, newManagerId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
