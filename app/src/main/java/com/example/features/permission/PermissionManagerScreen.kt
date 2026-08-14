package com.example.features.permission

import com.example.core.widgets.ResponsiveText
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.core.widgets.GlassCard
import com.example.core.widgets.PremiumBackground
import com.example.core.widgets.BackgroundStyle
import com.example.features.home.HomeViewModel
import com.example.features.settings.isAccessibilityServiceEnabled
import com.example.ui.theme.*

@Composable
fun PermissionManagerScreen(
    homeViewModel: HomeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

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

    fun checkBatteryOptGranted(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return pm?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    fun checkUsageAccessGranted(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    var isUsageGranted by remember { mutableStateOf(checkUsageAccessGranted()) }
    var isAccessibilityGranted by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var isOverlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var isActivityGranted by remember { mutableStateOf(checkActivityGranted()) }
    var isNotificationsGranted by remember { mutableStateOf(checkNotificationsGranted()) }
    var isBatteryGranted by remember { mutableStateOf(checkBatteryOptGranted()) }

    fun refreshAll() {
        homeViewModel.checkPermission()
        isUsageGranted = checkUsageAccessGranted()
        isAccessibilityGranted = isAccessibilityServiceEnabled(context)
        isOverlayGranted = Settings.canDrawOverlays(context)
        isActivityGranted = checkActivityGranted()
        isNotificationsGranted = checkNotificationsGranted()
        isBatteryGranted = checkBatteryOptGranted()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshAll()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val singlePermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        refreshAll()
    }

    val totalGranted = listOf(
        isUsageGranted,
        isAccessibilityGranted,
        isOverlayGranted,
        isActivityGranted,
        isNotificationsGranted,
        isBatteryGranted
    ).count { it }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        PremiumBackground(style = BackgroundStyle.SETTINGS)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    ResponsiveText(
                        text = "Permission Manager",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    ResponsiveText(
                        text = "System permissions for friction blocking & AI challenges",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Overview Glass Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                backgroundColor = FrictionPrimary.copy(alpha = 0.08f),
                borderColor = FrictionPrimary.copy(alpha = 0.35f)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = FrictionPrimary.copy(alpha = 0.2f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = FrictionPrimary,
                                    modifier = Modifier.padding(10.dp).size(22.dp)
                                )
                            }
                            Column {
                                ResponsiveText(
                                    text = "Permission Health",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                ResponsiveText(
                                    text = if (totalGranted == 7) "Optimal protection & AI verification" else "Action recommended for full protection",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (totalGranted == 7) FrictionPrimary.copy(alpha = 0.2f) else FrictionAccent.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, if (totalGranted == 7) FrictionPrimary.copy(alpha = 0.4f) else FrictionAccent.copy(alpha = 0.4f))
                        ) {
                            ResponsiveText(
                                text = "$totalGranted / 7 Active",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (totalGranted == 7) FrictionPrimary else FrictionAccent,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { totalGranted / 7f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = FrictionPrimary,
                        trackColor = DarkSurface
                    )
                }
            }

            ResponsiveText(
                text = "System Permissions Breakdown",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )

            // 1. Usage Access
            PermissionCard(
                icon = Icons.Default.DataUsage,
                title = "Usage Access",
                category = "Core Tracking",
                purpose = "Detects app launch events and tracks daily screentime stats locally.",
                isGranted = isUsageGranted,
                onAction = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    } catch (e: Exception) {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }
                }
            )

            // 2. Display Over Other Apps
            PermissionCard(
                icon = Icons.Default.Layers,
                title = "Display Over Other Apps",
                category = "Active Overlay",
                purpose = "Displays the liquid glass challenge barrier directly over restricted apps.",
                isGranted = isOverlayGranted,
                onAction = {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                            data = Uri.parse("package:${context.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }
                }
            )

            // 3. Accessibility Service
            PermissionCard(
                icon = Icons.Default.Block,
                title = "Accessibility Blocker",
                category = "Instant Enforcement",
                purpose = "Provides instant window interception to stop doomscrolling before it starts.",
                isGranted = isAccessibilityGranted,
                onAction = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    } catch (e: Exception) {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }
                }
            )

            // 5. Activity Recognition
            PermissionCard(
                icon = Icons.Default.FitnessCenter,
                title = "Activity Recognition",
                category = "Step Detection",
                purpose = "Accurately detects physical movement during the Walk 100m mindfulness challenge.",
                isGranted = isActivityGranted,
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        singlePermLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                    }
                }
            )

            // 6. Notifications
            PermissionCard(
                icon = Icons.Default.NotificationsActive,
                title = "Notifications",
                category = "Alerts & Reminders",
                purpose = "Sends real-time limit threshold warnings and streak celebrations.",
                isGranted = isNotificationsGranted,
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        singlePermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        try {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            })
                        }
                    }
                }
            )

            // 7. Battery Optimization
            PermissionCard(
                icon = Icons.Default.BatteryChargingFull,
                title = "Unrestricted Battery",
                category = "Service Reliability",
                purpose = "Prevents Android OS from killing background friction enforcement processes.",
                isGranted = isBatteryGranted,
                onAction = {
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
                            context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            })
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    category: String,
    purpose: String,
    isGranted: Boolean,
    onAction: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = DarkCardBg.copy(alpha = 0.85f),
        borderColor = if (isGranted) Color.White.copy(alpha = 0.12f) else FrictionError.copy(alpha = 0.35f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isGranted) FrictionPrimary.copy(alpha = 0.15f) else FrictionError.copy(alpha = 0.15f)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isGranted) FrictionPrimary else FrictionError,
                            modifier = Modifier.padding(10.dp).size(22.dp)
                        )
                    }

                    Column {
                        ResponsiveText(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        ResponsiveText(
                            text = category,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isGranted) FrictionPrimary.copy(alpha = 0.15f) else FrictionError.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, if (isGranted) FrictionPrimary.copy(alpha = 0.3f) else FrictionError.copy(alpha = 0.3f))
                ) {
                    ResponsiveText(
                        text = if (isGranted) "Granted ✓" else "Denied ✕",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isGranted) FrictionPrimary else FrictionError,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            ResponsiveText(
                text = purpose,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            if (!isGranted) {
                Spacer(modifier = Modifier.height(2.dp))
                FrictionButton(
                    text = "Grant Permission",
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                )
            } else {
                TextButton(
                    onClick = onAction,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    ResponsiveText("System Settings", color = TextMuted, fontSize = 12.sp)
                }
            }
        }
    }
}
