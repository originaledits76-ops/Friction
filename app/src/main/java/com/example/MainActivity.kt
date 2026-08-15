package com.example

import com.example.core.widgets.ResponsiveText
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.navigation.FrictionNavigationHost
import com.example.data.local.FrictionDatabase
import com.example.data.model.FrictionRule
import com.example.data.repository.AuthRepository
import com.example.data.repository.FrictionRepository
import com.example.data.repository.SafeFirebase
import com.example.data.service.FrictionAccessibilityService
import com.example.data.service.ScreenTimeService
import com.example.features.auth.LoginViewModel
import com.example.features.home.HomeViewModel
import com.example.features.settings.ActiveBlockerScreen
import com.example.data.repository.AuthStatus
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.theme.*
import com.google.firebase.FirebaseApp

data class BlockConfig(
    val packageName: String,
    val ruleId: String,
    val ruleName: String,
    val challengeType: String = "MATH",
    val challengeValue: Int = 10,
    val isExpired: Boolean = false
)

class MainActivity : ComponentActivity() {
    
    private var authRepository: AuthRepository? = null
    private var loginViewModel: LoginViewModel? = null
    private var frictionRepository: FrictionRepository? = null
    private var homeViewModel: HomeViewModel? = null

    private var activeBlockedAppConfig by mutableStateOf<BlockConfig?>(null)
    
    // State to track Firebase initialization failure
    private var firebaseInitError by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-edge drawing for premium immersive look (notch and gesture bar friendly)
        enableEdgeToEdge()
        
        // Attempt Firebase Initialization and set up core components safely
        tryInitServices()
        com.example.features.ads.AdManager.initialize(applicationContext)

        // Check incoming intent for background block triggers
        checkIntentForBlock(intent)

