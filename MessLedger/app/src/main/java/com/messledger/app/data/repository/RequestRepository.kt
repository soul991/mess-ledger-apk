package com.messledger.app.data.repository

import com.messledger.app.data.model.JoinRequest
import com.messledger.app.data.model.LeaveRequest
import com.messledger.app.data.remote.FirebaseAuthService
import com.messledger.app.data.remote.FirestoreService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

import kotlinx.coroutines.tasks.await

@Singleton
class RequestRepository @Inject constructor(
    private val firestoreService: FirestoreService,
    private val authService: FirebaseAuthService
) {
    suspend fun submitJoinRequest(messId: String): Result<Unit> {
        return try {
            val user = authService.getCurrentUser() ?: return Result.failure(Exception("Not logged in"))
            val request = JoinRequest(
                uid = user.uid,
                name = user.name,
                requestedAt = System.currentTimeMillis()
            )
            firestoreService.submitJoinRequest(messId, request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getJoinRequestsFlow(messId: String): Flow<List<JoinRequest>> {
        return firestoreService.getJoinRequestsFlow(messId)
    }

    fun getLeaveRequestsFlow(messId: String): Flow<List<LeaveRequest>> {
        return firestoreService.getLeaveRequestsFlow(messId)
    }

    fun getMyLeaveRequestFlow(messId: String): Flow<LeaveRequest?> {
        val uid = authService.isLoggedIn().let { 
            if (it) com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "" else "" 
        }
        return firestoreService.getMyLeaveRequestFlow(messId, uid)
    }

    suspend fun approveJoinRequest(messId: String, uid: String): Result<Unit> {
        return try {
            val userDoc = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
            val name = userDoc.getString("name") ?: "Unknown"
            firestoreService.approveJoinRequest(messId, uid, name)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectJoinRequest(messId: String, uid: String): Result<Unit> {
        return try {
            firestoreService.rejectJoinRequest(messId, uid)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitLeaveRequest(messId: String, reason: String?): Result<Unit> {
        return try {
            val user = authService.getCurrentUser() ?: return Result.failure(Exception("Not logged in"))
            val request = LeaveRequest(
                uid = user.uid,
                name = user.name,
                reason = reason,
                requestedAt = System.currentTimeMillis()
            )
            firestoreService.submitLeaveRequest(messId, request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun approveLeaveRequest(messId: String, uid: String): Result<Unit> {
        return try {
            firestoreService.approveLeaveRequest(messId, uid)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectLeaveRequest(messId: String, uid: String, reason: String?): Result<Unit> {
        return try {
            firestoreService.rejectLeaveRequest(messId, uid, reason)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun withdrawLeaveRequest(messId: String): Result<Unit> {
        return try {
            val user = authService.getCurrentUser() ?: return Result.failure(Exception("Not logged in"))
            firestoreService.withdrawLeaveRequest(messId, user.uid)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
