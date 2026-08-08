package com.example.features.dashboard

import android.text.format.DateFormat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.widgets.BackgroundStyle
import com.example.core.widgets.FrictionButton
import com.example.core.widgets.PremiumBackground
import com.example.R
import com.example.data.model.User
import com.example.ui.theme.*
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    user: User,
    todayScreenTimeMs: Long,
    unlocksToday: Int,
    onNavigateToTab: (String) -> Unit,
    onLinkGoogleAccount: () -> Unit = {},
    onStartFreeTrial: () -> Unit = {},
    onMarkOfferSeen: () -> Unit = {},
    onOpenPaywall: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val todayDateStr = DateFormat.getLongDateFormat(context).format(Date())

    val localPrefs = remember { context.getSharedPreferences("friction_local_prefs", android.content.Context.MODE_PRIVATE) }
    var localHasSeenEarlyBird by remember { mutableStateOf(localPrefs.getBoolean("hasSeenEarlyBirdPopup", false)) }

    var showEarlyBirdDialog by remember { mutableStateOf(false) }
    var showTrialExpiredDialog by remember { mutableStateOf(false) }

    // Trigger Early Bird Dialog ONCE if user has not seen it locally or in profile and is not premium
    LaunchedEffect(user.uid, user.hasSeenEarlyBirdOffer, user.premium, localHasSeenEarlyBird) {
        if (!localHasSeenEarlyBird && !user.hasSeenEarlyBirdOffer && !user.premium) {
            kotlinx.coroutines.delay(1200)
            showEarlyBirdDialog = true
        }
    }

    // Trigger Trial Expired Dialog if trial has ended
    LaunchedEffect(user.subscriptionStatus, user.trialEndsAt) {
        if (user.subscriptionStatus == "EXPIRED" || (user.trialConsumed && !user.isTrialActive && user.premiumPlan == "TRIAL")) {
            showTrialExpiredDialog = true
        }
    }

    // Animation progress for circular goal ring
    val goalMaxMs = 3600000L * 4 // 4 Hours limit
    val progressRatio = (todayScreenTimeMs.toFloat() / goalMaxMs).coerceIn(0f, 1f)
    
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(progressRatio) {
        animatedProgress.animateTo(
            targetValue = progressRatio,
            animationSpec = tween(1200, easing = EaseInOutQuad)
        )
    }

    if (showEarlyBirdDialog) {
        // Mark as seen immediately in SharedPreferences and user profile so it appears ONLY ONCE
        LaunchedEffect(Unit) {
            localPrefs.edit().putBoolean("hasSeenEarlyBirdPopup", true).apply()
            localHasSeenEarlyBird = true
            onMarkOfferSeen()
        }

        AlertDialog(
            onDismissRequest = { showEarlyBirdDialog = false },
            confirmButton = {},
            dismissButton = {},
            containerColor = Color.Transparent,
            shape = RoundedCornerShape(28.dp),
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(horizontal = 0.dp),
            text = {
                com.example.core.widgets.GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    backgroundColor = DarkCardBg.copy(alpha = 0.95f),
                    borderColor = FrictionPrimary.copy(alpha = 0.35f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Badge top
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = FrictionPrimary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, FrictionPrimary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stars,
                                    contentDescription = null,
                                    tint = FrictionAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "EARLY BIRD LAUNCH OFFER",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = FrictionAccent,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }

                        // Mascot image
                        Image(
                            painter = painterResource(id = R.drawable.mascot_hi),
                            contentDescription = "Early Bird Mascot",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(90.dp)
                        )

                        Text(
                            text = "Unlock Your Full Focus Potential",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        // Body text with highlighted terms
                        Text(
                            text = "Enjoy Premium free for 3 days and unlock our exclusive launch pricing on Annual and Lifetime plans.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        // Feature Highlights Card
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, Color(0x15FFFFFF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, null, tint = FrictionPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Unlimited App Limits & Barriers", style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, null, tint = FrictionPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("MediaPipe Camera Push-Up Counter", style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, null, tint = FrictionPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Exclusive 25% Launch Discount", style = MaterialTheme.typography.bodySmall, color = FrictionAccent, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Action Buttons
                        FrictionButton(
                            text = "Start 3-Day Free Trial",
                            onClick = {
                                showEarlyBirdDialog = false
                                onStartFreeTrial()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        )

                        TextButton(
                            onClick = { showEarlyBirdDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Maybe Later", color = TextMuted, fontSize = 14.sp)
                        }
                    }
                }
            }
        )
    }

    if (showTrialExpiredDialog) {
        AlertDialog(
            onDismissRequest = { showTrialExpiredDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TimerOff,
                        contentDescription = null,
                        tint = FrictionAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "3-Day Trial Ended",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Your 3-day Premium trial has completed. Upgrade to Premium to keep unlimited app rules, AI physical challenges, and detailed analytics.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                FrictionButton(
                    text = "Upgrade to Premium",
                    onClick = {
                        showTrialExpiredDialog = false
                        onOpenPaywall()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { showTrialExpiredDialog = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue with Free", color = TextMuted)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (user.isGuestExpired()) {
        AlertDialog(
            onDismissRequest = { /* Force account linking when expired */ },
            confirmButton = {
                FrictionButton(
                    text = "Link Google Account",
                    onClick = onLinkGoogleAccount,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            title = {
                Text(
                    text = "Guest Account Expired",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Your 14-day Guest trial has ended. Please link or sign in with your Google account to permanently save your progress, limits, and stats.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        PremiumBackground(BackgroundStyle.DASHBOARD)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
        // Persistent Guest Warning Card
        if (user.guest && !user.isGuestExpired()) {
            val remainingDays = user.getGuestRemainingDays()
            val daysLabel = if (remainingDays == 1L) "1 day remaining" else "$remainingDays days remaining"

            com.example.core.widgets.GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                backgroundColor = FrictionAccent.copy(alpha = 0.12f),
                borderColor = FrictionAccent.copy(alpha = 0.4f)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShieldMoon,
                                contentDescription = "Guest Warning",
                                tint = FrictionAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Protect your progress",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = FrictionAccent.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = daysLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = FrictionAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "You're using a Guest account. Link your Google account within 14 days to permanently save your progress.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )

                    FrictionButton(
                        text = "Link Google Account",
                        icon = { Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.White) },
                        onClick = onLinkGoogleAccount,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        // 1. Top Greeting Section with Flick Mascot
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkSurface.copy(alpha = 0.85f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Floating Dashboard Mascot
                    Image(
                        painter = painterResource(id = com.example.R.drawable.mascot_dashboard),
                        contentDescription = "Flick Mascot",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(52.dp)
                    )

                    Column {
                        Text(
                            text = "Hello, ${user.displayName.ifEmpty { "Companion" }}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Ready to reclaim your time?",
                            style = MaterialTheme.typography.bodySmall,
                            color = FrictionPrimary
                        )
                    }
                }

                // High-contrast Gradient Avatar (Tap to open Profile)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(FrictionPrimary, FrictionSecondary)
                            )
                        )
                        .clickable { onNavigateToTab("profile") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.displayName.ifEmpty { "C" }.take(1).uppercase(),
                        color = Color(0xFF111315),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // Dashboard Premium Card (Part 3)
        if (!user.premium) {
            com.example.core.widgets.GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenPaywall() },
                shape = RoundedCornerShape(22.dp),
                backgroundColor = FrictionPrimary.copy(alpha = 0.12f),
                borderColor = FrictionPrimary.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.mascot_premium),
                            contentDescription = "Premium Mascot",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(54.dp)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Early Bird Launch Offer",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = FrictionAccent.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "25% OFF",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = FrictionAccent,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Claim your launch pricing before the offer ends.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = FrictionPrimary,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "View Plans →",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = DarkBackground,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // 2. Main Hero Goal Ring & Screen Time Block (Primary Progress Focus)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x10FFFFFF)),
            modifier = Modifier.fillMaxWidth()
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                val isWide = maxWidth > 320.dp
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Side Stats Metrics
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Text(
                            text = "TODAY'S SCREEN LIMIT",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            letterSpacing = 0.5.sp
                        )

                        Column {
                            Text(
                                text = formatTimeMs(todayScreenTimeMs),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "of 4h limit set",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        // Level & Streak Badges in Premium Low-Opacity Accents
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Badge(
                                containerColor = FrictionAccent.copy(alpha = 0.15f),
                                contentColor = FrictionAccent,
                                icon = Icons.Default.Whatshot,
                                label = "${user.currentStreak}d Streak"
                            )
                            Badge(
                                containerColor = FrictionPrimary.copy(alpha = 0.15f),
                                contentColor = FrictionPrimary,
                                icon = Icons.Default.Star,
                                label = "Lvl ${user.level}"
                            )
                        }
                    }

                    // Right Side Animated Progress Ring
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .weight(0.9f),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(80.dp)) {
                            val strokePx = 7.dp.toPx()
                            
                            // Background track
                            drawCircle(
                                color = Color(0x10FFFFFF),
                                style = Stroke(width = strokePx)
                            )

                            // Progress arc
                            drawArc(
                                color = if (animatedProgress.value > 0.85f) FrictionError else FrictionPrimary,
                                startAngle = -90f,
                                sweepAngle = animatedProgress.value * 360f,
                                useCenter = false,
                                style = Stroke(width = strokePx, cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(animatedProgress.value * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                            Text(
                                text = "BUDGET",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                color = TextMuted,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }

        // 3. Mini Stats Blocks (Side-by-Side with Flexible Scaling)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MiniStatBlock(
                title = "Total Unlocks",
                value = "$unlocksToday",
                subText = "Target < 50",
                icon = Icons.Default.PhonelinkLock,
                iconColor = FrictionPrimary,
                modifier = Modifier.weight(1f)
            )

            MiniStatBlock(
                title = "Friction XP",
                value = "${user.xp} XP",
                subText = "Goal 1,000",
                icon = Icons.Default.EmojiEvents,
                iconColor = FrictionAccent,
                modifier = Modifier.weight(1f)
            )
        }

        // 4. Staggered Quick Actions Grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Quick Control Panel",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    title = "Attention Analytics",
                    desc = "Analyze screen cycles",
                    icon = Icons.Default.Timeline,
                    iconColor = Color(0xFF4F46E5),
                    onClick = { onNavigateToTab("analytics") },
                    modifier = Modifier.weight(1f)
                )

                QuickActionCard(
                    title = "Attention Buddies",
                    desc = "Support & compete",
                    icon = Icons.Default.Group,
                    iconColor = FrictionSecondary,
                    onClick = { onNavigateToTab("friends") },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    title = "Friction Engine",
                    desc = "Configure barriers",
                    icon = Icons.Default.Construction,
                    iconColor = FrictionAccent,
                    onClick = { onNavigateToTab("settings") },
                    modifier = Modifier.weight(1f)
                )

                QuickActionCard(
                    title = "Permission Manager",
                    desc = "System protection",
                    icon = Icons.Default.Security,
                    iconColor = FrictionPrimary,
                    onClick = { onNavigateToTab("permissions") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Navigation Card: Help us improve Friction
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DarkSurface.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Brush.horizontalGradient(listOf(FrictionPrimary.copy(alpha = 0.3f), FrictionSecondary.copy(alpha = 0.1f)))),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToTab("feedback") }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(FrictionPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RateReview,
                                contentDescription = "Feedback",
                                tint = FrictionPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Help us improve Friction",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Share feedback or report a bug",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Feedback",
                        tint = TextMuted
                    )
                }
            }
        }
    }
}
}

@Composable
fun Badge(
    containerColor: Color,
    contentColor: Color,
    icon: ImageVector,
    label: String
) {
    Box(
        modifier = Modifier
            .background(containerColor, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@Composable
fun MiniStatBlock(
    title: String,
    value: String,
    subText: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x10FFFFFF)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subText,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    desc: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x10FFFFFF)),
        modifier = modifier
            .height(110.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    lineHeight = 15.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

fun formatTimeMs(ms: Long): String {
    val mins = ms / 60000
    val hours = mins / 60
    val remainingMins = mins % 60
    return if (hours > 0) "${hours}h ${remainingMins}m" else "${remainingMins}m"
}
