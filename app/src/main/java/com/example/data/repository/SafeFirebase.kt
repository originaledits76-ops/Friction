package com.example.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

object SafeFirebase {
    private const val TAG = "SafeFirebase"

    fun initIfNecessary(context: Context) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                try {
                    FirebaseApp.initializeApp(context)
                    Log.i(TAG, "FirebaseApp default init succeeded.")
                } catch (e: Exception) {
                    Log.w(TAG, "Default init failed, using programmatic options: ${e.message}")
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:233127864359:android:a7ac4e2e673d26a2314602")
                        .setGcmSenderId("233127864359")
                        .setProjectId("friction-6dbb0")
                        .setStorageBucket("friction-6dbb0.firebasestorage.app")
                        .setApiKey("AIzaSyDt4vurhrs0z4cq7Jgt3GfZvF2AIk_s4XU")
                        .build()
                    FirebaseApp.initializeApp(context, options)
                    Log.i(TAG, "FirebaseApp initialized with fallback options.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in initIfNecessary: ${e.message}", e)
        }
    }

    val isAvailable: Boolean
        get() = try {
            val apps = FirebaseApp.getApps(com.google.firebase.FirebaseApp.getInstance().applicationContext)
            apps.isNotEmpty()
        } catch (e: Exception) {
            false
        }

    val auth: FirebaseAuth?
        get() = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth.getInstance() failed: ${e.message}")
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
        get() = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseFirestore.getInstance() failed: ${e.message}")
            null
        }
}

