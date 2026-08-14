package com.example.features.permission

import com.example.core.widgets.ResponsiveText
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
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
    var step by remember { mutableIntStateOf(1) }
    
    fun checkNotificationsGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun checkActivityGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun checkBatteryOptimizationIgnored(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    val isUsageGranted by homeViewModel.isPermissionGranted.collectAsState()
    var isAccessibilityGranted by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var isBatteryIgnored by remember { mutableStateOf(checkBatteryOptimizationIgnored()) }
    var isOverlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var isNotificationsGranted by remember { mutableStateOf(checkNotificationsGranted()) }
    var isActivityGranted by remember { mutableStateOf(checkActivityGranted()) }

    fun refreshAllPermissions() {
        homeViewModel.checkPermission()
        isAccessibilityGranted = isAccessibilityServiceEnabled(context)
        isBatteryIgnored = checkBatteryOptimizationIgnored()
        isOverlayGranted = Settings.canDrawOverlays(context)
        isNotificationsGranted = checkNotificationsGranted()
        isActivityGranted = checkActivityGranted()
    }

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

    val requestActivityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        isActivityGranted = isGranted
        onComplete()
    }

    val requestNotificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        isNotificationsGranted = isGranted
        if (isGranted) {
            showSettingsFallback = false
            step = 6
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

    @SuppressLint("BatteryLife")
    fun requestBatteryOptimization() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (err: Exception) {
                // Ignore
            }
        }
    }

    LaunchedEffect(isUsageGranted, isAccessibilityGranted, isBatteryIgnored, isOverlayGranted, isNotificationsGranted, isActivityGranted, step) {
        if (isUsageGranted && isAccessibilityGranted && isBatteryIgnored && isOverlayGranted && isNotificationsGranted && isActivityGranted) {
            onComplete()
        } else if (isUsageGranted && step == 1) {
            step = 2
        } else if (isAccessibilityGranted && step == 2) {
            step = 3
        } else if (isBatteryIgnored && step == 3) {
            step = 4
        } else if (isOverlayGranted && step == 4) {
            step = 5
        } else if (isNotificationsGranted && step == 5) {
            step = 6
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ResponsiveText(
                    text = "Permission $step of 6",
                    style = MaterialTheme.typography.labelLarge,
                    color = FrictionPrimary,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { onComplete() }) {
                    ResponsiveText(text = "Skip for now", color = TextMuted)
                }
            }

            LinearProgressIndicator(
                progress = { step / 6f },
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
                        privacyText = "Your usage data stays private and is never shared or sold.",
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
                        icon = Icons.Default.BatteryChargingFull,
                        title = "Background Usage",
                        whyText = "Friction needs to run reliably in the background to accurately track screen time and enforce app blocks.",
                        howText = "Tap below and allow Friction to ignore battery optimizations or run in the background.",
                        benefitText = "Ensures app blockers don't randomly fail when your device goes to sleep.",
                        privacyText = "Required solely for process stability. Does not affect tracking privacy.",
                        buttonText = "Allow Background Usage",
                        onClick = {
                            requestBatteryOptimization()
                        },
                        onCheck = {
                            isBatteryIgnored = checkBatteryOptimizationIgnored()
                            step = 4
                        }
                    )
                }
                4 -> {
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
                            step = 5
                        }
                    )
                }
                5 -> {
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
                                    step = 6
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
                            step = 6
                        }
                    )
                }
                6 -> {
                    PermissionStep(
                        icon = Icons.Default.DirectionsWalk,
                        title = "Physical Activity",
                        whyText = "Physical Activity is required for walking step detector tasks.",
                        howText = "Tap below to grant Physical Activity permission.",
                        benefitText = "Unlocks walking challenges to build healthy habits.",
                        privacyText = "Step data is processed locally to complete challenges.",
                        buttonText = "Grant Activity Permission",
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                requestActivityLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                            } else {
                                isActivityGranted = true
                                onComplete()
                            }
                        },
                        onCheck = {
                            isActivityGranted = checkActivityGranted()
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
        ResponsiveText(text = title, style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, Color(0x05FFFFFF)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ResponsiveText(text = "Why we need this", style = MaterialTheme.typography.labelMedium, color = FrictionPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                ResponsiveText(text = whyText, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                
                Spacer(modifier = Modifier.height(12.dp))
                ResponsiveText(text = "How it improves your workflow", style = MaterialTheme.typography.labelMedium, color = FrictionAccent, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                ResponsiveText(text = benefitText, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

                Spacer(modifier = Modifier.height(12.dp))
                ResponsiveText(text = "How your data is handled", style = MaterialTheme.typography.labelMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                ResponsiveText(text = privacyText, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

                Spacer(modifier = Modifier.height(12.dp))
                ResponsiveText(text = "How to enable", style = MaterialTheme.typography.labelMedium, color = TextMuted, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                ResponsiveText(text = howText, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
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
                        ResponsiveText(
                            text = "Permission Blocked",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = FrictionError
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        ResponsiveText(
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
                ResponsiveText(text = secondaryButtonText, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onCheck) {
            ResponsiveText(text = "I've enabled it / Continue", color = FrictionPrimary, fontWeight = FontWeight.SemiBold)
        }
    }
}
