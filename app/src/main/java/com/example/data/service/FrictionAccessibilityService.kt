package com.example.data.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.MainActivity
import com.example.data.local.FrictionDatabase
import com.example.data.model.RuleType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FrictionAccessibilityService : AccessibilityService() {

    private val tag = "FrictionAccessibility"
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var isPolling = false
    private var currentForegroundPackage = ""

    companion object {
        private const val PREFS_NAME = "friction_allowance_prefs"

        fun unlockAppTemporarily(context: Context, packageName: String, durationMinutes: Int) {
            val startTime = System.currentTimeMillis()
            val expiry = startTime + (durationMinutes * 60 * 1000L)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putLong("unlock_$packageName", expiry)
                .putLong("start_$packageName", startTime)
                .putBoolean("had_session_$packageName", true)
                .putBoolean("active_session_$packageName", true)
                .apply()
            Log.d("FrictionAccessibility", "Unlocked $packageName for $durationMinutes mins until $expiry")
        }

        fun getRemainingTimeMs(context: Context, packageName: String): Long {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val expiry = prefs.getLong("unlock_$packageName", 0L)
            val currentTime = System.currentTimeMillis()
            return if (expiry > currentTime) expiry - currentTime else 0L
        }

        fun isAppUnlocked(context: Context, packageName: String): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val expiry = prefs.getLong("unlock_$packageName", 0L)
            val currentTime = System.currentTimeMillis()
            if (expiry > 0L && currentTime < expiry) {
                return true
            }
            if (expiry > 0L) {
                // Expired
                prefs.edit()
                    .remove("unlock_$packageName")
                    .remove("active_session_$packageName")
                    .apply()
            }
            return false
        }

        fun wasAppPreviouslyUnlocked(context: Context, packageName: String): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val hadSession = prefs.getBoolean("had_session_$packageName", false)
            if (hadSession) {
                // Clear session flag after reading so future fresh launches don't immediately show expired
                prefs.edit().remove("had_session_$packageName").apply()
            }
            return hadSession
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        startActiveSessionPolling()
    }

    private fun startActiveSessionPolling() {
        if (isPolling) return
        isPolling = true
        serviceScope.launch {
            while(isPolling) {
                delay(2000L) // Poll every 2 seconds
                if (currentForegroundPackage.isNotEmpty() && currentForegroundPackage != packageName) {
                    checkAndBlockIfExpired(currentForegroundPackage)
                }
            }
        }
    }

    private suspend fun checkAndBlockIfExpired(pkg: String) {
        try {
            val db = FrictionDatabase.getDatabase(applicationContext)
            val rules = db.frictionDao().getAllRules().first()
            
            val matchingRule = rules.find { rule ->
                rule.active && 
                rule.type == RuleType.APP_LIMIT && 
                rule.targetAppPackage == pkg
            }

            if (matchingRule != null) {
                if (isAppUnlocked(applicationContext, pkg)) {
                    // Still unlocked, do nothing
                    return
                }

                val isExpired = wasAppPreviouslyUnlocked(applicationContext, pkg)
                Log.d(tag, "Polling caught expired app launch: $pkg (isExpired=$isExpired)")
                
                val intent = Intent(applicationContext, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("BLOCK_PACKAGE", pkg)
                    putExtra("BLOCK_RULE_ID", matchingRule.id)
                    putExtra("BLOCK_RULE_NAME", matchingRule.name)
                    putExtra("BLOCK_IS_EXPIRED", isExpired)
                }
                startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error during active session polling", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageNameEvent = event.packageName?.toString() ?: return
            currentForegroundPackage = packageNameEvent
            
            if (packageNameEvent == this.packageName) return

            serviceScope.launch {
                checkAndBlockIfExpired(packageNameEvent)
            }
        }
    }

    override fun onInterrupt() {
        Log.d(tag, "Service Interrupted")
    }
}
