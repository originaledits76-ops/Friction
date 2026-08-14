package com.example.data.repository

import android.util.Log
import com.example.data.model.BuddyAppUsage
import com.example.data.model.BuddyDetails
import com.example.data.model.FriendInfo
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class FriendsRepository(private val firestoreService: FirestoreService) {
    private val tag = "FriendsRepository"

    fun getFriendsFlow(userUid: String): Flow<List<FriendInfo>> = flow {
        val db = firestoreService.db
        if (db == null || userUid.isEmpty() || userUid.startsWith("offline_") || userUid.startsWith("guest_")) {
            emit(emptyList())
            return@flow
        }

        try {
            // 1. Fetch established friends
            val friendsSnapshot = db.collection("friends")
                .whereEqualTo("userUid", userUid)
                .get().await()

            val friendsList = mutableListOf<FriendInfo>()

            for (doc in friendsSnapshot.documents) {
                val friendUid = doc.getString("friendUid") ?: continue
                // Fetch latest friend details from users collection
                val userDoc = db.collection("users").document(friendUid).get().await()
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
            val incomingRequests = db.collection("friend_requests")
                .whereEqualTo("toUid", userUid)
                .whereEqualTo("status", "PENDING")
                .get().await()

            for (doc in incomingRequests.documents) {
                val fromUid = doc.getString("fromUid") ?: continue
                val userDoc = db.collection("users").document(fromUid).get().await()
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
            val outgoingRequests = db.collection("friend_requests")
                .whereEqualTo("fromUid", userUid)
                .whereEqualTo("status", "PENDING")
                .get().await()

            for (doc in outgoingRequests.documents) {
                val toUid = doc.getString("toUid") ?: continue
                val userDoc = db.collection("users").document(toUid).get().await()
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

    suspend fun searchUsers(currentUid: String, query: String): List<FriendInfo> {
        val db = firestoreService.db
        val trimmed = query.trim()
        if (db == null || trimmed.isEmpty()) return emptyList()

        val results = mutableMapOf<String, FriendInfo>()
        try {
            // 1. By email
            val emailDocs = db.collection("users")
                .whereEqualTo("email", trimmed)
                .get().await()
            for (doc in emailDocs.documents) {
                if (doc.id != currentUid) {
                    results[doc.id] = FriendInfo(
                        uid = doc.id,
                        displayName = doc.getString("displayName") ?: "Friction User",
                        email = doc.getString("email") ?: "",
                        currentStreak = doc.getLong("currentStreak")?.toInt() ?: 0,
                        level = doc.getLong("level")?.toInt() ?: 1,
                        xp = doc.getLong("xp")?.toInt() ?: 0,
                        status = "NONE"
                    )
                }
            }

            // 2. By displayName
            val nameDocs = db.collection("users")
                .whereEqualTo("displayName", trimmed)
                .get().await()
            for (doc in nameDocs.documents) {
                if (doc.id != currentUid && !results.containsKey(doc.id)) {
                    results[doc.id] = FriendInfo(
                        uid = doc.id,
                        displayName = doc.getString("displayName") ?: "Friction User",
                        email = doc.getString("email") ?: "",
                        currentStreak = doc.getLong("currentStreak")?.toInt() ?: 0,
                        level = doc.getLong("level")?.toInt() ?: 1,
                        xp = doc.getLong("xp")?.toInt() ?: 0,
                        status = "NONE"
                    )
                }
            }

            // 3. By UID document direct match
            val uidDoc = db.collection("users").document(trimmed).get().await()
            if (uidDoc.exists() && uidDoc.id != currentUid && !results.containsKey(uidDoc.id)) {
                results[uidDoc.id] = FriendInfo(
                    uid = uidDoc.id,
                    displayName = uidDoc.getString("displayName") ?: "Friction User",
                    email = uidDoc.getString("email") ?: "",
                    currentStreak = uidDoc.getLong("currentStreak")?.toInt() ?: 0,
                    level = uidDoc.getLong("level")?.toInt() ?: 1,
                    xp = uidDoc.getLong("xp")?.toInt() ?: 0,
                    status = "NONE"
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Error searching users: ${e.message}", e)
        }
        return results.values.toList()
    }

    suspend fun sendFriendRequest(userUid: String, queryOrEmail: String): Boolean {
        val db = firestoreService.db
        val targetQuery = queryOrEmail.trim()
        if (db == null || userUid.isEmpty() || userUid.startsWith("offline_") || targetQuery.isEmpty()) return false

        try {
            // Find target user in users collection by email, name, or UID
            val userQuery = db.collection("users")
                .whereEqualTo("email", targetQuery)
                .get().await()

            var targetUserDoc = if (!userQuery.isEmpty) userQuery.documents.first() else null

            if (targetUserDoc == null) {
                val nameQuery = db.collection("users")
                    .whereEqualTo("displayName", targetQuery)
                    .get().await()
                if (!nameQuery.isEmpty) {
                    targetUserDoc = nameQuery.documents.first()
                }
            }

            if (targetUserDoc == null) {
                val uidDoc = db.collection("users").document(targetQuery).get().await()
                if (uidDoc.exists()) {
                    targetUserDoc = uidDoc
                }
            }

            if (targetUserDoc != null) {
                val targetUid = targetUserDoc.id

                if (targetUid == userUid) return false // Cannot add yourself

                // Check if already friends
                val existingFriend = db.collection("friends").document("${userUid}_${targetUid}").get().await()
                if (existingFriend.exists()) return false

                val requestId = "${userUid}_${targetUid}"
                val requestMap = mapOf(
                    "fromUid" to userUid,
                    "toUid" to targetUid,
                    "status" to "PENDING",
                    "timestamp" to System.currentTimeMillis()
                )

                db.collection("friend_requests").document(requestId).set(requestMap).await()
                Log.d(tag, "Friend request sent from $userUid to $targetUid")
                return true
            }
        } catch (e: Exception) {
            Log.e(tag, "Error sending friend request: ${e.message}", e)
        }
        return false
    }

    suspend fun acceptFriendRequest(userUid: String, friendUid: String) {
        val db = firestoreService.db
        if (db == null || userUid.isEmpty() || userUid.startsWith("offline_")) return

        try {
            // 1. Delete friend request
            val requestId1 = "${friendUid}_${userUid}"
            val requestId2 = "${userUid}_${friendUid}"
            db.collection("friend_requests").document(requestId1).delete().await()
            db.collection("friend_requests").document(requestId2).delete().await()

            // 2. Create friend links in friends collection (both directions)
            val docId1 = "${userUid}_${friendUid}"
            db.collection("friends").document(docId1).set(
                mapOf(
                    "userUid" to userUid,
                    "friendUid" to friendUid,
                    "timestamp" to System.currentTimeMillis()
                )
            ).await()

            val docId2 = "${friendUid}_${userUid}"
            db.collection("friends").document(docId2).set(
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
        val db = firestoreService.db
        if (db == null || userUid.isEmpty() || userUid.startsWith("offline_")) return

        try {
            val requestId1 = "${friendUid}_${userUid}"
            val requestId2 = "${userUid}_${friendUid}"
            db.collection("friend_requests").document(requestId1).delete().await()
            db.collection("friend_requests").document(requestId2).delete().await()
            Log.d(tag, "Friend request rejected between $userUid and $friendUid")
        } catch (e: Exception) {
            Log.e(tag, "Error rejecting friend request: ${e.message}", e)
        }
    }

    suspend fun getAllAppUsersWithStatus(currentUid: String): List<FriendInfo> {
        val db = firestoreService.db ?: return emptyList()
        if (currentUid.isEmpty()) return emptyList()

        try {
            // 1. Get all established friend uids
            val friendsSnap = db.collection("friends")
                .whereEqualTo("userUid", currentUid)
                .get().await()
            val friendUids = friendsSnap.documents.mapNotNull { it.getString("friendUid") }.toSet()

            // 2. Get all outgoing pending request target uids
            val outgoingSnap = db.collection("friend_requests")
                .whereEqualTo("fromUid", currentUid)
                .whereEqualTo("status", "PENDING")
                .get().await()
            val outgoingUids = outgoingSnap.documents.mapNotNull { it.getString("toUid") }.toSet()

            // 3. Get all incoming pending request sender uids
            val incomingSnap = db.collection("friend_requests")
                .whereEqualTo("toUid", currentUid)
                .whereEqualTo("status", "PENDING")
                .get().await()
            val incomingUids = incomingSnap.documents.mapNotNull { it.getString("fromUid") }.toSet()

            // 4. Fetch all users from users collection
            val usersSnap = db.collection("users").get().await()
            val userList = mutableListOf<FriendInfo>()

            for (doc in usersSnap.documents) {
                val uid = doc.id
                if (uid == currentUid) continue

                val status = when {
                    friendUids.contains(uid) -> "FRIEND"
                    outgoingUids.contains(uid) -> "SENT"
                    incomingUids.contains(uid) -> "RECEIVED"
                    else -> "NONE"
                }

                val rawName = doc.getString("displayName") ?: ""
                val displayName = if (rawName.isBlank()) {
                    val email = doc.getString("email") ?: ""
                    if (email.isNotBlank()) email.substringBefore("@") else "Focus User ${uid.takeLast(4)}"
                } else rawName

                userList.add(
                    FriendInfo(
                        uid = uid,
                        displayName = displayName,
                        email = "", // Privacy: Never expose emails
                        currentStreak = doc.getLong("currentStreak")?.toInt() ?: 0,
                        level = doc.getLong("level")?.toInt() ?: 1,
                        xp = doc.getLong("xp")?.toInt() ?: 0,
                        status = status
                    )
                )
            }

            return userList.sortedBy { it.displayName.lowercase() }
        } catch (e: Exception) {
            Log.e(tag, "Error loading browse friends list: ${e.message}", e)
            return emptyList()
        }
    }

    suspend fun sendFriendRequestToUid(userUid: String, targetUid: String): Boolean {
        val db = firestoreService.db
        if (db == null || userUid.isEmpty() || targetUid.isEmpty() || userUid == targetUid) return false

        try {
            val existingFriend = db.collection("friends").document("${userUid}_${targetUid}").get().await()
            if (existingFriend.exists()) return false

            val requestId = "${userUid}_${targetUid}"
            val requestMap = mapOf(
                "fromUid" to userUid,
                "toUid" to targetUid,
                "status" to "PENDING",
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("friend_requests").document(requestId).set(requestMap).await()
            Log.d(tag, "Direct friend request sent from $userUid to $targetUid")
            return true
        } catch (e: Exception) {
            Log.e(tag, "Error sending direct friend request: ${e.message}", e)
            return false
        }
    }

    suspend fun getBuddyDetails(buddyUid: String): BuddyDetails? {
        val db = firestoreService.db ?: return null
        if (buddyUid.isEmpty()) return null

        try {
            val userDoc = db.collection("users").document(buddyUid).get().await()
            if (!userDoc.exists()) return null

            val rawName = userDoc.getString("displayName") ?: ""
            val displayName = if (rawName.isBlank()) {
                val email = userDoc.getString("email") ?: ""
                if (email.isNotBlank()) email.substringBefore("@") else "Focus Buddy"
            } else rawName

            val xp = userDoc.getLong("xp")?.toInt() ?: 0
            val level = userDoc.getLong("level")?.toInt() ?: 1
            val currentStreak = userDoc.getLong("currentStreak")?.toInt() ?: 0

            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val docId = "${buddyUid}_$dateStr"

            val analyticsDoc = db.collection("analytics").document(docId).get().await()
            var screenTimeMs = 0L
            val topAppsList = mutableListOf<BuddyAppUsage>()

            if (analyticsDoc.exists()) {
                screenTimeMs = analyticsDoc.getLong("screenTimeMs") ?: 0L
                val rawApps = analyticsDoc.get("topApps") as? List<Map<String, Any>>
                if (rawApps != null) {
                    for (appMap in rawApps) {
                        val pkg = appMap["packageName"] as? String ?: ""
                        val name = appMap["appName"] as? String ?: pkg
                        val timeMs = (appMap["totalTimeInForegroundMs"] as? Number)?.toLong() ?: 0L
                        
                        val classification = if (pkg.contains("learn", ignoreCase = true) ||
                            pkg.contains("study", ignoreCase = true) ||
                            pkg.contains("books", ignoreCase = true) ||
                            pkg.contains("office", ignoreCase = true) ||
                            pkg.contains("docs", ignoreCase = true) ||
                            pkg.contains("code", ignoreCase = true) ||
                            pkg.contains("edu", ignoreCase = true)
                        ) "PRODUCTIVE" else "DISTRACTING"

                        topAppsList.add(
                            BuddyAppUsage(
                                packageName = pkg,
                                appName = name,
                                totalTimeMs = timeMs,
                                classification = classification
                            )
                        )
                    }
                }
            }

            return BuddyDetails(
                uid = buddyUid,
                displayName = displayName,
                level = level,
                xp = xp,
                currentStreak = currentStreak,
                todayScreenTimeMs = screenTimeMs,
                topAppsToday = topAppsList.sortedByDescending { it.totalTimeMs }
            )
        } catch (e: Exception) {
            Log.e(tag, "Error loading buddy details for $buddyUid: ${e.message}", e)
            return null
        }
    }
}
