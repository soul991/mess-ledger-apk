package com.messledger.app.data.repository

import com.messledger.app.data.local.dao.MessDao
import com.messledger.app.data.local.entity.CachedMess
import com.messledger.app.data.model.Mess
import com.messledger.app.data.remote.FirebaseAuthService
import com.messledger.app.data.remote.FirestoreService
import com.messledger.app.util.MessIdGenerator
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class MessRepository @Inject constructor(
    private val firestoreService: FirestoreService,
    private val authService: FirebaseAuthService,
    private val messDao: MessDao
) {
    suspend fun createMess(name: String, categories: List<String>): Result<String> {
        return try {
            val user = authService.getCurrentUser() ?: return Result.failure(Exception("Not logged in"))
            val messId = generateUniqueMessId()
            val mess = Mess(
                id = messId,
                messName = name,
                categories = categories,
                createdAt = System.currentTimeMillis(),
                createdBy = user.uid
            )
            firestoreService.createMess(mess, user.uid, user.name)
            messDao.insertMess(CachedMess(mess.id, mess.messName, mess.categories, mess.createdAt))
            Result.success(messId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshUserMesses(uid: String): Result<Unit> {
        return try {
            val messes = firestoreService.getUserMesses(uid)
            val cachedMesses = messes.map { CachedMess(it.id, it.messName, it.categories, it.createdAt) }
            messDao.insertMesses(cachedMesses)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllMesses(): Flow<List<Mess>> {
        return messDao.getAllMesses().map { list ->
            list.map { Mess(it.id, it.messName, it.categories, it.createdAt) }
        }
    }

    fun getUserMessesFlow(uid: String): Flow<List<Mess>> {
        return firestoreService.getUserMessesFlow(uid)
    }

    fun getMessFlow(messId: String): Flow<Mess?> {
        return firestoreService.getMess(messId)
    }

    suspend fun getMess(messId: String): Mess? {
        return firestoreService.getMessById(messId)
    }

    suspend fun updateMess(mess: Mess): Result<Unit> {
        return try {
            firestoreService.updateMessDoc(mess)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Mirrors the original app's generateUniqueMessId(): short, human-shareable code
    // (Constants.MESS_ID_CHARS/MESS_ID_LENGTH), retried against real collisions rather
    // than trusting randomness alone — a raw UUID is not what invite links/screens expect.
    private suspend fun generateUniqueMessId(): String {
        repeat(8) {
            val candidate = MessIdGenerator.generateMessId()
            if (firestoreService.getMessById(candidate) == null) return candidate
        }
        throw Exception("Could not generate a unique mess ID — please try again.")
    }
}
