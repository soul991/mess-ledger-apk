package com.messledger.app.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.messledger.app.data.model.User
import com.messledger.app.util.Constants
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Singleton
class FirebaseAuthService @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    suspend fun register(name: String, username: String, password: String) {
        val email = "$username@${Constants.SYNTHETIC_EMAIL_DOMAIN}"
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: throw Exception("User creation failed")

        val user = User(
            uid = uid,
            name = name,
            username = username
        )

        firestore.runBatch { batch ->
            // Create user document
            batch.set(firestore.collection("users").document(uid), user)
            // Create username mapping document for uniqueness
            batch.set(
                firestore.collection("usernames").document(username),
                mapOf("uid" to uid)
            )
        }.await()
    }

    suspend fun login(username: String, password: String) {
        val email = "$username@${Constants.SYNTHETIC_EMAIL_DOMAIN}"
        auth.signInWithEmailAndPassword(email, password).await()
    }

    fun logout() {
        auth.signOut()
    }

    fun isLoggedIn(): Boolean = auth.currentUser != null

    suspend fun getCurrentUser(): User? {
        val uid = auth.currentUser?.uid ?: return null
        val snapshot = firestore.collection("users").document(uid).get().await()
        return snapshot.toObject(User::class.java)
    }

    suspend fun checkUsernameAvailability(username: String): Boolean {
        val snapshot = firestore.collection("usernames").document(username).get().await()
        return !snapshot.exists()
    }

    fun getAuthStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }
}
