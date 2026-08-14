package com.example.data.repository

import android.util.Log
import com.example.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class UserRepository(private val firestoreService: FirestoreService) {
    private val tag = "UserRepository"

    private fun resolveTargetUid(requestedUid: String): String? {
        val fbUser = SafeFirebase.currentUser
        if (fbUser != null && fbUser.uid.isNotBlank()) {
            return fbUser.uid
        }
        if (requestedUid.isNotBlank()) {
            return requestedUid
        }
        return null
    }

    suspend fun getUser(uid: String): User? {
        val targetUid = resolveTargetUid(uid) ?: return null
        val db = firestoreService.db ?: return null

        return try {
            val doc = db.collection("users").document(targetUid).get().await()
            if (doc.exists()) {
                val badgesList = (doc.get("unlockedBadges") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val customObjsList = (doc.get("customObjects") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

                val user = User(
                    uid = doc.getString("uid") ?: targetUid,
                    displayName = doc.getString("displayName") ?: "",
                    email = doc.getString("email") ?: "",
                    photoUrl = doc.getString("photoUrl") ?: "",
                    guest = doc.getBoolean("guest") ?: false,
                    premium = doc.getBoolean("premium") ?: false,
                    level = doc.getLong("level")?.toInt() ?: 1,
                    xp = doc.getLong("xp")?.toInt() ?: 0,
                    coins = doc.getLong("coins")?.toInt() ?: 0,
                    currentStreak = doc.getLong("currentStreak")?.toInt() ?: 0,
                    friendsCount = doc.getLong("friendsCount")?.toInt() ?: 0,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    lastLogin = doc.getLong("lastLogin") ?: System.currentTimeMillis(),
                    screenTimeToday = doc.getLong("screenTimeToday") ?: 0L,
                    unlocksToday = doc.getLong("unlockCountToday")?.toInt() ?: 0,
                    age = doc.getLong("age")?.toInt() ?: 0,
                    goal = doc.getString("goal") ?: "",
                    customGoal = doc.getString("customGoal") ?: "",
                    motivation = doc.getString("motivation") ?: "",
                    unlockedBadges = badgesList,
                    customObjects = customObjsList,
                    trialStartedAt = doc.getLong("trialStartedAt") ?: 0L,
                    trialEndsAt = doc.getLong("trialEndsAt") ?: 0L,
                    trialConsumed = doc.getBoolean("trialConsumed") ?: false,
                    isTrialActive = doc.getBoolean("isTrialActive") ?: false,
                    premiumPlan = doc.getString("premiumPlan") ?: "NONE",
                    subscriptionStatus = doc.getString("subscriptionStatus") ?: "FREE",
                    lastTrialValidation = doc.getLong("lastTrialValidation") ?: 0L
                )

                val now = System.currentTimeMillis()
                val finalUser = if (user.isTrialActive && now >= user.trialEndsAt && user.trialEndsAt > 0L) {
                    Log.i(tag, "[TrialValidation] 3-Day Trial has expired for UID '$targetUid'. Updating subscription status to EXPIRED.")
                    val expiredUser = user.copy(
                        isTrialActive = false,
                        premium = if (user.premiumPlan == "TRIAL") false else user.premium,
                        subscriptionStatus = "EXPIRED",
                        lastTrialValidation = now
                    )
                    createOrUpdateUser(expiredUser)
                    expiredUser
                } else {
                    user
                }

                Log.d(tag, "[UserRepository] Read profile for UID '$targetUid' - Name: '${finalUser.displayName}', Goal: '${finalUser.goal}', Premium: ${finalUser.premium}, TrialActive: ${finalUser.isTrialActive}")
                finalUser
            } else {
                Log.i(tag, "[UserRepository] Document does not exist in Firestore for UID '$targetUid'")
                null
            }
        } catch (e: Exception) {
            FirebaseDebugLogger.logWriteFailure(
                collection = "users",
                documentId = targetUid,
                exception = e,
                reason = "Firestore user profile read query failed"
            )
            null
        }
    }

    suspend fun createOrUpdateUser(user: User): Boolean {
        val targetUid = resolveTargetUid(user.uid)
        if (targetUid == null) {
            Log.w(tag, "Local user update skipped for unauthenticated user ${user.uid}")
            return false
        }
        val db = firestoreService.db ?: return false

        val updatedUser = user.copy(uid = targetUid)
        val userMap = updatedUser.toMap().toMutableMap().apply {
            put("lastLogin", System.currentTimeMillis())
            put("unlockCountToday", updatedUser.unlocksToday)
        }

        return try {
            db.collection("users").document(targetUid).set(userMap, SetOptions.merge()).await()
            FirebaseDebugLogger.logWriteSuccess("users", targetUid)
            Log.i(tag, "[UserRepository] Successfully updated user document for UID '$targetUid' (Goal: '${updatedUser.goal}', XP: ${updatedUser.xp}, CustomObjects: ${updatedUser.customObjects})")
            true
        } catch (e: Exception) {
            FirebaseDebugLogger.logWriteFailure(
                collection = "users",
                documentId = targetUid,
                exception = e,
                reason = "Failed to create or update user onboarding/profile document in Firestore."
            )
            false
        }
    }

    suspend fun updateStats(uid: String, screenTimeToday: Long, unlockCountToday: Int) {
        val targetUid = resolveTargetUid(uid) ?: return
        val db = firestoreService.db ?: return

        try {
            db.collection("users").document(targetUid).set(
                mapOf(
                    "screenTimeToday" to screenTimeToday,
                    "unlockCountToday" to unlockCountToday
                ),
                SetOptions.merge()
            ).await()
            FirebaseDebugLogger.logWriteSuccess("users", targetUid)
        } catch (e: Exception) {
            FirebaseDebugLogger.logWriteFailure(
                collection = "users",
                documentId = targetUid,
                exception = e,
                reason = "Failed updating stats in user document"
            )
        }
    }

    suspend fun syncProfileProgress(
        uid: String,
        level: Int,
        xp: Int,
        coins: Int,
        streak: Int,
        unlockedBadges: List<String>,
        customObjects: List<String>
    ) {
        val targetUid = resolveTargetUid(uid) ?: return
        val db = firestoreService.db ?: return

        try {
            val docRef = db.collection("users").document(targetUid)
            val docSnap = docRef.get().await()
            val remoteXp = docSnap.getLong("xp")?.toInt() ?: 0
            val finalXp = maxOf(remoteXp, xp)

            docRef.set(
                mapOf(
                    "level" to level,
                    "xp" to finalXp,
                    "coins" to coins,
                    "currentStreak" to streak,
                    "unlockedBadges" to unlockedBadges,
                    "customObjects" to customObjects
                ),
                SetOptions.merge()
            ).await()
            FirebaseDebugLogger.logWriteSuccess("users", targetUid)
            Log.d(tag, "[UserRepository] Profile progress synced to Firestore for UID '$targetUid' - Level: $level, XP: $finalXp (max of local $xp & remote $remoteXp), Coins: $coins")
        } catch (e: Exception) {
            FirebaseDebugLogger.logWriteFailure(
                collection = "users",
                documentId = targetUid,
                exception = e,
                reason = "Failed syncing profile progress to Firestore"
            )
        }
    }
}
