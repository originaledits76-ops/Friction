package com.example.data.repository

import android.util.Log

object FirebaseDebugLogger {
    private const val TAG = "FirebaseDebug"

    /**
     * Logs debugging information when a Firestore write operation fails or is skipped.
     */
    fun logWriteFailure(
        collection: String,
        documentId: String,
        exception: Exception,
        reason: String
    ) {
        val msg = exception.message ?: ""
        if (msg.contains("PERMISSION_DENIED") || 
            msg.contains("Missing or insufficient permissions") ||
            msg.contains("not available") ||
            exception is IllegalStateException) {
            Log.i(TAG, "Cloud sync skipped for $collection/$documentId: $reason ($msg)")
            return
        }
        val stackTraceStr = Log.getStackTraceString(exception)
        Log.w(TAG, "Cloud write skipped for collection '$collection', doc '$documentId': $reason ($msg)")
    }

    /**
     * Logs successful Firestore writes for auditing and development verification.
     */
    fun logWriteSuccess(collection: String, documentId: String) {
        Log.d(TAG, "[Firestore Write Success] Saved to collection '$collection', document '$documentId'")
    }
}
