package com.example.features.xp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.widgets.BackgroundStyle
import com.example.core.widgets.GlassCard
import com.example.core.widgets.PremiumBackground
import com.example.core.widgets.ResponsiveText
import com.example.data.model.User
import com.example.features.dashboard.formatTimeMs
import com.example.features.home.HomeViewModel
import com.example.ui.theme.*

@Composable
fun XpManagementScreen(
    user: User?,
    homeViewModel: HomeViewModel?,
    onBack: () -> Unit,
    onNavigateToTab: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val userXp = homeViewModel?.userXp?.collectAsState()?.value ?: (user?.xp ?: 0)
    val userLevel = homeViewModel?.userLevel?.collectAsState()?.value ?: (user?.level ?: 1)
    val userStreak = homeViewModel?.userStreak?.collectAsState()?.value ?: (user?.currentStreak ?: 0)
    val isTodayLimitRewarded = homeViewModel?.todayDailyLimitRewarded?.collectAsState()?.value ?: false
    val todayResistanceXp = homeViewModel?.todayResistanceXp?.collectAsState()?.value ?: 0
    val loginStreakDays = homeViewModel?.loginStreakDays?.collectAsState()?.value ?: 1
    val streakSaversRemaining = homeViewModel?.streakSaversRemaining?.collectAsState()?.value ?: 4
    val dailyScreenTimeLimitMs = homeViewModel?.dailyScreenTimeLimitMs?.collectAsState()?.value ?: 0L
    val todayScreenTimeMs = homeViewModel?.todayScreenTimeMs?.collectAsState()?.value ?: 0L

    // XP calculation thresholds
    val currentLevelXpThreshold = (userLevel * 100)
    val xpForCurrentLevel = userXp % 100
    val levelProgress = (xpForCurrentLevel.toFloat() / 100f).coerceIn(0f, 1f)

    Box(modifier = modifier.fillMaxSize()) {
        PremiumBackground(BackgroundStyle.DASHBOARD)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Top Bar Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkSurface)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    ResponsiveText(
                        text = "XP & Streaks Management",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    ResponsiveText(
                        text = "Track your focus level & rewards progression",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Main Level & XP Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = DarkCardBg,
                borderColor = FrictionAccent.copy(alpha = 0.4f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(FrictionAccent.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stars,
                                    contentDescription = "XP Icon",
                                    tint = FrictionAccent,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Column {
                                ResponsiveText(
                                    text = "Level $userLevel Mindful Master",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                ResponsiveText(
                                    text = "$userXp Total XP Earned",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, FrictionAccent.copy(alpha = 0.4f))
                        ) {
                            ResponsiveText(
                                text = "🔥 $userStreak Days",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = FrictionAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                maxLines = 1
                            )
                        }
                    }

                    // Level Progress Bar
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ResponsiveText(
                                text = "Progress to Level ${userLevel + 1}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            ResponsiveText(
                                text = "$xpForCurrentLevel / 100 XP",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = FrictionAccent,
                                fontSize = 11.sp
                            )
                        }

                        LinearProgressIndicator(
                            progress = { levelProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = FrictionAccent,
                            trackColor = DarkSurface
                        )
                    }
                }
            }

            // Section: Daily Rewards Breakdown
            ResponsiveText(
                text = "Daily Rewards Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                backgroundColor = DarkCardBg,
                borderColor = Color(0x15FFFFFF)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Daily Limit Reward
                    XpDetailItem(
                        icon = Icons.Default.Timer,
                        iconTint = FrictionPrimary,
                        title = "Daily Screen-Time Limit",
                        subtitle = if (dailyScreenTimeLimitMs > 0) "Target: ${formatTimeMs(dailyScreenTimeLimitMs)}" else "No limit set",
                        reward = "+30 XP",
                        statusText = when {
                            dailyScreenTimeLimitMs <= 0L -> "Configure limit to earn daily XP"
                            isTodayLimitRewarded -> "Earned (+30 XP) ✓"
                            todayScreenTimeMs <= dailyScreenTimeLimitMs -> "On track (+30 XP at day end)"
                            else -> "Limit exceeded today (0 XP)"
                        },
                        statusColor = when {
                            dailyScreenTimeLimitMs <= 0L -> TextMuted
                            isTodayLimitRewarded -> Color(0xFF22C55E)
                            todayScreenTimeMs <= dailyScreenTimeLimitMs -> FrictionPrimary
                            else -> FrictionError
                        }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    // 2. Resistance XP
                    XpDetailItem(
                        icon = Icons.Default.Shield,
                        iconTint = Color(0xFF3B82F6),
                        title = "App Resistance Bonus",
                        subtitle = "Earn 1 XP each time you resist launching a blocked app",
                        reward = "+$todayResistanceXp / 10 XP",
                        statusText = "$todayResistanceXp of 10 daily resistance XP collected today",
                        statusColor = if (todayResistanceXp >= 10) Color(0xFF22C55E) else Color(0xFF3B82F6)
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    // 3. Login Streak Bonus
                    val loginBonusToday = loginStreakDays * 5
                    XpDetailItem(
                        icon = Icons.Default.CalendarToday,
                        iconTint = Color(0xFFA855F7),
                        title = "Daily Login Streak",
                        subtitle = "Log in every day to increase your multiplier (+5 XP × day)",
                        reward = "+$loginBonusToday XP",
                        statusText = "Login Day $loginStreakDays achieved (+ $loginBonusToday XP) ✓",
                        statusColor = Color(0xFFA855F7)
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    // 4. Streak Savers
                    XpDetailItem(
                        icon = Icons.Default.LocalFireDepartment,
                        iconTint = Color(0xFFFF6B00),
                        title = "Monthly Streak Savers",
                        subtitle = "Auto-protects your streak if you miss a day (Max 4 per calendar month)",
                        reward = "$streakSaversRemaining / 4 Savers",
                        statusText = "$streakSaversRemaining streak savers remaining for this month",
                        statusColor = Color(0xFFFF6B00)
                    )
                }
            }

            // Section: How XP & Levels Work
            ResponsiveText(
                text = "XP Rules & Guidelines",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                backgroundColor = DarkSurface,
                borderColor = Color(0x15FFFFFF)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    XpRuleBullet(
                        rule = "Stay Under Daily Limit",
                        description = "Earn +30 XP every calendar day you stay within your configured screen time limit."
                    )
                    XpRuleBullet(
                        rule = "Resist Blocked Apps",
                        description = "Earn +1 XP for each attempt you turn away when prompted by Friction (capped at 10 XP/day)."
                    )
                    XpRuleBullet(
                        rule = "Maintain Login Streak",
                        description = "Receive a daily login reward equal to 5 XP multiplied by your current login day streak."
                    )
                    XpRuleBullet(
                        rule = "Fair Play Protection",
                        description = "Challenges do not award XP directly to prevent repetitive farming. XP comes from genuine mindfulness!"
                    )
                }
            }

            // Banner Ad at Bottom
            if (user != null) {
                Spacer(modifier = Modifier.height(8.dp))
                com.example.features.ads.FrictionBannerAd(user = user)
            }
        }
    }
}

@Composable
private fun XpDetailItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    reward: String,
    statusText: String,
    statusColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
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

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    ResponsiveText(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    ResponsiveText(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(100.dp),
                color = iconTint.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, iconTint.copy(alpha = 0.3f))
            ) {
                ResponsiveText(
                    text = reward,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = iconTint,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    maxLines = 1
                )
            }
        }

        ResponsiveText(
            text = statusText,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = statusColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun XpRuleBullet(
    rule: String,
    description: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = FrictionPrimary,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Column {
            ResponsiveText(
                text = rule,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            ResponsiveText(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}
