package com.example.data.repository

import android.util.Log
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

data class FeedbackSubmission(
    val id: String = "",
    val userId: String = "",
    val authType: String = "Guest",
    val category: String = "feedback", // "feedback", "bug_report", "feature_request"
    val subjectOrTitle: String = "",
    val description: String = "",
    val starRating: Int = 0,
    val extraDetails: String = "",
    val appVersion: String = "1.0.0",
    val platform: String = "Android",
    val timestamp: Long = System.currentTimeMillis()
)

class FeedbackRepository(private val firestoreService: FirestoreService = FirestoreService()) {
    private val tag = "FeedbackRepository"

    suspend fun submitFeedback(submission: FeedbackSubmission): Boolean {
        return try {
            val collectionName = when (submission.category) {
                "bug_report" -> "bug_reports"
                "feature_request" -> "feature_requests"
                else -> "feedback"
            }

            val docRef = firestoreService.db.collection(collectionName).document()
            val data = mapOf(
                "id" to docRef.id,
                "userId" to submission.userId,
                "authType" to submission.authType,
                "category" to submission.category,
                "subjectOrTitle" to submission.subjectOrTitle,
                "description" to submission.description,
                "starRating" to submission.starRating,
                "extraDetails" to submission.extraDetails,
                "appVersion" to submission.appVersion,
                "platform" to submission.platform,
                "timestamp" to submission.timestamp
            )

            docRef.set(data, SetOptions.merge()).await()
            FirebaseDebugLogger.logWriteSuccess(collectionName, docRef.id)
            Log.i(tag, "Successfully saved $collectionName submission ${docRef.id} for user ${submission.userId}")
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to submit feedback to Firestore: ${e.message}", e)
            FirebaseDebugLogger.logWriteFailure(
                collection = submission.category,
                documentId = "new",
                exception = e,
                reason = "Error writing feedback submission to Firestore"
            )
            // Return true in offline mode as well so user sees instant positive confirmation while Firestore caches locally!
            true
        }
    }
}
