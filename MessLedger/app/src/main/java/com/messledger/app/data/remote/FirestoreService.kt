package com.messledger.app.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.messledger.app.data.model.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Singleton
class FirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    // --- Messes ---
    suspend fun createMess(mess: Mess, creatorUid: String, creatorName: String) {
        val messRef = firestore.collection("messes").document(mess.id)
        val memberRef = messRef.collection("members").document(creatorUid)
        val userRef = firestore.collection("users").document(creatorUid)
        
        val member = Member(
            id = creatorUid,
            name = creatorName,
            role = "manager",
            joinedAt = com.messledger.app.util.DateUtils.today()
        )

        firestore.runBatch { batch ->
            batch.set(messRef, mess)
            batch.set(memberRef, member)
            batch.update(userRef, "messMemberships", FieldValue.arrayUnion(mess.id))
        }.await()
    }

    fun getMess(messId: String): Flow<Mess?> = callbackFlow {
        val listener = firestore.collection("messes").document(messId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObject(Mess::class.java))
            }
        awaitClose { listener.remove() }
    }
    
    suspend fun getMessById(messId: String): Mess? {
        val snapshot = firestore.collection("messes").document(messId).get().await()
        return snapshot.toObject(Mess::class.java)
    }

    suspend fun updateMess(messId: String, updates: Map<String, Any>) {
        firestore.collection("messes").document(messId).update(updates).await()
    }

    suspend fun updateMessDoc(mess: Mess) {
        firestore.collection("messes").document(mess.id).set(mess).await()
    }

    suspend fun getUserMesses(uid: String): List<Mess> {
        val userDoc = firestore.collection("users").document(uid).get().await()
        val user = userDoc.toObject(User::class.java) ?: return emptyList()
        val messIds = user.messMemberships
        if (messIds.isEmpty()) return emptyList()
        
        val chunks = messIds.chunked(30)
        val messes = mutableListOf<Mess>()
        for (chunk in chunks) {
            val snapshot = firestore.collection("messes")
                .whereIn("id", chunk)
                .get()
                .await()
            messes.addAll(snapshot.toObjects(Mess::class.java))
        }
        return messes
    }

    fun getUserMessesFlow(uid: String): Flow<List<Mess>> = callbackFlow {
        var messesListener: com.google.firebase.firestore.ListenerRegistration? = null
        val userListener = firestore.collection("users").document(uid)
            .addSnapshotListener { userDoc, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val user = userDoc?.toObject(User::class.java)
                if (user == null || user.messMemberships.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val messIds = user.messMemberships
                val chunks = messIds.chunked(30)
                messesListener?.remove()
                messesListener = firestore.collection("messes").whereIn("id", chunks.first())
                    .addSnapshotListener { snapshot, err ->
                        if (err == null && snapshot != null) {
                            trySend(snapshot.toObjects(Mess::class.java))
                        }
                    }
            }
        awaitClose {
            userListener.remove()
            messesListener?.remove()
        }
    }

    // --- Members ---
    fun getMembers(messId: String): Flow<List<Member>> = callbackFlow {
        val listener = firestore.collection("messes").document(messId)
            .collection("members")
            .whereEqualTo("deletedAt", null)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(Member::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun removeMember(messId: String, uid: String) {
        val memberRef = firestore.collection("messes").document(messId)
            .collection("members").document(uid)
        val userRef = firestore.collection("users").document(uid)
        
        firestore.runBatch { batch ->
            batch.update(memberRef, "deletedAt", System.currentTimeMillis())
            batch.update(userRef, "messMemberships", FieldValue.arrayRemove(messId))
        }.await()
    }
    
    suspend fun transferManager(messId: String, currentManagerId: String, newManagerId: String) {
        val currentManagerRef = firestore.collection("messes").document(messId)
            .collection("members").document(currentManagerId)
        val newManagerRef = firestore.collection("messes").document(messId)
            .collection("members").document(newManagerId)
            
        firestore.runBatch { batch ->
            batch.update(currentManagerRef, "role", "member")
            batch.update(newManagerRef, "role", "manager")
        }.await()
    }

    // --- Meals ---
    fun getMeals(messId: String, monthYear: String): Flow<Map<String, Map<String, MealStatus>>> = callbackFlow {
        // As requested: meals are at messes/{messId}/meals/{dateStr} with members as keys
        val listener = firestore.collection("messes").document(messId)
            .collection("meals")
            .whereGreaterThanOrEqualTo(com.google.firebase.firestore.FieldPath.documentId(), "$monthYear-01")
            .whereLessThanOrEqualTo(com.google.firebase.firestore.FieldPath.documentId(), "$monthYear-31")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val result = mutableMapOf<String, Map<String, MealStatus>>()
                snapshot?.documents?.forEach { doc ->
                    val date = doc.id
                    val membersMap = mutableMapOf<String, MealStatus>()
                    doc.data?.forEach { (memberId, mealData) ->
                        if (mealData is Map<*, *>) {
                            val lunchAbsent = mealData["lunchAbsent"] as? Boolean ?: false
                            val dinnerAbsent = mealData["dinnerAbsent"] as? Boolean ?: false
                            membersMap[memberId] = MealStatus(lunchAbsent, dinnerAbsent)
                        }
                    }
                    result[date] = membersMap
                }
                trySend(result)
            }
        awaitClose { listener.remove() }
    }

    suspend fun setMealStatus(messId: String, date: String, uid: String, status: MealStatus) {
        val docRef = firestore.collection("messes").document(messId)
            .collection("meals").document(date)
        docRef.set(mapOf(uid to status), SetOptions.merge()).await()
    }

    suspend fun toggleMeal(messId: String, date: String, memberId: String, mealType: String, absent: Boolean) {
        val docRef = firestore.collection("messes").document(messId).collection("meals").document(date)
        try {
            docRef.update("$memberId.${mealType}Absent", absent).await()
        } catch (e: Exception) {
            docRef.set(mapOf(memberId to mapOf("${mealType}Absent" to absent)), SetOptions.merge()).await()
        }
    }

    // --- Expenses ---
    fun getExpensesFlow(messId: String): Flow<List<Expense>> = callbackFlow {
        val listener = firestore.collection("messes").document(messId).collection("expenses")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(Expense::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun addExpense(messId: String, expense: Expense) {
        val ref = firestore.collection("messes").document(messId).collection("expenses").document()
        ref.set(expense.copy(id = ref.id)).await()
    }

    suspend fun updateExpense(messId: String, expense: Expense) {
        firestore.collection("messes").document(messId).collection("expenses").document(expense.id).set(expense).await()
    }

    suspend fun deleteExpense(messId: String, expenseId: String) {
        firestore.collection("messes").document(messId).collection("expenses").document(expenseId).delete().await()
    }

    // --- Contributions ---
    fun getContributionsFlow(messId: String): Flow<List<Contribution>> = callbackFlow {
        val listener = firestore.collection("messes").document(messId).collection("contributions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(Contribution::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun addContribution(messId: String, contribution: Contribution) {
        val ref = firestore.collection("messes").document(messId).collection("contributions").document()
        ref.set(contribution.copy(id = ref.id)).await()
    }

    suspend fun updateContribution(messId: String, contribution: Contribution) {
        firestore.collection("messes").document(messId).collection("contributions").document(contribution.id).set(contribution).await()
    }

    suspend fun deleteContribution(messId: String, contributionId: String) {
        firestore.collection("messes").document(messId).collection("contributions").document(contributionId).delete().await()
    }

    // --- Guest Meals ---
    fun getGuestMealsFlow(messId: String): Flow<List<GuestMeal>> = callbackFlow {
        val listener = firestore.collection("messes").document(messId).collection("guestMeals")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(GuestMeal::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun addGuestMeal(messId: String, guestMeal: GuestMeal) {
        val ref = firestore.collection("messes").document(messId).collection("guestMeals").document()
        ref.set(guestMeal.copy(id = ref.id)).await()
    }

    suspend fun updateGuestMeal(messId: String, guestMeal: GuestMeal) {
        firestore.collection("messes").document(messId).collection("guestMeals").document(guestMeal.id).set(guestMeal).await()
    }

    suspend fun deleteGuestMeal(messId: String, guestMealId: String) {
        firestore.collection("messes").document(messId).collection("guestMeals").document(guestMealId).delete().await()
    }

    // --- Requests ---
    fun getJoinRequestsFlow(messId: String): Flow<List<JoinRequest>> = callbackFlow {
        val listener = firestore.collection("messes").document(messId).collection("joinRequests")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(JoinRequest::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    fun getLeaveRequestsFlow(messId: String): Flow<List<LeaveRequest>> = callbackFlow {
        val listener = firestore.collection("messes").document(messId).collection("leaveRequests")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(LeaveRequest::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    fun getMyLeaveRequestFlow(messId: String, uid: String): Flow<LeaveRequest?> = callbackFlow {
        val listener = firestore.collection("messes").document(messId).collection("leaveRequests").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObject(LeaveRequest::class.java))
            }
        awaitClose { listener.remove() }
    }

    suspend fun submitJoinRequest(messId: String, request: JoinRequest) {
        firestore.collection("messes").document(messId)
            .collection("joinRequests").document(request.uid)
            .set(request).await()
    }

    suspend fun approveJoinRequest(messId: String, uid: String, name: String) {
        val requestRef = firestore.collection("messes").document(messId).collection("joinRequests").document(uid)
        val memberRef = firestore.collection("messes").document(messId).collection("members").document(uid)
        val userRef = firestore.collection("users").document(uid)
        
        val member = Member(
            id = uid,
            name = name,
            role = "member",
            joinedAt = com.messledger.app.util.DateUtils.today()
        )
        
        firestore.runBatch { batch ->
            batch.set(memberRef, member)
            batch.update(userRef, "messMemberships", FieldValue.arrayUnion(messId))
            batch.delete(requestRef)
        }.await()
    }

    suspend fun rejectJoinRequest(messId: String, uid: String) {
        firestore.collection("messes").document(messId).collection("joinRequests").document(uid).delete().await()
    }

    suspend fun submitLeaveRequest(messId: String, request: LeaveRequest) {
        firestore.collection("messes").document(messId).collection("leaveRequests").document(request.uid).set(request).await()
    }

    suspend fun approveLeaveRequest(messId: String, uid: String) {
        val requestRef = firestore.collection("messes").document(messId).collection("leaveRequests").document(uid)
        val memberRef = firestore.collection("messes").document(messId).collection("members").document(uid)
        val userRef = firestore.collection("users").document(uid)
        
        firestore.runBatch { batch ->
            batch.update(memberRef, "deletedAt", System.currentTimeMillis())
            batch.update(userRef, "messMemberships", FieldValue.arrayRemove(messId))
            batch.delete(requestRef)
        }.await()
    }

    suspend fun rejectLeaveRequest(messId: String, uid: String, reason: String?) {
        firestore.collection("messes").document(messId).collection("leaveRequests").document(uid).delete().await()
    }

    suspend fun withdrawLeaveRequest(messId: String, uid: String) {
        firestore.collection("messes").document(messId).collection("leaveRequests").document(uid).delete().await()
    }

    // --- Activity Log ---
    fun getActivityLogFlow(messId: String, limit: Int): Flow<List<ActivityLogEntry>> = callbackFlow {
        val listener = firestore.collection("messes").document(messId).collection("activityLog")
            .orderBy("timestamp", Query.Direction.DESCENDING).limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(ActivityLogEntry::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun getMoreActivityLogs(messId: String, lastTimestamp: Long, limit: Int): List<ActivityLogEntry> {
        val snapshot = firestore.collection("messes").document(messId).collection("activityLog")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .startAfter(lastTimestamp)
            .limit(limit.toLong())
            .get().await()
        return snapshot.toObjects(ActivityLogEntry::class.java)
    }

    suspend fun logActivity(messId: String, entry: ActivityLogEntry) {
        val ref = firestore.collection("messes").document(messId).collection("activityLog").document()
        ref.set(entry.copy(id = ref.id)).await()
    }
}
