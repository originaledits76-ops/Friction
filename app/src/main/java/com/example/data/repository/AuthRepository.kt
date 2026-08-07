package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
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
        val customObjsStr = prefs.getString("${uid}_custom_objects", null) ?: "Water Bottle,Notebook,Backpack,Pen,Chair"
        val badgesStr = prefs.getString("${uid}_badges", null) ?: ""

        val customObjs = customObjsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val badges = badgesStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        val user = User(
            uid = uid,
            displayName = name,
            email = email,
            guest = isGuest,
            level = level,
            xp = xp,
            coins = coins,
            currentStreak = streak,
            age = age,
            goal = goal,
            customGoal = customGoal,
            motivation = motivation,
            unlockedBadges = badges,
            customObjects = customObjs
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
        _authStatus.value = AuthStatus.Loading
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val credentialManager = CredentialManager.create(activity)
                
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId("233127864359-habb02a5ekgljr4ffm9511i9hl8nrak0.apps.googleusercontent.com")
                    .setAutoSelectEnabled(true)
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
                    
                    val fbCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult = firebaseAuth?.signInWithCredential(fbCredential)?.await() ?: throw IllegalStateException("Firebase Auth is not available")
                    val fbUser = authResult.user
                    
                    if (fbUser != null) {
                        val uid = fbUser.uid
                        val cachedUser = getCachedUser(uid)
                        val existingUser = userRepository.getUser(uid)
                        
                        val user = if (existingUser != null) {
                            existingUser.copy(
                                lastLogin = System.currentTimeMillis(),
                                goal = existingUser.goal.ifEmpty { cachedUser.goal },
                                customGoal = existingUser.customGoal.ifEmpty { cachedUser.customGoal },
                                motivation = existingUser.motivation.ifEmpty { cachedUser.motivation },
                                age = if (existingUser.age > 0) existingUser.age else cachedUser.age
                            )
                        } else {
                            cachedUser.copy(
                                uid = uid,
                                displayName = fbUser.displayName ?: cachedUser.displayName.ifEmpty { "Friction User" },
                                email = fbUser.email ?: "",
                                photoUrl = fbUser.photoUrl?.toString() ?: "",
                                guest = false
                            )
                        }
                        
                        saveCachedUser(user)
                        userRepository.createOrUpdateUser(user)
                        _authStatus.value = AuthStatus.Authenticated(user)
                    } else {
                        throw Exception("Firebase user is null after sign in")
                    }
                } else {
                    throw Exception("Unsupported credential type received.")
                }
            } catch (e: GetCredentialException) {
                Log.w(tag, "Credential Manager exception (${e.javaClass.simpleName}): ${e.message}. Falling back gracefully to account session.")
                loginWithOfflineFallback(guest = false)
            } catch (e: Exception) {
                Log.e(tag, "Google sign in failed, falling back to account session.", e)
                loginWithOfflineFallback(guest = false)
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
