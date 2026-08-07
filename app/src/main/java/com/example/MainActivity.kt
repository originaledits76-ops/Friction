package com.example

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

data class BlockConfig(val packageName: String, val ruleId: String, val ruleName: String)

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
                        }
                    )
                } else {
                    val currentLoginViewModel = loginViewModel
                    val currentHomeViewModel = homeViewModel
                    
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
                                    rule = currentHomeViewModel.rules.collectAsState().value.find { it.id == config.ruleId } ?: FrictionRule(
                                        id = config.ruleId,
                                        name = config.ruleName,
                                        targetAppPackage = config.packageName
                                    ),
                                    onComplete = { xp, coins ->
                                        // Grant bypass permissions to the target app package
                                        FrictionAccessibilityService.unlockAppTemporarily(config.packageName, 15)
                                        currentHomeViewModel.completeChallenge(config.ruleName, "TASK", xp, coins, 20)
                                        activeBlockedAppConfig = null
                                    },
                                    onSkip = { xpPenalty ->
                                        // Bypass target app anyway but apply active penalty
                                        FrictionAccessibilityService.unlockAppTemporarily(config.packageName, 15)
                                        currentHomeViewModel.skipChallenge(config.ruleName, "TASK_SKIP", xpPenalty)
                                        activeBlockedAppConfig = null
                                    },
                                    onCancel = {
                                        activeBlockedAppConfig = null
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
            // 1. Initialize Firebase App safely without letting it crash the whole app boot
            try {
                FirebaseApp.initializeApp(applicationContext)
                Log.d("MainActivity", "FirebaseApp initialized successfully.")
            } catch (e: Exception) {
                Log.e("MainActivity", "FirebaseApp.initializeApp failed: ${e.message}", e)
            }
            
            // 2. Initialize Core Repositories and ViewModels safely (always works offline)
            authRepository = AuthRepository(applicationContext)
            loginViewModel = LoginViewModel(authRepository!!)

            val database = FrictionDatabase.getDatabase(applicationContext)
            val screenTimeService = ScreenTimeService(applicationContext)
            frictionRepository = FrictionRepository(applicationContext, database.frictionDao(), screenTimeService)
            homeViewModel = HomeViewModel(frictionRepository!!, applicationContext)
            
            // Reset error if local initialization is successful (ignore Firebase initialization warnings)
            firebaseInitError = null
            Log.d("MainActivity", "Core services initialized successfully.")
        } catch (e: Exception) {
            Log.e("MainActivity", "Core services initialization failed", e)
            firebaseInitError = e.localizedMessage ?: "Unable to establish secure offline/local database connection."
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
        if (!blockPackage.isNullOrEmpty() && !blockRuleId.isNullOrEmpty()) {
            activeBlockedAppConfig = BlockConfig(
                packageName = blockPackage,
                ruleId = blockRuleId,
                ruleName = blockRuleName ?: "App Limit"
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
            
            Text(
                text = "Secure Sync Offline",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
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
                    Text(
                        text = "TECHNICAL DETAILS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = FrictionError,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
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
                Text(
                    text = "Retry Sync Connection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
