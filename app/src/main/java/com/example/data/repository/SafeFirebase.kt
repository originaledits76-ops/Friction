package com.example.data.repository

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

object SafeFirebase {
    private const val TAG = "SafeFirebase"

    val isAvailable: Boolean
        get() = try {
            FirebaseApp.getInstance()
            true
        } catch (e: Exception) {
            false
        }

    val auth: FirebaseAuth?
        get() = if (isAvailable) {
            try {
                FirebaseAuth.getInstance()
            } catch (e: Exception) {
                Log.w(TAG, "FirebaseAuth.getInstance() failed: ${e.message}")
                null
            }
        } else {
            null
        }

    val currentUser: FirebaseUser?
        get() = try {
            auth?.currentUser
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get currentUser: ${e.message}")
            null
        }

    val firestore: FirebaseFirestore?
        get() = if (isAvailable) {
            try {
                FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                Log.w(TAG, "FirebaseFirestore.getInstance() failed: ${e.message}")
                null
            }
        } else {
            null
        }
}
