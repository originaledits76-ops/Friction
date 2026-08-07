package com.example.data.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.MainActivity
import com.example.data.local.FrictionDatabase
import com.example.data.model.RuleType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FrictionAccessibilityService : AccessibilityService() {

    private val tag = "FrictionAccessibility"
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    companion object {
        // Temporary storage for unlocked apps (package to unlock-until timestamp)
        private val temporarilyUnlockedApps = mutableMapOf<String, Long>()

        fun unlockAppTemporarily(packageName: String, durationMinutes: Int) {
            val expiry = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
            temporarilyUnlockedApps[packageName] = expiry
        }

        fun isAppUnlocked(packageName: String): Boolean {
            val expiry = temporarilyUnlockedApps[packageName] ?: return false
            if (System.currentTimeMillis() < expiry) {
                return true
            }
            temporarilyUnlockedApps.remove(packageName)
            return false
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Check if window state changed (e.g. app launched)
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageNameEvent = event.packageName?.toString() ?: return
            
            // Avoid blocking ourselves
            if (packageNameEvent == this.packageName) return

            serviceScope.launch {
                try {
                    val db = FrictionDatabase.getDatabase(applicationContext)
                    val rules = db.frictionDao().getAllRules().first()
                    
                    // Find an active rule for this app package
                    val matchingRule = rules.find { rule ->
                        rule.active && 
                        rule.type == RuleType.APP_LIMIT && 
                        rule.targetAppPackage == packageNameEvent
                    }

                    if (matchingRule != null) {
                        // Check if the app is temporarily unlocked
                        if (isAppUnlocked(packageNameEvent)) {
                            Log.d(tag, "App $packageNameEvent is temporarily unlocked, bypassing block.")
                            return@launch
                        }

                        Log.d(tag, "Blocking app launch: $packageNameEvent")
                        
                        // 2. Launch Friction's blocker challenge in the foreground immediately on top
                        val intent = Intent(applicationContext, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            putExtra("BLOCK_PACKAGE", packageNameEvent)
                            putExtra("BLOCK_RULE_ID", matchingRule.id)
                            putExtra("BLOCK_RULE_NAME", matchingRule.name)
                        }
                        startActivity(intent)
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error processing accessibility event", e)
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d(tag, "Service Interrupted")
    }
}
