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
            val fbUser = try { firebaseAuth?.currentUser } catch (e: Exception) { null }
            if (fbUser != null) {
                val uid = fbUser.uid
                Log.i(tag, "[checkInitialAuthState] Firebase currentUser found: $uid")
                
                // 1. Get local cached user profile first so goal and onboarding details are available immediately
                val cachedUser = getCachedUser(uid)
                
                // 2. Fetch remote user doc from Firestore asynchronously
                val remoteUser = try {
                    userRepository.getUser(uid)
                } catch (e: Exception) {
                    Log.e(tag, "[checkInitialAuthState] Firestore getUser exception: ${e.message}", e)
                    null
                }

                val finalUser = if (remoteUser != null) {
                    // Merge remote with cached
                    val merged = remoteUser.copy(
                        displayName = remoteUser.displayName.ifEmpty { cachedUser.displayName },
                        age = if (remoteUser.age > 0) remoteUser.age else cachedUser.age,
                        goal = remoteUser.goal.ifEmpty { cachedUser.goal },
                        customGoal = remoteUser.customGoal.ifEmpty { cachedUser.customGoal },
                        motivation = remoteUser.motivation.ifEmpty { cachedUser.motivation },
                        customObjects = if (remoteUser.customObjects.isNotEmpty()) remoteUser.customObjects else cachedUser.customObjects,
                        unlockedBadges = if (remoteUser.unlockedBadges.isNotEmpty()) remoteUser.unlockedBadges else cachedUser.unlockedBadges
                    )
                    saveCachedUser(merged)
                    merged
                } else {
                    val fallback = cachedUser.copy(
                        uid = uid,
                        displayName = fbUser.displayName ?: cachedUser.displayName.ifEmpty { "Friction User" },
                        email = fbUser.email ?: cachedUser.email,
                        photoUrl = fbUser.photoUrl?.toString() ?: "",
                        guest = fbUser.isAnonymous
                    )
                    saveCachedUser(fallback)
                    userRepository.createOrUpdateUser(fallback)
                    fallback
                }

                Log.i(tag, "[checkInitialAuthState] Successfully authenticated UID '$uid' (Goal: '${finalUser.goal}')")
                _authStatus.value = AuthStatus.Authenticated(finalUser)
            } else {
                val isDemoLoggedIn = prefs.getBoolean("demo_logged_in", false)
                val savedUid = prefs.getString("active_uid", null) ?: prefs.getString("demo_uid", null)
                val anonUid = prefs.getString("anonymous_uid", null)

                val targetUid = savedUid ?: anonUid

                if (isDemoLoggedIn && targetUid != null) {
                    val cachedUser = getCachedUser(targetUid)
                    Log.i(tag, "[checkInitialAuthState] Offline/Cached session restored for UID '$targetUid' (Goal: '${cachedUser.goal}')")
                    _authStatus.value = AuthStatus.Authenticated(cachedUser)
                } else if (anonUid != null) {
                    val cachedUser = getCachedUser(anonUid)
                    Log.i(tag, "[checkInitialAuthState] Anonymous UID session restored for '$anonUid' (Goal: '${cachedUser.goal}')")
                    _authStatus.value = AuthStatus.Authenticated(cachedUser)
                } else {
                    Log.i(tag, "[checkInitialAuthState] No active user session found.")
                    _authStatus.value = AuthStatus.Unauthenticated
                }
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

                var fbUser = try { firebaseAuth?.currentUser } catch (e: Exception) { null }
                if (fbUser == null && firebaseAuth != null) {
                    try {
                        val authResult = firebaseAuth!!.signInAnonymously().await()
                        fbUser = authResult.user
                        Log.i(tag, "[signInAnonymously] FirebaseAuth anonymous sign-in succeeded. FB UID: ${fbUser?.uid}")
                    } catch (e: Exception) {
                        Log.w(tag, "[signInAnonymously] FirebaseAuth anonymous sign-in exception: ${e.message}. Using persistent anonymous UID: $anonUid")
                    }
                }

                val targetUid = fbUser?.uid ?: anonUid
                prefs.edit().putString("active_uid", targetUid).apply()

                val cachedUser = getCachedUser(targetUid)
                val existingUser = userRepository.getUser(targetUid)

                val finalUser = existingUser?.let { remote ->
                    val merged = remote.copy(
                        displayName = remote.displayName.ifEmpty { cachedUser.displayName.ifEmpty { "Guest Companion" } },
                        age = if (remote.age > 0) remote.age else cachedUser.age,
                        goal = remote.goal.ifEmpty { cachedUser.goal },
                        customGoal = remote.customGoal.ifEmpty { cachedUser.customGoal },
                        motivation = remote.motivation.ifEmpty { cachedUser.motivation },
                        guest = true
                    )
                    saveCachedUser(merged)
                    merged
                } ?: cachedUser.copy(
                    uid = targetUid,
                    displayName = cachedUser.displayName.ifEmpty { "Guest Companion" },
                    guest = true
                )

                saveCachedUser(finalUser)
                userRepository.createOrUpdateUser(finalUser)

                Log.i(tag, "[signInAnonymously] Anonymous login complete for UID '$targetUid' (Goal: '${finalUser.goal}')")
                _authStatus.value = AuthStatus.Authenticated(finalUser)
            } catch (e: Exception) {
                Log.e(tag, "[signInAnonymously] Error during login: ${e.message}", e)
                loginWithOfflineFallback(guest = true)
            }
        }
    }

    fun signInWithGoogle(activity: Activity) {
        Log.i(tag, "[AuthFlow] Step 1: 'Continue with Google' button pressed")
        _authStatus.value = AuthStatus.Loading
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.i(tag, "[AuthFlow] Step 2: Launching official Google Account Picker")
                val credentialManager = CredentialManager.create(activity)
                
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId("233127864359-habb02a5ekgljr4ffm9511i9hl8nrak0.apps.googleusercontent.com")
                    .setAutoSelectEnabled(false) // Force Google Account Picker dialog
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result: GetCredentialResponse = withContext(Dispatchers.IO) {
                    credentialManager.getCredential(activity, request)
                }

                val credential = result.credential
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    val email = googleIdTokenCredential.id
                    val displayName = googleIdTokenCredential.displayName ?: ""
                    
                    Log.i(tag, "[AuthFlow] Step 3: Google account selected. Email: '$email', Name: '$displayName'")
                    Log.i(tag, "[AuthFlow] Step 4: OAuth ID token received (Token length: ${idToken.length})")
                    
                    Log.i(tag, "[AuthFlow] Step 5: Firebase authentication started")
                    val fbCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val authInstance = firebaseAuth ?: throw IllegalStateException("Firebase Auth service is unavailable")
                    val authResult = authInstance.signInWithCredential(fbCredential).await()
                    val fbUser = authResult.user
                    
                    if (fbUser != null && firebaseAuth?.currentUser != null) {
                        val uid = fbUser.uid
                        Log.i(tag, "[AuthFlow] Step 6: Firebase authentication successful. FirebaseAuth currentUser UID: '$uid'")
                        
                        Log.i(tag, "[AuthFlow] Step 7: Firestore profile lookup for UID '$uid'")
                        val existingUser = userRepository.getUser(uid)
                        val cachedUser = getCachedUser(uid)
                        
                        val finalUser = if (existingUser != null && existingUser.goal.isNotEmpty()) {
                            Log.i(tag, "[AuthFlow] Step 8: Firestore profile document EXISTS for UID '$uid'. Goal: '${existingUser.goal}'")
                            val updated = existingUser.copy(
                                lastLogin = System.currentTimeMillis(),
                                displayName = fbUser.displayName ?: existingUser.displayName.ifEmpty { displayName },
                                email = fbUser.email ?: existingUser.email,
                                photoUrl = fbUser.photoUrl?.toString() ?: existingUser.photoUrl,
                                guest = false
                            )
                            saveCachedUser(updated)
                            userRepository.createOrUpdateUser(updated)
                            updated
                        } else {
                            Log.i(tag, "[AuthFlow] Step 8: Firestore profile document NEW or INCOMPLETE for UID '$uid'")
                            val newUser = User(
                                uid = uid,
                                displayName = fbUser.displayName ?: displayName.ifEmpty { "Friction Member" },
                                email = fbUser.email ?: email,
                                photoUrl = fbUser.photoUrl?.toString() ?: "",
                                guest = false,
                                goal = existingUser?.goal ?: cachedUser.goal, // empty if fresh user
                                age = if ((existingUser?.age ?: 0) > 0) existingUser!!.age else cachedUser.age,
                                motivation = existingUser?.motivation ?: cachedUser.motivation
                            )
                            saveCachedUser(newUser)
                            userRepository.createOrUpdateUser(newUser)
                            newUser
                        }
                        
                        if (finalUser.goal.isEmpty()) {
                            Log.i(tag, "[AuthFlow] Step 9: Navigation decision -> First-time user detected. Directing to User Introduction page.")
                        } else {
                            Log.i(tag, "[AuthFlow] Step 9: Navigation decision -> Returning user detected (Goal: '${finalUser.goal}'). Directing directly to Dashboard.")
                        }
                        _authStatus.value = AuthStatus.Authenticated(finalUser)
                    } else {
                        Log.e(tag, "[AuthFlow] Firebase user is null after sign in")
                        _authStatus.value = AuthStatus.Error("Firebase authentication failed. Could not verify user credentials.")
                    }
                } else {
                    Log.e(tag, "[AuthFlow] Unsupported credential type received: ${credential.type}")
                    _authStatus.value = AuthStatus.Error("Received unsupported authentication credential format.")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.i(tag, "[AuthFlow] Google account selection cancelled by user. Remaining on Login screen.")
                _authStatus.value = AuthStatus.Unauthenticated
            } catch (e: GetCredentialException) {
                Log.w(tag, "[AuthFlow] Credential Manager Exception (${e.javaClass.simpleName}): ${e.message}")
                val isCancelled = e.message?.contains("cancel", ignoreCase = true) == true
                if (isCancelled) {
                    Log.i(tag, "[AuthFlow] Sign-in flow cancelled by user.")
                    _authStatus.value = AuthStatus.Unauthenticated
                } else {
                    val userMsg = when {
                        e.message?.contains("No credentials available", ignoreCase = true) == true ->
                            "No Google accounts found on this device or Google Play Services requires updating. Please ensure a Google account is added in device settings."
                        else -> "Google Sign-In failed: ${e.message ?: "Unable to complete Google Sign-In."}"
                    }
                    _authStatus.value = AuthStatus.Error(userMsg)
                }
            } catch (e: Exception) {
                Log.e(tag, "[AuthFlow] Exception during Google Sign-In: ${e.message}", e)
                val msg = if (e.message?.contains("network", ignoreCase = true) == true) {
                    "Network error during authentication. Please check your internet connection."
                } else {
                    "Authentication error: ${e.message ?: "Sign-in process failed."}"
                }
                _authStatus.value = AuthStatus.Error(msg)
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

                val result: GetCredentialResponse = withContext(Dispatchers.IO) {
                    credentialManager.getCredential(activity, request)
                }

                val credential = result.credential
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    val email = googleIdTokenCredential.id
                    val displayName = googleIdTokenCredential.displayName ?: ""

                    val fbCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val authInstance = firebaseAuth ?: throw IllegalStateException("Firebase Auth service is unavailable")
                    val currentFbUser = authInstance.currentUser

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
                            Log.w(tag, "[LinkAccount] Link failed (account collision or error): ${e.message}. Signing in directly with Google.")
                            val authResult = authInstance.signInWithCredential(fbCredential).await()
                            val fbUser = authResult.user
                            if (fbUser != null) {
                                val uid = fbUser.uid
                                val existingUser = userRepository.getUser(uid) ?: getCachedUser(uid)
                                val updatedUser = existingUser.copy(
                                    uid = uid,
                                    guest = false,
                                    displayName = fbUser.displayName ?: displayName.ifEmpty { existingUser.displayName },
                                    email = fbUser.email ?: email.ifEmpty { existingUser.email },
                                    photoUrl = fbUser.photoUrl?.toString() ?: existingUser.photoUrl
                                )
                                saveCachedUser(updatedUser)
                                userRepository.createOrUpdateUser(updatedUser)
                                _authStatus.value = AuthStatus.Authenticated(updatedUser)
                            } else {
                                throw Exception("Google account sign-in failed during linking.")
                            }
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
                _authStatus.value = AuthStatus.Error("Failed to link Google account: ${e.message ?: "Unknown error"}")
            }
        }
    }

    private fun loginWithOfflineFallback(guest: Boolean) {
        val anonUid = prefs.getString("anonymous_uid", null) ?: "offline_guest_${(1000..9999).random()}"
        prefs.edit().putString("anonymous_uid", anonUid).apply()
        
        val uid = if (guest) anonUid else "offline_user_alok"
        val cached = getCachedUser(uid)
        
        val offlineUser = cached.copy(
            uid = uid,
            displayName = cached.displayName.ifEmpty { if (guest) "Friction Guest" else "Alok Choubey" },
            email = cached.email.ifEmpty { if (guest) "" else "alokchoubey892@gmail.com" },
            guest = guest
        )

        saveCachedUser(offlineUser)
        _authStatus.value = AuthStatus.Authenticated(offlineUser)
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
}
