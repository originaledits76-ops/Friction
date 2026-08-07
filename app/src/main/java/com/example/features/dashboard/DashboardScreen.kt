package com.example.features.dashboard

import android.text.format.DateFormat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val todayDateStr = DateFormat.getLongDateFormat(context).format(Date())

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
                    title = "Leaderboards",
                    desc = "Global rankings",
                    icon = Icons.Default.Leaderboard,
                    iconColor = FrictionError,
                    onClick = { onNavigateToTab("friends") },
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
