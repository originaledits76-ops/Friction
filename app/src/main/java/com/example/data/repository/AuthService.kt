package com.example.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthService {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun signInAnonymously(): FirebaseUser? {
        val result = auth.signInAnonymously().await()
        return result.user
    }

    fun signOut() {
        auth.signOut()
    }
}
