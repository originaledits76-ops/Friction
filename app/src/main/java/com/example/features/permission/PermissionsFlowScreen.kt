package com.example.features.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.core.widgets.FrictionButton
import com.example.features.home.HomeViewModel
import com.example.features.settings.isAccessibilityServiceEnabled
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsFlowScreen(
    homeViewModel: HomeViewModel,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var step by remember { mutableStateOf(1) }
    
    fun checkNotificationsGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun checkCameraGranted(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    fun checkActivityGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    val isUsageGranted by homeViewModel.isPermissionGranted.collectAsState()
    var isAccessibilityGranted by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var isOverlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var isNotificationsGranted by remember { mutableStateOf(checkNotificationsGranted()) }
    var isChallengesGranted by remember { mutableStateOf(checkCameraGranted() && checkActivityGranted()) }

    fun refreshAllPermissions() {
        homeViewModel.checkPermission()
        isAccessibilityGranted = isAccessibilityServiceEnabled(context)
        isOverlayGranted = Settings.canDrawOverlays(context)
        isNotificationsGranted = checkNotificationsGranted()
        isChallengesGranted = checkCameraGranted() && checkActivityGranted()
    }

    // Refresh permission statuses when app comes back to foreground
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshAllPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showSettingsFallback by remember { mutableStateOf(false) }

    // Multi-permission launcher for Camera & Activity Recognition (Step 5)
    val challengePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        isChallengesGranted = checkCameraGranted() && checkActivityGranted()
        if (isChallengesGranted) {
            onComplete()
        } else {
            // Even if partially granted, let them proceed
            onComplete()
        }
    }

    val requestNotificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        isNotificationsGranted = isGranted
        if (isGranted) {
            showSettingsFallback = false
            step = 5
        } else {
            showSettingsFallback = true
        }
    }

    fun openNotificationSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } else {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (err: Exception) {
                // Ignore
            }
        }
    }

    LaunchedEffect(isUsageGranted, isAccessibilityGranted, isOverlayGranted, isNotificationsGranted, isChallengesGranted, step) {
        if (isUsageGranted && isAccessibilityGranted && isOverlayGranted && isNotificationsGranted && isChallengesGranted) {
            onComplete()
        } else if (isUsageGranted && step == 1) {
            step = 2
        } else if (isAccessibilityGranted && step == 2) {
            step = 3
        } else if (isOverlayGranted && step == 3) {
            step = 4
        } else if (isNotificationsGranted && step == 4) {
            step = 5
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Step counter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Permission $step of 5",
                    style = MaterialTheme.typography.labelLarge,
                    color = FrictionPrimary,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { onComplete() }) {
                    Text("Skip for now", color = TextMuted)
                }
            }

            LinearProgressIndicator(
                progress = { step / 5f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = FrictionPrimary,
                trackColor = DarkSurface,
            )

            Spacer(modifier = Modifier.height(24.dp))

            when (step) {
                1 -> {
                    PermissionStep(
                        icon = Icons.Default.DataUsage,
                        title = "Usage Access",
                        whyText = "Friction needs to know when you open distracting apps and how long you use them.",
                        howText = "Tap below, find Friction, and toggle 'Permit usage access'.",
                        benefitText = "Automatically scans your app usage to build highly customized block rules.",
                        privacyText = "Stays locally on your device. We never sync your app usage logs to any external server.",
                        buttonText = "Grant Usage Access",
                        onClick = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                })
                            } catch (e: Exception) {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                })
                            }
                        },
                        onCheck = { 
                            homeViewModel.checkPermission()
                            step = 2
                        }
                    )
                }
                2 -> {
                    PermissionStep(
                        icon = Icons.Default.Block,
                        title = "Accessibility Service",
                        whyText = "To physically block you from endless scrolling, Friction must detect when you attempt to launch restricted apps.",
                        howText = "Tap below, scroll to downloaded services, select 'Friction Blocker', and turn it on.",
                        benefitText = "Ensures instant detection and smooth challenge transition overlays.",
                        privacyText = "Analyzes active app package names in real-time. No keyboard keystrokes or screen text is monitored.",
                        buttonText = "Enable Accessibility",
                        onClick = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                })
                            } catch (e: Exception) {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                })
                            }
                        },
                        onCheck = {
                            isAccessibilityGranted = isAccessibilityServiceEnabled(context)
                            step = 3
                        }
                    )
                }
                3 -> {
                    PermissionStep(
                        icon = Icons.Default.Layers,
                        title = "Display Over Other Apps",
                        whyText = "Necessary for overlaying our mindful challenge screen directly on top of blocked apps.",
                        howText = "Tap below, find Friction, and enable 'Allow display over other apps'.",
                        benefitText = "Creates the premium, uninterrupted blocking window that stops doomscrolling instantly.",
                        privacyText = "Does not record or stream your visual content; strictly for drawing the challenge UI.",
                        buttonText = "Grant Overlay Permission",
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (err: Exception) {
                                    context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    })
                                }
                            }
                        },
                        onCheck = {
                            isOverlayGranted = Settings.canDrawOverlays(context)
                            step = 4
                        }
                    )
                }
                4 -> {
                    PermissionStep(
                        icon = Icons.Default.NotificationsActive,
                        title = "Notifications",
                        whyText = "Friction sends real-time warnings when you are about to exceed a limit.",
                        howText = "Tap below to request system permission or open notification settings.",
                        benefitText = "Enables real-time reminders, custom alerts, and streak celebration feedback.",
                        privacyText = "Kept completely private. We never collect or track your notification payload.",
                        buttonText = "Allow Notifications",
                        secondaryButtonText = "Open App Notification Settings",
                        showFallbackWarning = showSettingsFallback,
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                    isNotificationsGranted = true
                                    step = 5
                                } else {
                                    requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                openNotificationSettings()
                            }
                        },
                        onSecondaryClick = {
                            openNotificationSettings()
                        },
                        onCheck = {
                            isNotificationsGranted = checkNotificationsGranted()
                            step = 5
                        }
                    )
                }
                5 -> {
                    PermissionStep(
                        icon = Icons.Default.FitnessCenter,
                        title = "Advanced Challenges",
                        whyText = "Camera is required for the 'Find the Object' visual challenge. Physical Activity is required for walking step detector tasks.",
                        howText = "Tap below to grant Camera and Physical Activity permissions.",
                        benefitText = "Unlocks healthy, active mindfulness-breaking challenges.",
                        privacyText = "Visual objects are analyzed on-device only; camera feeds or physical logs are never recorded.",
                        buttonText = "Grant Challenge Permissions",
                        onClick = {
                            val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACTIVITY_RECOGNITION)
                            } else {
                                arrayOf(Manifest.permission.CAMERA)
                            }
                            challengePermissionsLauncher.launch(perms)
                        },
                        onCheck = {
                            isChallengesGranted = checkCameraGranted() && checkActivityGranted()
                            onComplete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionStep(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    whyText: String,
    howText: String,
    benefitText: String,
    privacyText: String,
    buttonText: String,
    secondaryButtonText: String? = null,
    showFallbackWarning: Boolean = false,
    onClick: () -> Unit,
    onSecondaryClick: (() -> Unit)? = null,
    onCheck: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(FrictionPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = FrictionPrimary, modifier = Modifier.size(40.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = title, style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, Color(0x05FFFFFF)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Why we need this", style = MaterialTheme.typography.labelMedium, color = FrictionPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(whyText, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                
                Spacer(modifier = Modifier.height(12.dp))
                Text("How it improves your workflow", style = MaterialTheme.typography.labelMedium, color = FrictionAccent, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(benefitText, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

                Spacer(modifier = Modifier.height(12.dp))
                Text("How your data is handled", style = MaterialTheme.typography.labelMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(privacyText, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

                Spacer(modifier = Modifier.height(12.dp))
                Text("How to enable", style = MaterialTheme.typography.labelMedium, color = TextMuted, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(howText, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }
        
        if (showFallbackWarning) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FrictionError.copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, FrictionError.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = FrictionError,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Permission Blocked",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = FrictionError
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "System notifications have been blocked. Please click 'Open App Notification Settings' to enable them manually, then tap Continue.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(28.dp))
        FrictionButton(text = buttonText, onClick = onClick, modifier = Modifier.fillMaxWidth())

        if (secondaryButtonText != null && onSecondaryClick != null) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onSecondaryClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
            ) {
                Text(secondaryButtonText, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onCheck) {
            Text("I've enabled it / Continue", color = FrictionPrimary, fontWeight = FontWeight.SemiBold)
        }
    }
}