        setContent {
            MyApplicationTheme {
                val error = firebaseInitError
                if (error != null) {
                    // Full-screen Graceful Firebase Error Screen with Retry Button
                    FirebaseErrorScreen(
                        errorMessage = error,
                        onRetry = {
                            tryInitServices()
        com.example.features.ads.AdManager.initialize(applicationContext)
                        }
                    )
                } else {
                    val currentLoginViewModel = loginViewModel ?: authRepository?.let { LoginViewModel(it) } ?: LoginViewModel(AuthRepository(applicationContext))
                    val currentHomeViewModel = homeViewModel ?: frictionRepository?.let { HomeViewModel(it, applicationContext) } ?: HomeViewModel(FrictionRepository(applicationContext, FrictionDatabase.getDatabase(applicationContext).frictionDao(), ScreenTimeService(applicationContext)), applicationContext)
                    
                    if (currentLoginViewModel != null && currentHomeViewModel != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Scaffold(
                                modifier = Modifier.fillMaxSize()
                            ) { innerPadding ->
                                FrictionNavigationHost(
                                    loginViewModel = currentLoginViewModel,
                                    homeViewModel = currentHomeViewModel,
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }

                            // Render Real-time Accessibility Blocker Challenge over everything else if triggered
                            val config = activeBlockedAppConfig
                            val authStatus by currentLoginViewModel.uiState.collectAsState()
                            val user = if (authStatus is AuthStatus.Authenticated) (authStatus as AuthStatus.Authenticated).user else com.example.data.model.User()
                            if (config != null) {
                                ActiveBlockerScreen(
                                    user = user,
                                    rule = (currentHomeViewModel.rules.collectAsState().value.find { 
                                        it.id == config.ruleId || (it.targetAppPackage != null && it.targetAppPackage == config.packageName)
                                    } ?: FrictionRule(
                                        id = config.ruleId,
                                        name = config.ruleName,
                                        targetAppPackage = config.packageName,
                                        targetAppName = config.ruleName,
                                        challengeType = config.challengeType,
                                        challengeValue = config.challengeValue
                                    )).let { matched ->
                                        if (config.challengeType.isNotBlank() && config.challengeType != "MATH" && matched.challengeType == "MATH") {
                                            matched.copy(challengeType = config.challengeType, challengeValue = config.challengeValue)
                                        } else matched
                                    },
                                    isExpiredMode = config.isExpired,
                                    onComplete = { xp, coins, durationMinutes ->
                                        // Grant temporary unlock allowance to the specific target app package
                                        FrictionAccessibilityService.unlockAppTemporarily(applicationContext, config.packageName, durationMinutes)
                                        currentHomeViewModel.completeChallenge(config.ruleName, "TASK", xp, coins, 20)
                                        activeBlockedAppConfig = null

                                        // Launch target blocked app
                                        try {
                                            val launchIntent = packageManager.getLaunchIntentForPackage(config.packageName)
                                            if (launchIntent != null) {
                                                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                startActivity(launchIntent)
                                            }
                                        } catch (e: Exception) {
                                            Log.e("MainActivity", "Failed to launch app ${config.packageName}", e)
                                        }
                                    },
                                    onSkip = { xpPenalty, durationMinutes ->
                                        // Bypass target app anyway but apply active penalty
                                        FrictionAccessibilityService.unlockAppTemporarily(applicationContext, config.packageName, durationMinutes)
                                        currentHomeViewModel.skipChallenge(config.ruleName, "TASK_SKIP", xpPenalty)
                                        activeBlockedAppConfig = null

                                        // Launch target blocked app
                                        try {
                                            val launchIntent = packageManager.getLaunchIntentForPackage(config.packageName)
                                            if (launchIntent != null) {
                                                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                startActivity(launchIntent)
                                            }
                                        } catch (e: Exception) {
                                            Log.e("MainActivity", "Failed to launch app ${config.packageName}", e)
                                        }
                                    },
                                    onCancel = {
                                        activeBlockedAppConfig = null
                                        currentHomeViewModel.onCloseBlockedApp()
                                        // Securely send the user back to the launcher/home screen
                                        try {
                                            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                                addCategory(Intent.CATEGORY_HOME)
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            startActivity(homeIntent)
                                        } catch (e: Exception) {
                                            Log.e("MainActivity", "Failed to navigate home on cancel", e)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun tryInitServices() {
        try {
            SafeFirebase.initIfNecessary(applicationContext)
        } catch (e: Exception) {
            Log.e("MainActivity", "SafeFirebase init warning: ${e.message}")
        }

        try {
            if (authRepository == null) {
                authRepository = AuthRepository(applicationContext)
            }
            if (loginViewModel == null && authRepository != null) {
                loginViewModel = LoginViewModel(authRepository!!)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "AuthRepository/LoginViewModel init warning: ${e.message}")
        }

        try {
            val database = FrictionDatabase.getDatabase(applicationContext)
            val screenTimeService = ScreenTimeService(applicationContext)
            if (frictionRepository == null) {
                frictionRepository = FrictionRepository(applicationContext, database.frictionDao(), screenTimeService)
            }
            if (homeViewModel == null && frictionRepository != null) {
                homeViewModel = HomeViewModel(frictionRepository!!, applicationContext)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "FrictionRepository/HomeViewModel init warning: ${e.message}")
        }

        try {
            com.example.features.ads.AdManager.initialize(applicationContext)
        } catch (e: Exception) {
            Log.e("MainActivity", "AdManager init warning: ${e.message}")
        }

        firebaseInitError = null
        Log.d("MainActivity", "Core services initialized successfully.")
    }

    override fun onResume() {
        super.onResume()
        com.example.features.ads.AdManager.handleAppOpen(applicationContext, this) {
            activeBlockedAppConfig == null
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkIntentForBlock(intent)
    }

    private fun checkIntentForBlock(intent: Intent) {
        val blockPackage = intent.getStringExtra("BLOCK_PACKAGE")
        val blockRuleId = intent.getStringExtra("BLOCK_RULE_ID")
        val blockRuleName = intent.getStringExtra("BLOCK_RULE_NAME")
        val blockChallengeType = intent.getStringExtra("BLOCK_CHALLENGE_TYPE") ?: "MATH"
        val blockChallengeValue = intent.getIntExtra("BLOCK_CHALLENGE_VALUE", 10)
        val isExpired = intent.getBooleanExtra("BLOCK_IS_EXPIRED", false)
        if (!blockPackage.isNullOrEmpty() && !blockRuleId.isNullOrEmpty()) {
            activeBlockedAppConfig = BlockConfig(
                packageName = blockPackage,
                ruleId = blockRuleId,
                ruleName = blockRuleName ?: "App Limit",
                challengeType = blockChallengeType,
                challengeValue = blockChallengeValue,
                isExpired = isExpired
            )
        }
    }
}

@Composable
fun FirebaseErrorScreen(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = "Cloud Off Icon",
                tint = FrictionError,
                modifier = Modifier.size(72.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            ResponsiveText(
                text = "Secure Sync Offline",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ResponsiveText(
                text = "Friction is unable to establish a secure cloud connection to sync your brand assets, buddies, and rules. Please verify your internet connection.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    ResponsiveText(
                        text = "TECHNICAL DETAILS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = FrictionError,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ResponsiveText(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FrictionPrimary,
                    contentColor = Color(0xFF111315)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                ResponsiveText(
                    text = "Retry Sync Connection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
