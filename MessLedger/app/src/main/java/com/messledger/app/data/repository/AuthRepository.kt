package com.messledger.app.data.repository

import com.messledger.app.data.model.User
import com.messledger.app.data.remote.FirebaseAuthService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

@Singleton
class AuthRepository @Inject constructor(
    private val authService: FirebaseAuthService
) {
    suspend fun register(name: String, username: String, password: String): Result<Unit> {
        return try {
            authService.register(name, username, password)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(username: String, password: String): Result<Unit> {
        return try {
            authService.login(username, password)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        authService.logout()
    }

    fun isLoggedIn(): Boolean {
        return authService.isLoggedIn()
    }

    fun getCurrentUser(): User? {
        return runBlocking {
            try {
                authService.getCurrentUser()
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun checkUsernameAvailability(username: String): Boolean {
        return authService.checkUsernameAvailability(username)
    }

    fun getAuthStateFlow(): Flow<User?> {
        return authService.getAuthStateFlow().map { firebaseUser ->
            if (firebaseUser != null) {
                try {
                    authService.getCurrentUser() ?: User(uid = firebaseUser.uid, name = firebaseUser.displayName ?: "")
                } catch (e: Exception) {
                    User(uid = firebaseUser.uid, name = firebaseUser.displayName ?: "")
                }
            } else null
        }
    }
}
