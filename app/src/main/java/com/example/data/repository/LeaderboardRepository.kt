package com.example.data.repository

import android.util.Log
import com.example.data.model.FriendInfo
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class LeaderboardRepository(private val firestoreService: FirestoreService) {
    private val tag = "LeaderboardRepository"

    fun getLeaderboard(type: String): Flow<List<FriendInfo>> = flow {
        val db = firestoreService.db
        if (db == null) {
            emit(emptyList())
            return@flow
        }

        try {
            // Query users from Firestore, ordered by XP descending
            val querySnapshot = db.collection("users")
                .orderBy("xp", Query.Direction.DESCENDING)
                .limit(100)
                .get().await()

            val rankings = querySnapshot.documents.mapIndexed { index, doc ->
                val rawName = doc.getString("displayName") ?: ""
                val displayName = if (rawName.isBlank()) {
                    val email = doc.getString("email") ?: ""
                    if (email.isNotBlank()) email.substringBefore("@") else "Focus Explorer ${doc.id.takeLast(4)}"
                } else rawName

                FriendInfo(
                    uid = doc.id,
                    displayName = displayName,
                    email = "", // Privacy: Never expose emails in Leaderboard
                    currentStreak = doc.getLong("currentStreak")?.toInt() ?: 0,
                    level = doc.getLong("level")?.toInt() ?: 1,
                    xp = doc.getLong("xp")?.toInt() ?: 0,
                    status = "FRIEND"
                )
            }

            emit(rankings)
        } catch (e: Exception) {
            Log.w(tag, "Firestore leaderboard query empty or restricted: ${e.message}")
            // Return empty list if no real data is available. NO mock data allowed.
            emit(emptyList())
        }
    }
}
