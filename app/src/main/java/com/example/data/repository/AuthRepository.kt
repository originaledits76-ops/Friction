package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.data.model.User
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

sealed class AuthStatus {
    object Idle : AuthStatus()
    object Loading : AuthStatus()
    data class Authenticated(val user: User) : AuthStatus()
    data class Error(val message: String) : AuthStatus()
    object Unauthenticated : AuthStatus()
}

class AuthRepository(private val context: Context) {

    private val tag = "AuthRepository"

    private val firebaseAuth: FirebaseAuth?
        get() = SafeFirebase.auth
    private val firestoreService = FirestoreService()
    private val userRepository = UserRepository(firestoreService)

    private val _authStatus = MutableStateFlow<AuthStatus>(AuthStatus.Idle)
    val authStatus: StateFlow<AuthStatus> = _authStatus.asStateFlow()

    private val prefs = context.getSharedPreferences("friction_prefs", Context.MODE_PRIVATE)

    init {
        checkInitialAuthState()
    }

    fun saveCachedUser(user: User) {
        if (user.uid.isBlank()) return
        prefs.edit().apply {
            putBoolean("demo_logged_in", true)
            putString("active_uid", user.uid)
            putString("${user.uid}_name", user.displayName)
            putString("${user.uid}_email", user.email)
            putInt("${user.uid}_age", user.age)
            putString("${user.uid}_goal", user.goal)
            putString("${user.uid}_custom_goal", user.customGoal)
            putString("${user.uid}_motivation", user.motivation)
            putInt("${user.uid}_level", user.level)
            putInt("${user.uid}_xp", user.xp)
            putInt("${user.uid}_coins", user.coins)
            putInt("${user.uid}_streak", user.currentStreak)
            putBoolean("${user.uid}_is_guest", user.guest)
            putLong("${user.uid}_created_at", user.createdAt)
            putBoolean("${user.uid}_premium", user.premium)
            putLong("${user.uid}_trial_started_at", user.trialStartedAt)
            putLong("${user.uid}_trial_ends_at", user.trialEndsAt)
            putBoolean("${user.uid}_trial_consumed", user.trialConsumed)
            putBoolean("${user.uid}_is_trial_active", user.isTrialActive)
            putString("${user.uid}_premium_plan", user.premiumPlan)
            putString("${user.uid}_sub_status", user.subscriptionStatus)
            putLong("${user.uid}_last_trial_validation", user.lastTrialValidation)
            putString("${user.uid}_custom_objects", user.customObjects.joinToString(","))
            putString("${user.uid}_badges", user.unlockedBadges.joinToString(","))

            // Fallback global keys
            putString("demo_uid", user.uid)
            putString("demo_name", user.displayName)
            putString("demo_email", user.email)
            putInt("demo_age", user.age)
            putString("demo_goal", user.goal)
            putString("demo_custom_goal", user.customGoal)
            putString("demo_motivation", user.motivation)
            putBoolean("demo_is_guest", user.guest)
            putLong("demo_created_at", user.createdAt)
            putBoolean("demo_premium", user.premium)
            apply()
        }
        _authStatus.value = AuthStatus.Authenticated(user)
        Log.d(tag, "[AuthRepository] Local cache updated for UID '${user.uid}': Name='${user.displayName}', Goal='${user.goal}', Age=${user.age}, XP=${user.xp}")
    }

