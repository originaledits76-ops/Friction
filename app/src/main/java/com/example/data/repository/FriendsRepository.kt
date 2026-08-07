package com.example.data.repository

import android.util.Log
import com.example.data.model.FriendInfo
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class FriendsRepository(private val firestoreService: FirestoreService) {
    private val tag = "FriendsRepository"

    fun getFriendsFlow(userUid: String): Flow<List<FriendInfo>> = flow {
        if (userUid.isEmpty() || userUid.startsWith("offline_") || userUid.startsWith("guest_")) {
            emit(emptyList())
            return@flow
        }

        try {
            // 1. Fetch established friends
            val friendsSnapshot = firestoreService.db.collection("friends")
                .whereEqualTo("userUid", userUid)
                .get().await()

            val friendsList = mutableListOf<FriendInfo>()

            for (doc in friendsSnapshot.documents) {
                val friendUid = doc.getString("friendUid") ?: continue
                // Fetch latest friend details from users collection
                val userDoc = firestoreService.db.collection("users").document(friendUid).get().await()
                if (userDoc.exists()) {
                    friendsList.add(
                        FriendInfo(
                            uid = friendUid,
                            displayName = userDoc.getString("displayName") ?: "Friction Friend",
                            email = userDoc.getString("email") ?: "",
                            currentStreak = userDoc.getLong("currentStreak")?.toInt() ?: 0,
                            level = userDoc.getLong("level")?.toInt() ?: 1,
                            xp = userDoc.getLong("xp")?.toInt() ?: 0,
                            status = "FRIEND"
                        )
                    )
                }
            }

            // 2. Fetch pending incoming requests
            val incomingRequests = firestoreService.db.collection("friend_requests")
                .whereEqualTo("toUid", userUid)
                .whereEqualTo("status", "PENDING")
                .get().await()

            for (doc in incomingRequests.documents) {
                val fromUid = doc.getString("fromUid") ?: continue
                val userDoc = firestoreService.db.collection("users").document(fromUid).get().await()
                if (userDoc.exists()) {
                    friendsList.add(
                        FriendInfo(
                            uid = fromUid,
                            displayName = userDoc.getString("displayName") ?: "Friction Friend",
                            email = userDoc.getString("email") ?: "",
                            currentStreak = userDoc.getLong("currentStreak")?.toInt() ?: 0,
                            level = userDoc.getLong("level")?.toInt() ?: 1,
                            xp = userDoc.getLong("xp")?.toInt() ?: 0,
                            status = "RECEIVED"
                        )
                    )
                }
            }

            // 3. Fetch pending outgoing requests
            val outgoingRequests = firestoreService.db.collection("friend_requests")
                .whereEqualTo("fromUid", userUid)
                .whereEqualTo("status", "PENDING")
                .get().await()

            for (doc in outgoingRequests.documents) {
                val toUid = doc.getString("toUid") ?: continue
                val userDoc = firestoreService.db.collection("users").document(toUid).get().await()
                if (userDoc.exists()) {
                    friendsList.add(
                        FriendInfo(
                            uid = toUid,
                            displayName = userDoc.getString("displayName") ?: "Friction Friend",
                            email = userDoc.getString("email") ?: "",
                            currentStreak = userDoc.getLong("currentStreak")?.toInt() ?: 0,
                            level = userDoc.getLong("level")?.toInt() ?: 1,
                            xp = userDoc.getLong("xp")?.toInt() ?: 0,
                            status = "SENT"
                        )
                    )
                }
            }

            emit(friendsList)
        } catch (e: Exception) {
            Log.w(tag, "Firestore friends access restricted: ${e.message}")
            emit(emptyList())
        }
    }

    suspend fun sendFriendRequest(userUid: String, targetEmail: String): Boolean {
        if (userUid.isEmpty() || userUid.startsWith("offline_")) return false

        try {
            // Find target user in users collection
            val userQuery = firestoreService.db.collection("users")
                .whereEqualTo("email", targetEmail)
                .get().await()

            if (!userQuery.isEmpty) {
                val targetUserDoc = userQuery.documents.first()
                val targetUid = targetUserDoc.id

                if (targetUid == userUid) return false // Cannot add yourself

                val requestId = "${userUid}_${targetUid}"
                val requestMap = mapOf(
                    "fromUid" to userUid,
                    "toUid" to targetUid,
                    "status" to "PENDING",
                    "timestamp" to System.currentTimeMillis()
                )

                firestoreService.db.collection("friend_requests").document(requestId).set(requestMap).await()
                Log.d(tag, "Friend request sent to $targetEmail")
                return true
            }
        } catch (e: Exception) {
            Log.e(tag, "Error sending friend request: ${e.message}", e)
        }
        return false
    }

    suspend fun acceptFriendRequest(userUid: String, friendUid: String) {
        if (userUid.isEmpty() || userUid.startsWith("offline_")) return

        try {
            // 1. Delete friend request
            val requestId1 = "${friendUid}_${userUid}"
            val requestId2 = "${userUid}_${friendUid}"
            firestoreService.db.collection("friend_requests").document(requestId1).delete().await()
            firestoreService.db.collection("friend_requests").document(requestId2).delete().await()

            // 2. Create friend links in friends collection (both directions)
            val docId1 = "${userUid}_${friendUid}"
            firestoreService.db.collection("friends").document(docId1).set(
                mapOf(
                    "userUid" to userUid,
                    "friendUid" to friendUid,
                    "timestamp" to System.currentTimeMillis()
                )
            ).await()

            val docId2 = "${friendUid}_${userUid}"
            firestoreService.db.collection("friends").document(docId2).set(
                mapOf(
                    "userUid" to friendUid,
                    "friendUid" to userUid,
                    "timestamp" to System.currentTimeMillis()
                )
            ).await()

            Log.d(tag, "Friend request accepted between $userUid and $friendUid")
        } catch (e: Exception) {
            Log.e(tag, "Error accepting friend request: ${e.message}", e)
        }
    }

    suspend fun rejectFriendRequest(userUid: String, friendUid: String) {
        if (userUid.isEmpty() || userUid.startsWith("offline_")) return

        try {
            val requestId1 = "${friendUid}_${userUid}"
            val requestId2 = "${userUid}_${friendUid}"
            firestoreService.db.collection("friend_requests").document(requestId1).delete().await()
            firestoreService.db.collection("friend_requests").document(requestId2).delete().await()
            Log.d(tag, "Friend request rejected between $userUid and $friendUid")
        } catch (e: Exception) {
            Log.e(tag, "Error rejecting friend request: ${e.message}", e)
        }
    }
}
