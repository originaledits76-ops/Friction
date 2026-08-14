package com.example.data.repository

import android.util.Log
import com.example.data.model.AppUsageInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsRepository(private val firestoreService: FirestoreService) {
    private val tag = "AnalyticsRepository"

    suspend fun saveDailyAnalytics(userUid: String, screenTimeMs: Long, topApps: List<AppUsageInfo>) {
        val targetUid = SafeFirebase.currentUser?.uid ?: userUid
        val db = firestoreService.db
        if (db == null || targetUid.isEmpty() || targetUid.startsWith("offline_") || targetUid.startsWith("guest_")) return

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val docId = "${targetUid}_$dateStr"

        try {
            val appListMap = topApps.map {
                mapOf(
                    "packageName" to it.packageName,
                    "appName" to it.appName,
                    "totalTimeInForegroundMs" to it.totalTimeInForegroundMs
                )
            }

            val analyticsMap = mapOf(
                "userUid" to targetUid,
                "dateStr" to dateStr,
                "screenTimeMs" to screenTimeMs,
                "topApps" to appListMap,
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("analytics").document(docId)
                .set(analyticsMap, SetOptions.merge()).await()

            FirebaseDebugLogger.logWriteSuccess("analytics", docId)
        } catch (e: Exception) {
            FirebaseDebugLogger.logWriteFailure(
                collection = "analytics",
                documentId = docId,
                exception = e,
                reason = "Failed to write daily usage analytics to Firestore"
            )
        }
    }
}