    fun getCachedUser(uid: String): User {
        val name = prefs.getString("${uid}_name", null) ?: prefs.getString("demo_name", "Friction Companion") ?: "Friction Companion"
        val email = prefs.getString("${uid}_email", null) ?: prefs.getString("demo_email", "") ?: ""
        val age = prefs.getInt("${uid}_age", prefs.getInt("demo_age", 0))
        val goal = prefs.getString("${uid}_goal", null) ?: prefs.getString("demo_goal", "") ?: ""
        val customGoal = prefs.getString("${uid}_custom_goal", null) ?: prefs.getString("demo_custom_goal", "") ?: ""
        val motivation = prefs.getString("${uid}_motivation", null) ?: prefs.getString("demo_motivation", "") ?: ""
        val level = prefs.getInt("${uid}_level", 1)
        val xp = prefs.getInt("${uid}_xp", 0)
        val coins = prefs.getInt("${uid}_coins", 0)
        val streak = prefs.getInt("${uid}_streak", 0)
        val isGuest = prefs.getBoolean("${uid}_is_guest", prefs.getBoolean("demo_is_guest", false))
        val createdAt = prefs.getLong("${uid}_created_at", prefs.getLong("demo_created_at", System.currentTimeMillis()))
        val isPremium = prefs.getBoolean("${uid}_premium", prefs.getBoolean("demo_premium", false))
        val trialStartedAt = prefs.getLong("${uid}_trial_started_at", 0L)
        val trialEndsAt = prefs.getLong("${uid}_trial_ends_at", 0L)
        val trialConsumed = prefs.getBoolean("${uid}_trial_consumed", false)
        val isTrialActive = prefs.getBoolean("${uid}_is_trial_active", false)
        val premiumPlan = prefs.getString("${uid}_premium_plan", "NONE") ?: "NONE"
        val subStatus = prefs.getString("${uid}_sub_status", "FREE") ?: "FREE"
        val lastTrialValidation = prefs.getLong("${uid}_last_trial_validation", 0L)
        val customObjsStr = prefs.getString("${uid}_custom_objects", null) ?: "Water Bottle,Notebook,Backpack,Pen,Chair"
        val badgesStr = prefs.getString("${uid}_badges", null) ?: ""

        val customObjs = customObjsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val badges = badgesStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        val user = User(
            uid = uid,
            displayName = name,
            email = email,
            guest = isGuest,
            createdAt = createdAt,
            premium = isPremium,
            level = level,
            xp = xp,
            coins = coins,
            currentStreak = streak,
            age = age,
            goal = goal,
            customGoal = customGoal,
            motivation = motivation,
            unlockedBadges = badges,
            customObjects = customObjs,
            trialStartedAt = trialStartedAt,
            trialEndsAt = trialEndsAt,
            trialConsumed = trialConsumed,
            isTrialActive = isTrialActive,
            premiumPlan = premiumPlan,
            subscriptionStatus = subStatus,
            lastTrialValidation = lastTrialValidation
        )
        Log.d(tag, "[AuthRepository] Loaded cached user for UID '$uid' - Goal: '${user.goal}', Name: '${user.displayName}'")
        return user
    }

