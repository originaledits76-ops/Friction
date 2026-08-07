package com.example.data.repository

import android.util.Log
import com.example.data.local.FrictionDao
import com.example.data.model.ChallengeHistoryEntity
import com.example.features.home.ChallengeHistoryEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class ChallengeRepository(
    private val firestoreService: FirestoreService,
    private val dao: FrictionDao? = null
) {
    private val tag = "ChallengeRepository"

    suspend fun saveChallengeHistory(userUid: String, entry: ChallengeHistoryEntry) {
        val targetUid = if (userUid.isNotBlank()) userUid else (SafeFirebase.currentUser?.uid ?: "")
        
        Log.d(tag, "[ChallengeRepository] Saving challenge history locally & remotely for UID '$targetUid': ${entry.title} (${entry.status})")

        // 1. Save to Room local DB first for instant local persistence
        val entity = ChallengeHistoryEntity(
            id = entry.id,
            userUid = targetUid,
            title = entry.title,
            type = entry.type,
            status = entry.status,
            rewardText = entry.rewardText,
            completionTimeSec = entry.completionTimeSec,
            dateStr = entry.dateStr,
            timestamp = System.currentTimeMillis()
        )
        try {
            dao?.insertChallengeHistory(entity)
            Log.d(tag, "[ChallengeRepository] Local Room insert successful for history ID '${entry.id}'")
        } catch (e: Exception) {
            Log.e(tag, "[ChallengeRepository] Error saving challenge history to Room local DB: ${e.message}", e)
        }

        // 2. Save to Firestore if online
        if (targetUid.isEmpty() || targetUid.startsWith("offline_") || targetUid.startsWith("guest_")) return

        val docId = entry.id
        try {
            val historyMap = mapOf(
                "userUid" to targetUid,
                "historyId" to entry.id,
                "title" to entry.title,
                "type" to entry.type,
                "status" to entry.status,
                "rewardText" to entry.rewardText,
                "completionTimeSec" to entry.completionTimeSec,
                "dateStr" to entry.dateStr,
                "timestamp" to System.currentTimeMillis()
            )

            firestoreService.db.collection("challenge_history").document(docId)
                .set(historyMap, SetOptions.merge()).await()
            FirebaseDebugLogger.logWriteSuccess("challenge_history", docId)
            Log.i(tag, "[ChallengeRepository] Firestore write success for history ID '$docId' (User: $targetUid)")
        } catch (e: Exception) {
            FirebaseDebugLogger.logWriteFailure(
                collection = "challenge_history",
                documentId = docId,
                exception = e,
                reason = "Failed to write challenge history record to Firestore"
            )
        }
    }

    suspend fun fetchAndSyncChallengeHistoryFromCloud(userUid: String): List<ChallengeHistoryEntry> {
        if (userUid.isEmpty() || userUid.startsWith("offline_") || userUid.startsWith("guest_")) return emptyList()

        return try {
            Log.d(tag, "[ChallengeRepository] Querying Firestore for challenge history of UID '$userUid'...")
            val querySnapshot = firestoreService.db.collection("challenge_history")
                .whereEqualTo("userUid", userUid)
                .get().await()

            val remoteEntries = mutableListOf<ChallengeHistoryEntry>()
            val remoteEntities = mutableListOf<ChallengeHistoryEntity>()

            for (doc in querySnapshot.documents) {
                val id = doc.getString("historyId") ?: doc.id
                val title = doc.getString("title") ?: ""
                val type = doc.getString("type") ?: ""
                val status = doc.getString("status") ?: "COMPLETED"
                val rewardText = doc.getString("rewardText") ?: ""
                val completionTimeSec = doc.getLong("completionTimeSec")?.toInt() ?: 0
                val dateStr = doc.getString("dateStr") ?: ""
                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                val entry = ChallengeHistoryEntry(
                    id = id,
                    title = title,
                    type = type,
                    status = status,
                    rewardText = rewardText,
                    completionTimeSec = completionTimeSec,
                    dateStr = dateStr
                )
                remoteEntries.add(entry)

                remoteEntities.add(
                    ChallengeHistoryEntity(
                        id = id,
                        userUid = userUid,
                        title = title,
                        type = type,
                        status = status,
                        rewardText = rewardText,
                        completionTimeSec = completionTimeSec,
                        dateStr = dateStr,
                        timestamp = timestamp
                    )
                )
            }

            if (remoteEntities.isNotEmpty() && dao != null) {
                dao.insertAllChallengeHistory(remoteEntities)
                Log.i(tag, "[ChallengeRepository] Successfully fetched ${remoteEntries.size} challenge history entries from Firestore and synced to local Room DB")
            }
            remoteEntries
        } catch (e: Exception) {
            FirebaseDebugLogger.logWriteFailure(
                collection = "challenge_history",
                documentId = "query_$userUid",
                exception = e,
                reason = "Failed to sync challenge history from Firestore"
            )
            emptyList()
        }
    }
}
