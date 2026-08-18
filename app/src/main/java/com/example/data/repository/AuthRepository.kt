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

    private fun getAppSigningSha1(): String {
        return try {
            val pm = context.packageManager
            val packageName = context.packageName
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pm.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNATURES)
            }
            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            val cert = signatures?.firstOrNull()?.toByteArray()
            if (cert != null) {
                val md = java.security.MessageDigest.getInstance("SHA-1")
                val digest = md.digest(cert)
                digest.joinToString(":") { "%02X".format(it) }
            } else "Unknown"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private fun getWebClientId(): String {
        return try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) {
                val foundId = context.getString(resId)
                if (foundId.isNotBlank()) foundId else "233127864359-habb02a5ekgljr4ffm9511i9hl8nrak0.apps.googleusercontent.com"
            } else {
                "233127864359-habb02a5ekgljr4ffm9511i9hl8nrak0.apps.googleusercontent.com"
            }
        } catch (e: Exception) {
            "233127864359-habb02a5ekgljr4ffm9511i9hl8nrak0.apps.googleusercontent.com"
        }
    }

    private fun checkInitialAuthState() {
        _authStatus.value = AuthStatus.Loading
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val fbUser = try { firebaseAuth?.currentUser } catch (e: Exception) { null }
                val isDemoLoggedIn = prefs.getBoolean("demo_logged_in", false)
                val savedUid = prefs.getString("active_uid", null) ?: prefs.getString("demo_uid", null)

                val targetUid = fbUser?.uid ?: if (isDemoLoggedIn) savedUid else null

                if (targetUid != null) {
                    val cachedUser = getCachedUser(targetUid)
                    _authStatus.value = AuthStatus.Authenticated(cachedUser)
                    Log.i(tag, "[checkInitialAuthState] Restored active session for UID '$targetUid' (Goal: '${cachedUser.goal}')")

                    // Asynchronously attempt remote sync without blocking UI
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val activeUid = firebaseAuth?.currentUser?.uid ?: targetUid
                            val remoteUser = userRepository.getUser(activeUid)
                            if (remoteUser != null) {
                                saveCachedUser(remoteUser)
                                _authStatus.value = AuthStatus.Authenticated(remoteUser)
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
                val authInstance = firebaseAuth
                var fbUser: FirebaseUser? = authInstance?.currentUser

                if (fbUser == null || !fbUser.isAnonymous) {
                    if (authInstance != null) {
                        try {
                            val authResult = authInstance.signInAnonymously().await()
                            fbUser = authResult.user
                        } catch (e: Exception) {
                            Log.w(tag, "[signInAnonymously] Firebase Anonymous Auth unprovisioned/failed (${e.message}). Proceeding with local guest session.")
                        }
                    }
                }

                val targetUid = fbUser?.uid
                    ?: prefs.getString("active_uid", null)
                    ?: prefs.getString("anonymous_uid", null)
                    ?: "anon_${UUID.randomUUID().toString().take(12)}"

                prefs.edit().apply {
                    putString("active_uid", targetUid)
                    putString("anonymous_uid", targetUid)
                    putBoolean("demo_logged_in", true)
                    apply()
                }

                val existingRemoteUser = try {
                    withContext(Dispatchers.IO) { userRepository.getUser(targetUid) }
                } catch (e: Exception) { null }

                val finalUser = if (existingRemoteUser != null) {
                    Log.i(tag, "[signInAnonymously] Existing guest user detected for UID '$targetUid'. Restoring profile.")
                    existingRemoteUser
                } else {
                    val cachedUser = getCachedUser(targetUid)
                    User(
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
                }

                // Save locally so guest session is persistent instantly
                saveCachedUser(finalUser)

                // Authenticate UI IMMEDIATELY
                _authStatus.value = AuthStatus.Authenticated(finalUser)
                Log.i(tag, "[signInAnonymously] Guest authentication complete for UID '$targetUid'")

                if (existingRemoteUser == null) {
                    withContext(Dispatchers.IO) {
                        try {
                            userRepository.createOrUpdateUser(finalUser)
                        } catch (e: Exception) {
                            Log.w(tag, "[signInAnonymously] Async Firestore sync skipped: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "[signInAnonymously] Unhandled exception in guest sign-in: ${e.message}", e)
                val fallbackUid = prefs.getString("anonymous_uid", null) ?: "anon_${UUID.randomUUID().toString().take(12)}"
                prefs.edit().putString("active_uid", fallbackUid).putString("anonymous_uid", fallbackUid).putBoolean("demo_logged_in", true).apply()
                val fallbackUser = getCachedUser(fallbackUid).copy(uid = fallbackUid, guest = true, displayName = "Guest Companion")
                saveCachedUser(fallbackUser)
                _authStatus.value = AuthStatus.Authenticated(fallbackUser)
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
                val webClientId = getWebClientId()
                
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
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
                    val authInstance = firebaseAuth
                    if (authInstance != null) {
                        val authResult = authInstance.signInWithCredential(fbCredential).await()
                        fbUser = authResult.user
                    }
                    
                    if (fbUser == null) {
                        throw Exception("Firebase Auth failed to return a user from Google Credential.")
                    }
                    
                    val uid = fbUser.uid
                    prefs.edit().putString("active_uid", uid).putBoolean("demo_logged_in", true).apply()
                    
                    val existingRemoteUser = withContext(Dispatchers.IO) { userRepository.getUser(uid) }
                    
                    val finalUser = if (existingRemoteUser != null) {
                        Log.i(tag, "[AuthFlow] Returning user detected for UID '$uid'. Restoring existing Firestore profile data.")
                        existingRemoteUser.copy(
                            displayName = fbUser.displayName ?: displayName.ifEmpty { existingRemoteUser.displayName.ifEmpty { "Google Member" } },
                            email = fbUser.email ?: email.ifEmpty { existingRemoteUser.email },
                            photoUrl = fbUser.photoUrl?.toString() ?: googleIdTokenCredential.profilePictureUri?.toString() ?: existingRemoteUser.photoUrl,
                            guest = false
                        )
                    } else {
                        Log.i(tag, "[AuthFlow] New user detected for UID '$uid'. Initializing user state.")
                        User(
                            uid = uid,
                            displayName = fbUser.displayName ?: displayName.ifEmpty { "Google Member" },
                            email = fbUser.email ?: email.ifEmpty { "" },
                            photoUrl = fbUser.photoUrl?.toString() ?: googleIdTokenCredential.profilePictureUri?.toString() ?: "",
                            guest = false
                        )
                    }
                    
                    saveCachedUser(finalUser)
                    _authStatus.value = AuthStatus.Authenticated(finalUser)

                    withContext(Dispatchers.IO) {
                        try {
                            userRepository.createOrUpdateUser(finalUser)
                        } catch (e: Exception) {
                            Log.w(tag, "[AuthFlow] Async Firestore profile save skipped: ${e.message}")
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
                val activeSha1 = getAppSigningSha1()
                Log.e(tag, "[AuthFlow] Exception during Google Sign-In: ${e.message}. Active APK SHA-1: $activeSha1", e)
                
                val errorDetails = if (e.message?.contains("28444") == true || e.message?.contains("10") == true) {
                    "Google Sign-In Error 10 (Developer Console Setup Mismatch).\n\n" +
                    "• Installed APK SHA-1 Fingerprint:\n$activeSha1\n\n" +
                    "• Registered Firebase SHA-1 Fingerprint:\n0D:AC:03:BF:E1:0A:76:C4:18:8D:2F:E8:0E:E4:8C:E3:42:25:AB:74\n\n" +
                    "Action Required: If installing a custom build or running on a physical device, add your APK's SHA-1 ($activeSha1) in Firebase Console under Project Settings -> Android Apps."
                } else {
                    "Google Sign-In failed: ${e.localizedMessage ?: e.message}"
                }
                _authStatus.value = AuthStatus.Error(errorDetails)
            }
        }
    }

    fun linkGoogleAccount(activity: Activity) {
        Log.i(tag, "[LinkAccount] Starting Google account linking process for guest user")
        _authStatus.value = AuthStatus.Loading
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val credentialManager = CredentialManager.create(activity)
                val webClientId = getWebClientId()
                
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
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
                _authStatus.value = AuthStatus.Error("Account linking failed: ${e.localizedMessage ?: e.message}")
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
}
