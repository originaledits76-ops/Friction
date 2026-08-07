package com.example.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class BlockedAppsRepository(private val firestoreService: FirestoreService) {
    private val tag = "BlockedAppsRepository"

    suspend fun saveBlockedApp(userUid: String, packageName: String, ruleId: String) {
        val targetUid = SafeFirebase.currentUser?.uid ?: userUid
        if (targetUid.isEmpty() || targetUid.startsWith("offline_") || targetUid.startsWith("guest_")) return

        val docId = "${targetUid}_$packageName"
        try {
            val blockedMap = mapOf(
                "userUid" to targetUid,
                "packageName" to packageName,
                "ruleId" to ruleId,
                "timestamp" to System.currentTimeMillis()
            )
            firestoreService.db.collection("blocked_apps").document(docId)
                .set(blockedMap, SetOptions.merge()).await()
            FirebaseDebugLogger.logWriteSuccess("blocked_apps", docId)
        } catch (e: Exception) {
            FirebaseDebugLogger.logWriteFailure(
                collection = "blocked_apps",
                documentId = docId,
                exception = e,
                reason = "Failed to register blocked app in Firestore"
            )
        }
    }

    suspend fun removeBlockedApp(userUid: String, packageName: String) {
        val targetUid = SafeFirebase.currentUser?.uid ?: userUid
        if (targetUid.isEmpty() || targetUid.startsWith("offline_") || targetUid.startsWith("guest_")) return

        val docId = "${targetUid}_$packageName"
        try {
            firestoreService.db.collection("blocked_apps").document(docId).delete().await()
            FirebaseDebugLogger.logWriteSuccess("blocked_apps", docId)
        } catch (e: Exception) {
            FirebaseDebugLogger.logWriteFailure(
                collection = "blocked_apps",
                documentId = docId,
                exception = e,
                reason = "Failed to delete blocked app record from Firestore"
            )
        }
    }
}
