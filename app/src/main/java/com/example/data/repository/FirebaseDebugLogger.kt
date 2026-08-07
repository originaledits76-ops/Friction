package com.example.data.repository

import android.util.Log

object FirebaseDebugLogger {
    private const val TAG = "FirebaseDebug"

    /**
     * Logs detailed debugging information when a Firestore write operation fails.
     */
    fun logWriteFailure(
        collection: String,
        documentId: String,
        exception: Exception,
        reason: String
    ) {
        val msg = exception.message ?: ""
        if (msg.contains("PERMISSION_DENIED") || msg.contains("Missing or insufficient permissions")) {
            Log.i(TAG, "Cloud sync skipped for $collection/$documentId (Offline mode active or permission restricted)")
            return
        }
        val stackTraceStr = Log.getStackTraceString(exception)
        Log.e(TAG, "================ FIREBASE WRITE FAILURE ===============")
        Log.e(TAG, "Collection:  $collection")
        Log.e(TAG, "Document ID: $documentId")
        Log.e(TAG, "Reason:      $reason")
        Log.e(TAG, "Exception:   ${exception.javaClass.simpleName}: $msg")
        Log.e(TAG, "Stack Trace:\n$stackTraceStr")
        Log.e(TAG, "=======================================================")
    }

    /**
     * Logs successful Firestore writes for auditing and development verification.
     */
    fun logWriteSuccess(collection: String, documentId: String) {
        Log.d(TAG, "[Firestore Write Success] Saved to collection '$collection', document '$documentId'")
    }
}
