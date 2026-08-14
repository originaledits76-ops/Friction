package com.example.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class FirestoreService {
    private val tag = "FirestoreService"

    val isAvailable: Boolean
        get() = SafeFirebase.isAvailable && SafeFirebase.firestore != null

    val db: FirebaseFirestore?
        get() = SafeFirebase.firestore

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
                Log.i(tag, "Firestore is not available (SafeFirebase.firestore is null). Operating in local offline mode.")
            }
        } catch (e: Exception) {
            Log.i(tag, "Could not set Firestore settings: ${e.message}")
        }
    }

    suspend fun <T> runSafe(action: suspend (FirebaseFirestore) -> T): T? {
        val database = db ?: return null
        return try {
            action(database)
        } catch (e: Exception) {
            Log.i(tag, "Firestore operation skipped: ${e.message}")
            null
        }
    }
}
