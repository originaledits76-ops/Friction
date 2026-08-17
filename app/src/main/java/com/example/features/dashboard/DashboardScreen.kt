package com.example.features.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.widgets.BackgroundStyle
import com.example.core.widgets.FrictionButton
import com.example.core.widgets.GlassCard
import com.example.core.widgets.NeumorphicCard
import com.example.core.widgets.PremiumBackground
import com.example.core.widgets.ResponsiveText
import com.example.data.model.User
import com.example.features.home.HomeViewModel
import com.example.ui.theme.*

fun formatTimeMs(ms: Long): String {
    val totalMinutes = ms / (1000 * 60)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    user: User?,
    todayScreenTimeMs: Long,
    todayHourlyScreenTimeMs: Map<Int, Long>,
    dailyScreenTimeLimitMs: Long,
    unlocksToday: Int,
    onNavigateToTab: (String) -> Unit,
    onSetDailyLimit: (Long) -> Unit,
    onLinkGoogleAccount: () -> Unit,
    homeViewModel: HomeViewModel? = null,
    onStartFreeTrial: () -> Unit = {},
    onMarkOfferSeen: () -> Unit = {},
    onOpenPaywall: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showSetLimitDialog by remember { mutableStateOf(false) }

    val userXp = homeViewModel?.userXp?.collectAsState()?.value ?: (user?.xp ?: 0)
    val userLevel = homeViewModel?.userLevel?.collectAsState()?.value ?: (user?.level ?: 1)
    val userStreak = homeViewModel?.userStreak?.collectAsState()?.value ?: (user?.currentStreak ?: 0)
    val isPremiumVerified = user?.premium == true || (user?.isTrialActive == true && !user.hasTrialExpired())

    if (showSetLimitDialog) {
        SetDailyLimitDialog(
            currentLimitMs = dailyScreenTimeLimitMs,
            onDismiss = { showSetLimitDialog = false },
            onConfirm = { newLimitMs ->
                onSetDailyLimit(newLimitMs)
                showSetLimitDialog = false
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        PremiumBackground(BackgroundStyle.DASHBOARD)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Row: Greeting & Streak (Responsive Collision Protected)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Column {
                        ResponsiveText(
                            text = "Hello, ${user?.displayName?.ifBlank { "Mindful User" } ?: "Mindful User"}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        ResponsiveText(
                            text = "Track & master your digital balance",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = DarkCardBg,
                    border = BorderStroke(1.dp, FrictionAccent.copy(alpha = 0.4f)),
                    modifier = Modifier.clickable { onNavigateToTab("friends") }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ResponsiveText(
                            text = "🔥 $userStreak Days",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = FrictionAccent,
                            maxLines = 1
                        )
                    }
                }
            }

            // Google Account Link Prompt Banner (If guest)
            if (user?.guest == true) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLinkGoogleAccount() },
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = DarkSurface,
                    borderColor = FrictionPrimary.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(FrictionPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Sign In",
                                tint = FrictionPrimary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            ResponsiveText(
                                text = "Link Google Account",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            ResponsiveText(
                                text = "Save your XP, streaks, and progress across devices.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = FrictionPrimary
                        )
                    }
                }
            }

            // Early Bird Trial Offer Banner (For non-premium users)
            if (!isPremiumVerified) {
                EarlyBirdTrialCard(
                    onStartTrialClick = onStartFreeTrial,
                    onOpenPaywallClick = onOpenPaywall
                )
            }

            // Screen Time Summary Card
            TodayScreenTimeCard(
                todayScreenTimeMs = todayScreenTimeMs,
                dailyLimitMs = dailyScreenTimeLimitMs,
                unlocksToday = unlocksToday,
                onSetLimitClick = { showSetLimitDialog = true }
            )

            // Screen Time Progression Graph (Restored & Gated for Pro Users)
            if (isPremiumVerified) {
                DashboardScreenTimeGraph(hourlyData = todayHourlyScreenTimeMs)
            } else {
                LockedScreenTimeGraphCard(onUnlockClick = onOpenPaywall)
            }

            // XP & Streaks Navigation Card
            XpNavigationCard(
                userXp = userXp,
                userLevel = userLevel,
                userStreak = userStreak,
                onClick = { onNavigateToTab("xp") }
            )

            // Navigation Quick Actions Grid
            ResponsiveText(
                text = "Quick Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        title = "App Limits",
                        subtitle = "Configure rules",
                        icon = Icons.Default.Shield,
                        iconTint = FrictionPrimary,
                        onClick = { onNavigateToTab("settings") },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        title = "Analytics",
                        subtitle = "View trends",
                        icon = Icons.Default.BarChart,
                        iconTint = Color(0xFF3B82F6),
                        onClick = { onNavigateToTab("analytics") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        title = "Focus Buddies",
                        subtitle = "Leaderboard",
                        icon = Icons.Default.People,
                        iconTint = FrictionAccent,
                        onClick = { onNavigateToTab("friends") },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        title = "XP & Streaks",
                        subtitle = "Level & rewards",
                        icon = Icons.Default.Stars,
                        iconTint = Color(0xFFEAB308),
                        onClick = { onNavigateToTab("xp") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        title = "Permissions",
                        subtitle = "Manage security",
                        icon = Icons.Default.Security,
                        iconTint = Color(0xFF10B981),
                        onClick = { onNavigateToTab("permissions") },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        title = "Feedback & Support",
                        subtitle = "Help & reports",
                        icon = Icons.Default.Feedback,
                        iconTint = Color(0xFF8B5CF6),
                        onClick = { onNavigateToTab("feedback") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        title = "Profile",
                        subtitle = "Account settings",
                        icon = Icons.Default.Person,
                        iconTint = Color(0xFFF59E0B),
                        onClick = { onNavigateToTab("profile") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            // Bottom Banner Ad
            if (user != null) {
                Spacer(modifier = Modifier.height(8.dp))
                com.example.features.ads.FrictionBannerAd(user = user)
            }
        }
    }
}

@Composable
fun TodayScreenTimeCard(
    todayScreenTimeMs: Long,
    dailyLimitMs: Long,
    unlocksToday: Int,
    onSetLimitClick: () -> Unit
) {
    val progress = if (dailyLimitMs > 0) {
        (todayScreenTimeMs.toFloat() / dailyLimitMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val isOverLimit = dailyLimitMs > 0 && todayScreenTimeMs > dailyLimitMs

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        backgroundColor = DarkCardBg,
        borderColor = if (isOverLimit) FrictionError.copy(alpha = 0.5f) else Color(0x20FFFFFF)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    ResponsiveText(
                        text = "Screen Time Today",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    ResponsiveText(
                        text = if (dailyLimitMs > 0) "Daily Limit: ${formatTimeMs(dailyLimitMs)}" else "No limit set",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface,
                    border = BorderStroke(1.dp, FrictionPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.clickable { onSetLimitClick() }
                ) {
                    ResponsiveText(
                        text = if (dailyLimitMs > 0) "Change Limit" else "Set Daily Limit",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = FrictionPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        maxLines = 1
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ResponsiveText(
                    text = formatTimeMs(todayScreenTimeMs),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isOverLimit) FrictionError else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                ResponsiveText(
                    text = "$unlocksToday Unlocks",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextSecondary,
                    maxLines = 1
                )
            }

            if (dailyLimitMs > 0) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = if (isOverLimit) FrictionError else FrictionPrimary,
                    trackColor = DarkSurface
                )
            }
        }
    }
}

@Composable
fun DashboardScreenTimeGraph(
    hourlyData: Map<Int, Long>,
    modifier: Modifier = Modifier
) {
    val hasData = hourlyData.values.any { it > 0 }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        backgroundColor = DarkCardBg,
        borderColor = FrictionPrimary.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    ResponsiveText(
                        text = "Screen-Time Progression",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    ResponsiveText(
                        text = "Hourly focus activity today",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = FrictionPrimary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, FrictionPrimary.copy(alpha = 0.3f))
                ) {
                    ResponsiveText(
                        text = "PRO FEATURE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = FrictionPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        maxLines = 1
                    )
                }
            }

            if (!hasData) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ResponsiveText(
                            text = "Not enough data yet",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ResponsiveText(
                            text = "Use your device today to see hourly progression",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val hours = (0..23).toList()
                val valuesMinutes = hours.map { h -> ((hourlyData[h] ?: 0L) / (1000 * 60)).toFloat() }
                val maxVal = (valuesMinutes.maxOrNull() ?: 60f).coerceAtLeast(15f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height - 20.dp.toPx()
                        val stepX = w / 23f

                        val points = valuesMinutes.mapIndexed { i, mins ->
                            val x = i * stepX
                            val y = h - ((mins / maxVal) * (h - 10.dp.toPx()))
                            Offset(x, y)
                        }

                        val path = Path().apply {
                            if (points.isNotEmpty()) {
                                moveTo(points[0].x, points[0].y)
                                for (i in 1 until points.size) {
                                    val p0 = points[i - 1]
                                    val p1 = points[i]
                                    val controlX = (p0.x + p1.x) / 2f
                                    cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                                }
                            }
                        }

                        val fillPath = Path().apply {
                            addPath(path)
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    FrictionPrimary.copy(alpha = 0.35f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = h
                            )
                        )

                        drawPath(
                            path = path,
                            color = FrictionPrimary,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )

                        points.forEachIndexed { i, pt ->
                            if (valuesMinutes[i] > 0) {
                                drawCircle(
                                    color = FrictionAccent,
                                    radius = 3.5.dp.toPx(),
                                    center = pt
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ResponsiveText("12 AM", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = TextMuted)
                    ResponsiveText("6 AM", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = TextMuted)
                    ResponsiveText("12 PM", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = TextMuted)
                    ResponsiveText("6 PM", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = TextMuted)
                    ResponsiveText("11 PM", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = TextMuted)
                }
            }
        }
    }
}

@Composable
fun LockedScreenTimeGraphCard(
    onUnlockClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUnlockClick() },
        shape = RoundedCornerShape(24.dp),
        backgroundColor = DarkCardBg,
        borderColor = FrictionAccent.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(FrictionAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked Feature",
                    tint = FrictionAccent,
                    modifier = Modifier.size(24.dp)
                )
            }

            ResponsiveText(
                text = "Screen-Time Progression Graph",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            ResponsiveText(
                text = "Track hourly focus trends and screen time progression throughout the day with Friction Pro.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            FrictionButton(
                text = "Unlock Progression Graph",
                onClick = onUnlockClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            )
        }
    }
}

@Composable
fun XpNavigationCard(
    userXp: Int,
    userLevel: Int,
    userStreak: Int,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        backgroundColor = DarkCardBg,
        borderColor = FrictionAccent.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(FrictionAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Stars,
                        contentDescription = "XP Management",
                        tint = FrictionAccent,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    ResponsiveText(
                        text = "XP & Streaks Management",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    ResponsiveText(
                        text = "Level $userLevel • $userXp XP • 🔥 $userStreak Days",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = DarkSurface,
                border = BorderStroke(1.dp, FrictionAccent.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier.padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open XP Management",
                        tint = FrictionAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        backgroundColor = DarkCardBg,
        borderColor = Color(0x15FFFFFF)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            ResponsiveText(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            ResponsiveText(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SetDailyLimitDialog(
    currentLimitMs: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var hours by remember { mutableStateOf((currentLimitMs / (1000 * 60 * 60)).toInt()) }
    var minutes by remember { mutableStateOf(((currentLimitMs / (1000 * 60)) % 60).toInt()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            ResponsiveText(
                text = "Set Daily Screen-Time Limit",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ResponsiveText(
                    text = "Configure your target maximum daily screen time. Staying under this limit earns +30 XP daily.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ResponsiveText("Hours", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (hours > 0) hours-- }) {
                                Icon(Icons.Default.Remove, null, tint = TextPrimary)
                            }
                            ResponsiveText("$hours h", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = FrictionPrimary)
                            IconButton(onClick = { if (hours < 23) hours++ }) {
                                Icon(Icons.Default.Add, null, tint = TextPrimary)
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ResponsiveText("Minutes", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (minutes >= 15) minutes -= 15 else if (minutes > 0) minutes = 0 }) {
                                Icon(Icons.Default.Remove, null, tint = TextPrimary)
                            }
                            ResponsiveText("$minutes m", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = FrictionPrimary)
                            IconButton(onClick = { if (minutes <= 45) minutes += 15 else minutes = 59 }) {
                                Icon(Icons.Default.Add, null, tint = TextPrimary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            FrictionButton(
                text = "Save Limit",
                onClick = {
                    val totalMs = (hours * 3600L + minutes * 60L) * 1000L
                    onConfirm(totalMs)
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                ResponsiveText("Cancel", color = TextMuted)
            }
        }
    )
}

@Composable
fun EarlyBirdTrialCard(
    onStartTrialClick: () -> Unit,
    onOpenPaywallClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onOpenPaywallClick() },
        color = Color(0xFF0F172A),
        border = BorderStroke(
            width = 1.5.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFFF59E0B),
                    FrictionPrimary,
                    FrictionAccent
                )
            )
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E293B).copy(alpha = 0.9f),
                            Color(0xFF0F172A).copy(alpha = 0.95f)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(14.dp)
                            )
                            ResponsiveText(
                                text = "EARLY BIRD SPECIAL OFFER",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "View offer",
                        tint = FrictionPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ResponsiveText(
                        text = "Claim Your 3-Day Free Trial 🎁",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    ResponsiveText(
                        text = "Unlock Pro hourly analytics graphs, unlimited app blockers, and custom friction barriers with zero ads.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onStartTrialClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FrictionPrimary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        ResponsiveText(
                            text = "Start 3-Day Trial Free",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.Black
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenPaywallClick,
                        border = BorderStroke(1.dp, FrictionPrimary.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        ResponsiveText(
                            text = "Plans",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelMedium,
                            color = FrictionPrimary
                        )
                    }
                }
            }
        }
    }
}
