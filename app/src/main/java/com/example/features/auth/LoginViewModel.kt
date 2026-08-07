package com.example.features.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    // Expose the AuthStatus flow directly to Compose
    val uiState: StateFlow<AuthStatus> = authRepository.authStatus
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuthStatus.Idle
        )

    fun loginWithGoogle(activity: Activity) {
        authRepository.signInWithGoogle(activity)
    }

    fun loginAsGuest() {
        authRepository.signInAnonymously()
    }
    
    fun logout() {
        authRepository.logout()
    }

    fun updateOnboardingData(name: String, age: Int, goal: String, customGoal: String, motivation: String) {
        viewModelScope.launch {
            authRepository.updateOnboardingData(name, age, goal, customGoal, motivation)
        }
    }
}
