package com.example.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.tasks.await

class FirestoreService {
    private val tag = "FirestoreService"

    val db: FirebaseFirestore
        get() = SafeFirebase.firestore ?: throw IllegalStateException("Firebase Firestore is not available")

    init {
        try {
            val firestoreInstance = SafeFirebase.firestore
            if (firestoreInstance != null) {
                // Enable offline persistence for Firestore so offline mode works elegantly out-of-the-box!
                val settings = FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
                firestoreInstance.firestoreSettings = settings
                Log.d(tag, "Firestore offline persistence enabled successfully.")
            } else {
                Log.w(tag, "Firestore is not available (SafeFirebase.firestore is null).")
            }
        } catch (e: Exception) {
            Log.w(tag, "Could not set Firestore settings: ${e.message}")
        }
    }

    suspend fun <T> runSafe(action: suspend () -> T): T? {
        return try {
            action()
        } catch (e: Exception) {
            Log.e(tag, "Firestore operation failed: ${e.message}", e)
            null
        }
    }
}
