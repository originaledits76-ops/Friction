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
        try {
            // Query users from Firestore, ordered by level and XP descending
            val querySnapshot = firestoreService.db.collection("users")
                .orderBy("level", Query.Direction.DESCENDING)
                .orderBy("xp", Query.Direction.DESCENDING)
                .limit(50)
                .get().await()

            val rankings = querySnapshot.documents.mapIndexed { index, doc ->
                FriendInfo(
                    uid = doc.id,
                    displayName = doc.getString("displayName") ?: "Friction Explorer",
                    email = doc.getString("email") ?: "",
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