    private fun checkInitialAuthState() {
        _authStatus.value = AuthStatus.Loading
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val fbUser = try { firebaseAuth?.currentUser } catch (e: Exception) { null }
                val isDemoLoggedIn = prefs.getBoolean("demo_logged_in", false)
                val savedUid = prefs.getString("active_uid", null) ?: prefs.getString("demo_uid", null)
                val anonUid = prefs.getString("anonymous_uid", null)

                val targetUid = fbUser?.uid ?: savedUid ?: anonUid

                if (targetUid != null) {
                    val cachedUser = getCachedUser(targetUid)
                    val finalUser = if (cachedUser.goal.isEmpty()) {
                        cachedUser.copy(
                            uid = targetUid,
                            displayName = cachedUser.displayName.ifEmpty { if (fbUser?.isAnonymous == true || targetUid.startsWith("anon")) "Guest Companion" else "Friction Member" },
                            guest = fbUser?.isAnonymous ?: targetUid.startsWith("anon") ?: true,
                            goal = "Reduce Screen Time",
                            age = if (cachedUser.age > 0) cachedUser.age else 22
                        )
                    } else {
                        cachedUser
                    }
                    saveCachedUser(finalUser)
                    _authStatus.value = AuthStatus.Authenticated(finalUser)
                    Log.i(tag, "[checkInitialAuthState] Restored active session for UID '$targetUid' (Goal: '${finalUser.goal}')")

                    // Asynchronously attempt remote sync and background Firebase Auth sign-in without blocking UI
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val currentAuthUser = firebaseAuth?.currentUser
                            if (currentAuthUser == null) {
                                try {
                                    val authResult = withTimeoutOrNull(3000L) {
                                        firebaseAuth?.signInAnonymously()?.await()
                                    }
                                    Log.i(tag, "[checkInitialAuthState] Background anonymous sign-in completed. UID: ${authResult?.user?.uid}")
                                } catch (e: Exception) {
                                    Log.w(tag, "[checkInitialAuthState] Background anonymous sign-in skipped: ${e.message}")
                                }
                            }
                            
                            withTimeoutOrNull(2500L) {
                                val activeUid = firebaseAuth?.currentUser?.uid ?: targetUid
                                userRepository.createOrUpdateUser(finalUser.copy(uid = activeUid))
                                val remoteUser = userRepository.getUser(activeUid)
                                if (remoteUser != null) {
                                    val merged = remoteUser.copy(
                                        displayName = remoteUser.displayName.ifEmpty { finalUser.displayName },
                                        age = if (remoteUser.age > 0) remoteUser.age else finalUser.age,
                                        goal = remoteUser.goal.ifEmpty { finalUser.goal },
                                        customGoal = remoteUser.customGoal.ifEmpty { finalUser.customGoal },
                                        motivation = remoteUser.motivation.ifEmpty { finalUser.motivation }
                                    )
                                    saveCachedUser(merged)
                                    _authStatus.value = AuthStatus.Authenticated(merged)
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(tag, "[checkInitialAuthState] Async remote fetch skipped: ${e.message}")
                        }
                    }
                } else {
                    Log.i(tag, "[checkInitialAuthState] No active user session found.")
                    _authStatus.value = AuthStatus.Unauthenticated
                }
            } catch (e: Exception) {
                Log.e(tag, "[checkInitialAuthState] Exception during initial check: ${e.message}", e)
                _authStatus.value = AuthStatus.Unauthenticated
            }
        }
    }

    fun signInAnonymously() {
        _authStatus.value = AuthStatus.Loading
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Persistent anonymous UID
                var anonUid = prefs.getString("anonymous_uid", null)
                if (anonUid == null) {
                    anonUid = "anon_${UUID.randomUUID().toString().take(12)}"
                    prefs.edit().putString("anonymous_uid", anonUid).apply()
                    Log.i(tag, "[signInAnonymously] Created new persistent anonymous UID: $anonUid")
                } else {
                    Log.i(tag, "[signInAnonymously] Reusing existing persistent anonymous UID: $anonUid")
                }

                // Attempt Firebase Anonymous Auth with non-blocking try
                var fbUser: FirebaseUser? = null
                val authInstance = firebaseAuth
                if (authInstance != null) {
                    try {
                        val current = authInstance.currentUser
                        if (current != null && current.isAnonymous) {
                            fbUser = current
                        } else {
                            val authResult = authInstance.signInAnonymously().await()
                            fbUser = authResult.user
                        }
                        Log.i(tag, "[signInAnonymously] FirebaseAuth result. FB UID: ${fbUser?.uid}")
                    } catch (e: Exception) {
                        Log.w(tag, "[signInAnonymously] FirebaseAuth sign-in exception: ${e.message}")
                        throw e
                    }
                }

                if (fbUser == null) {
                    throw Exception("Firebase Auth failed to return a user.")
                }

                val targetUid = fbUser.uid
                prefs.edit().putString("active_uid", targetUid).putBoolean("demo_logged_in", true).apply()

                val cachedUser = getCachedUser(targetUid)
                val finalUser = User(
                    uid = targetUid,
                    displayName = if (cachedUser.displayName.isNotBlank() && cachedUser.displayName != "Friction Companion") cachedUser.displayName else "Guest Companion",
                    email = cachedUser.email,
                    guest = true,
                    createdAt = if (cachedUser.createdAt > 0) cachedUser.createdAt else System.currentTimeMillis(),
                    premium = cachedUser.premium,
                    level = cachedUser.level,
                    xp = cachedUser.xp,
                    coins = cachedUser.coins,
                    currentStreak = cachedUser.currentStreak,
                    age = cachedUser.age,
                    goal = cachedUser.goal,
                    customGoal = cachedUser.customGoal,
                    motivation = cachedUser.motivation,
                    unlockedBadges = cachedUser.unlockedBadges,
                    customObjects = cachedUser.customObjects
                )

                // Save locally so guest session is persistent instantly
                saveCachedUser(finalUser)

                // Authenticate UI IMMEDIATELY
                _authStatus.value = AuthStatus.Authenticated(finalUser)
                Log.i(tag, "[signInAnonymously] Guest authentication complete for UID '$targetUid'")

                // Synchronously sync with Firestore to ensure user is created
                try {
                    userRepository.createOrUpdateUser(finalUser)
                } catch (e: Exception) {
                    Log.w(tag, "[signInAnonymously] Firestore sync failed: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(tag, "[signInAnonymously] Error during login: ${e.message}", e)
                _authStatus.value = AuthStatus.Error("Failed to authenticate anonymously: ${e.localizedMessage}")
            }
        }
    }

    fun signInWithGoogle(activity: Activity) {
        Log.i(tag, "[AuthFlow] Step 1: 'Continue with Google' button pressed")
        _authStatus.value = AuthStatus.Loading
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.i(tag, "[AuthFlow] Step 2: Launching official Google Account Picker on Main thread")
                val credentialManager = CredentialManager.create(activity)
                
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId("233127864359-habb02a5ekgljr4ffm9511i9hl8nrak0.apps.googleusercontent.com")
                    .setAutoSelectEnabled(false) // Force Google Account Picker dialog
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                // Execute getCredential directly on Main Thread so UI dialog displays properly
                val result: GetCredentialResponse = credentialManager.getCredential(activity, request)

                if (result.credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    val email = googleIdTokenCredential.id
                    val displayName = googleIdTokenCredential.displayName ?: ""
                    
                    Log.i(tag, "[AuthFlow] Step 3: Google account selected. Email: '$email', Name: '$displayName'")
                    
                    val fbCredential = GoogleAuthProvider.getCredential(idToken, null)
                    var fbUser: FirebaseUser? = null
                    try {
                        val authInstance = firebaseAuth
                        if (authInstance != null) {
                            val authResult = authInstance.signInWithCredential(fbCredential).await()
                            fbUser = authResult.user
                        }
                    } catch (e: Exception) {
                        Log.w(tag, "[AuthFlow] Firebase Auth exception during sign in with credential: ${e.message}")
                        throw e
                    }
                    
                    if (fbUser == null) {
                        throw Exception("Firebase Auth failed to return a user from Google Credential.")
                    }
                    
                    val uid = fbUser.uid
                    prefs.edit().putString("active_uid", uid).putBoolean("demo_logged_in", true).apply()
                    
                    val existingRemoteUser = withContext(Dispatchers.IO) { userRepository.getUser(uid) }
                    
                    val finalUser = if (existingRemoteUser != null && existingRemoteUser.goal.isNotEmpty()) {
                        Log.i(tag, "[AuthFlow] Returning user detected for UID '$uid'. Restoring existing Firestore profile data.")
                        existingRemoteUser.copy(
                            displayName = fbUser.displayName ?: displayName.ifEmpty { existingRemoteUser.displayName },
                            email = fbUser.email ?: email.ifEmpty { existingRemoteUser.email },
                            photoUrl = fbUser.photoUrl?.toString() ?: googleIdTokenCredential.profilePictureUri?.toString() ?: existingRemoteUser.photoUrl,
                            guest = false
                        )
                    } else {
                        Log.i(tag, "[AuthFlow] New user detected for UID '$uid'. Initializing onboarding state.")
                        val cachedUser = getCachedUser(uid)
                        User(
                            uid = uid,
                            displayName = fbUser.displayName ?: displayName.ifEmpty { cachedUser.displayName.ifEmpty { "Google Member" } },
                            email = fbUser.email ?: email.ifEmpty { cachedUser.email },
                            photoUrl = fbUser.photoUrl?.toString() ?: googleIdTokenCredential.profilePictureUri?.toString() ?: cachedUser.photoUrl,
                            guest = false,
                            goal = cachedUser.goal,
                            age = if (cachedUser.age > 0) cachedUser.age else 0,
                            motivation = cachedUser.motivation
                        )
                    }
                    
                    saveCachedUser(finalUser)
                    _authStatus.value = AuthStatus.Authenticated(finalUser)

                    if (existingRemoteUser == null || existingRemoteUser.goal.isEmpty()) {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                userRepository.createOrUpdateUser(finalUser)
                            } catch (e: Exception) {
                                Log.w(tag, "[AuthFlow] Async Firestore profile save skipped: ${e.message}")
                            }
                        }
                    }
                } else {
                    Log.i(tag, "[AuthFlow] CredentialManager returned unsupported type.")
                    _authStatus.value = AuthStatus.Error("Unsupported credential type.")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.i(tag, "[AuthFlow] Google account selection cancelled by user.")
                _authStatus.value = AuthStatus.Unauthenticated
            } catch (e: Exception) {
                Log.w(tag, "[AuthFlow] Exception during Google Sign-In: ${e.message}.")
                _authStatus.value = AuthStatus.Error("Google Sign-In failed: ${e.localizedMessage}")
            }
        }
    }

    fun linkGoogleAccount(activity: Activity) {
        Log.i(tag, "[LinkAccount] Starting Google account linking process for guest user")
        _authStatus.value = AuthStatus.Loading
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val credentialManager = CredentialManager.create(activity)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId("233127864359-habb02a5ekgljr4ffm9511i9hl8nrak0.apps.googleusercontent.com")
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result: GetCredentialResponse = credentialManager.getCredential(activity, request)

                val credential = result.credential
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    val email = googleIdTokenCredential.id
                    val displayName = googleIdTokenCredential.displayName ?: ""

                    val fbCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val authInstance = firebaseAuth
                    val currentFbUser = authInstance?.currentUser

                    if (currentFbUser != null && currentFbUser.isAnonymous) {
                        try {
                            val linkResult = currentFbUser.linkWithCredential(fbCredential).await()
                            val linkedFbUser = linkResult.user
                            val uid = linkedFbUser?.uid ?: currentFbUser.uid

                            val existingUser = userRepository.getUser(uid) ?: getCachedUser(uid)
                            val updatedUser = existingUser.copy(
                                uid = uid,
                                guest = false,
                                displayName = linkedFbUser?.displayName ?: displayName.ifEmpty { existingUser.displayName.ifEmpty { "Friction Member" } },
                                email = linkedFbUser?.email ?: email.ifEmpty { existingUser.email },
                                photoUrl = linkedFbUser?.photoUrl?.toString() ?: existingUser.photoUrl
                            )
                            saveCachedUser(updatedUser)
                            userRepository.createOrUpdateUser(updatedUser)
                            Log.i(tag, "[LinkAccount] Successfully linked Google credential to anonymous user UID '$uid'")
                            _authStatus.value = AuthStatus.Authenticated(updatedUser)
                        } catch (e: Exception) {
                            Log.w(tag, "[LinkAccount] Link failed: ${e.message}. Signing in directly with Google.")
                            signInWithGoogle(activity)
                        }
                    } else {
                        signInWithGoogle(activity)
                    }
                } else {
                    _authStatus.value = AuthStatus.Error("Received unsupported authentication credential format.")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.i(tag, "[LinkAccount] Account picker cancelled by user.")
                checkInitialAuthState()
            } catch (e: Exception) {
                Log.e(tag, "[LinkAccount] Error during Google account linking: ${e.message}", e)
                _authStatus.value = AuthStatus.Error("Account linking failed: ${e.localizedMessage}")
            }
        }
    }

    fun logout() {
        _authStatus.value = AuthStatus.Loading
        CoroutineScope(Dispatchers.Main).launch {
            try {
                firebaseAuth?.signOut()
            } catch (e: Exception) {
                Log.e(tag, "Failed to sign out from Firebase: ${e.message}")
            }
            
            prefs.edit().apply {
                remove("demo_logged_in")
                remove("active_uid")
                remove("demo_uid")
                remove("demo_name")
                remove("demo_email")
                remove("demo_is_guest")
                apply()
            }
            
            delay(500)
            _authStatus.value = AuthStatus.Unauthenticated
        }
    }

    suspend fun updateOnboardingData(name: String, age: Int, goal: String, customGoal: String, motivation: String) {
        val currentStatus = _authStatus.value
        if (currentStatus is AuthStatus.Authenticated) {
            val currentUser = currentStatus.user
            val updatedUser = currentUser.copy(
                displayName = name,
                age = age,
                goal = goal,
                customGoal = customGoal,
                motivation = motivation
            )

            Log.i(tag, "[updateOnboardingData] Updating onboarding info for UID '${updatedUser.uid}' - Name: '$name', Goal: '$goal', Age: $age")

            // 1. Save locally to cache immediately so user NEVER has to re-enter upon restart
            saveCachedUser(updatedUser)

            // 2. Push to Firestore
            val success = userRepository.createOrUpdateUser(updatedUser)
            Log.d(tag, "[updateOnboardingData] Firestore sync status: $success for UID '${updatedUser.uid}'")

            _authStatus.value = AuthStatus.Authenticated(updatedUser)
        } else {
            Log.e(tag, "[updateOnboardingData] Cannot save onboarding data because user is unauthenticated")
        }
    }

    suspend fun startFreeTrial(user: User): User? {
        if (user.trialConsumed) {
            Log.w(tag, "[startFreeTrial] User '${user.uid}' has already consumed their free trial.")
            return null
        }
        val now = System.currentTimeMillis()
        val threeDaysMs = 3L * 24 * 3600 * 1000
        val updatedUser = user.copy(
            premium = true,
            isTrialActive = true,
            trialStartedAt = now,
            trialEndsAt = now + threeDaysMs,
            trialConsumed = true,
            subscriptionStatus = "ACTIVE",
            premiumPlan = "TRIAL",
            lastTrialValidation = now
        )
        saveCachedUser(updatedUser)
        withContext(Dispatchers.IO) {
            userRepository.createOrUpdateUser(updatedUser)
        }
        _authStatus.value = AuthStatus.Authenticated(updatedUser)
        return updatedUser
    }

    suspend fun redeemCoupon(code: String, user: User): CouponResult {
        val couponRepo = CouponRepository(firestoreService, userRepository)
        return couponRepo.redeemCoupon(code, user, this)
    }
}
