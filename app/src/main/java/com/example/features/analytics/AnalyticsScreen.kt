package com.example.features.analytics

import com.example.core.widgets.ResponsiveText
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppUsageInfo
import com.example.data.service.ScreenTimeService
import com.example.features.dashboard.formatTimeMs
import com.example.core.widgets.BackgroundStyle
import com.example.core.widgets.FrictionButton
import com.example.core.widgets.PremiumBackground
import com.example.ui.theme.*

@Composable
fun AnalyticsScreen(
    user: com.example.data.model.User? = null,
    todayScreenTimeMs: Long,
    weeklyScreenTimeMs: Long = 0L,
    monthlyScreenTimeMs: Long = 0L,
    dailyHistory: Map<String, Long>,
    weeklyHistory: Map<String, Long>,
    monthlyHistory: Map<String, Long>,
    topApps: List<AppUsageInfo>,
    weeklyTopApps: List<AppUsageInfo> = emptyList(),
    monthlyTopApps: List<AppUsageInfo> = emptyList(),
    detailedAnalytics: ScreenTimeService.DetailedAnalytics? = null,
    weeklyDetailedAnalytics: ScreenTimeService.DetailedAnalytics? = null,
    monthlyDetailedAnalytics: ScreenTimeService.DetailedAnalytics? = null,
    aiCoachingText: String = "",
    isAiLoading: Boolean = false,
    onGenerateAiCoaching: () -> Unit = {},
    onSetLimit: () -> Unit = {},
    onOpenEngine: () -> Unit = {},
    onClassifyApps: () -> Unit = {},
    onReviewGoal: () -> Unit = {},
    onViewAnalytics: () -> Unit = {},
    onOpenPaywall: () -> Unit = {},
    onVerifyEntitlement: ((Boolean) -> Unit) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dims = com.example.ui.theme.LocalResponsiveDimensions.current
    var selectedTab by remember { mutableStateOf("DAILY") } // "DAILY", "WEEKLY", "MONTHLY"
    val scrollState = rememberScrollState()

    var isPremiumVerified by remember(user?.premium, user?.isTrialActive) {
        mutableStateOf(user?.premium == true || (user?.isTrialActive == true && !user.hasTrialExpired()))
    }
    LaunchedEffect(user?.uid) {
        if (user != null) {
            onVerifyEntitlement { isPremiumVerified = it }
        }
    }

    val currentHistory = when (selectedTab) {
        "DAILY" -> dailyHistory
        "WEEKLY" -> weeklyHistory
        else -> monthlyHistory
    }

    val currentScreenTimeMs = when (selectedTab) {
        "DAILY" -> todayScreenTimeMs
        "WEEKLY" -> weeklyScreenTimeMs
        else -> monthlyScreenTimeMs
    }

    val currentDetailedAnalytics = when (selectedTab) {
        "DAILY" -> detailedAnalytics
        "WEEKLY" -> weeklyDetailedAnalytics ?: detailedAnalytics
        else -> monthlyDetailedAnalytics ?: detailedAnalytics
    }

    val currentTopApps = when (selectedTab) {
        "DAILY" -> topApps
        "WEEKLY" -> weeklyTopApps.ifEmpty { topApps }
        else -> monthlyTopApps.ifEmpty { topApps }
    }

    // Real-time calculated statistics
    val hasRealData = topApps.isNotEmpty() || 
            (selectedTab == "DAILY" && dailyHistory.values.any { it > 0 }) ||
            (selectedTab == "WEEKLY" && weeklyHistory.values.any { it > 0 }) || 
            (selectedTab == "MONTHLY" && monthlyHistory.values.any { it > 0 })

    Box(modifier = modifier.fillMaxSize()) {
        PremiumBackground(BackgroundStyle.ANALYTICS)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = dims.outerPadding, vertical = dims.spacingLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
        
        // 1. Title Row
        Column(modifier = Modifier.fillMaxWidth()) {
            ResponsiveText(
                text = "Attention Analytics",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            ResponsiveText(
                text = "Track your attention flow cycles",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        if (!hasRealData) {
            // Elegant Empty State
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Image(
                        painter = painterResource(id = com.example.R.drawable.mascot_low_data),
                        contentDescription = "Flick Low Data Mascot",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(130.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    ResponsiveText(
                        text = "We're still learning your habits",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    ResponsiveText(
                        text = "Use Friction for a little longer to unlock deeper insights. Not enough usage data yet, but we are ready to analyze your habits as you go.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }
        } else {
            // 2. Tab Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("DAILY", "WEEKLY", "MONTHLY").forEach { tab ->
                    val isSelected = selectedTab == tab
                    val animatedColor by animateColorAsState(
                        targetValue = if (isSelected) DarkCardBg else Color.Transparent,
                        animationSpec = tween(250),
                        label = "tab_bg"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(animatedColor)
                            .clickable { selectedTab = tab }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ResponsiveText(
                            text = when (tab) {
                                "DAILY" -> "Daily"
                                "WEEKLY" -> "Weekly"
                                else -> "Monthly Trends"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) FrictionPrimary else TextSecondary
                        )
                    }
                }
            }

            // 3. Yesterday Comparison Header Banner
            val yesterdayMs = detailedAnalytics?.yesterdayScreenTimeMs ?: 0L
            val comparisonText = when {
                yesterdayMs == 0L -> "Yesterday's data is not loaded yet."
                todayScreenTimeMs < yesterdayMs -> {
                    val diff = yesterdayMs - todayScreenTimeMs
                    "Your screen time is down ${formatTimeMs(diff)} compared to yesterday!"
                }
                else -> {
                    val diff = todayScreenTimeMs - yesterdayMs
                    "Your screen time is up ${formatTimeMs(diff)} compared to yesterday. Stay intentional!"
                }
            }
            val comparisonColor = if (todayScreenTimeMs <= yesterdayMs) FrictionPrimary else FrictionError

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = comparisonColor.copy(alpha = 0.08f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, comparisonColor.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(comparisonColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (todayScreenTimeMs <= yesterdayMs) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = comparisonColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        ResponsiveText(
                            text = if (todayScreenTimeMs <= yesterdayMs) "Excellent mindfulness!" else "Digital fatigue warning",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = comparisonColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        ResponsiveText(
                            text = comparisonText,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Total Screen Time Number-Only Overview Card
            com.example.core.widgets.GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                backgroundColor = DarkCardBg.copy(alpha = 0.9f),
                borderColor = FrictionPrimary.copy(alpha = 0.3f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ResponsiveText(
                        text = when (selectedTab) {
                            "DAILY" -> "TODAY'S SCREEN TIME"
                            "WEEKLY" -> "THIS WEEK'S SCREEN TIME"
                            else -> "THIS MONTH'S SCREEN TIME"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = FrictionPrimary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ResponsiveText(
                        text = formatTimeMs(currentScreenTimeMs),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                }
            }

            if (!isPremiumVerified) {
                // Free Plan Lock Banner for Graphs & AI Insights
                com.example.core.widgets.GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = DarkCardBg.copy(alpha = 0.95f),
                    borderColor = FrictionAccent.copy(alpha = 0.35f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = FrictionAccent.copy(alpha = 0.15f),
                            modifier = Modifier.size(60.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = FrictionAccent,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        ResponsiveText(
                            text = "Interactive Analytics & AI Insights",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        ResponsiveText(
                            text = "Free users can view today's total screen time numbers. Upgrade to Premium for interactive charts, trend breakdowns, and AI habit coaching!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        FrictionButton(
                            text = "Unlock Detailed Analytics",
                            onClick = onOpenPaywall,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                // 4. Custom drawn dynamic Chart Graph
                val currentHistory = when (selectedTab) {
                    "DAILY" -> dailyHistory
                    "WEEKLY" -> weeklyHistory
                    else -> monthlyHistory
                }
                var selectedInterval by remember { mutableStateOf<com.example.data.service.IntervalDetails?>(null) }
                AnalyticsChart(
                    historyMap = currentHistory,
                    isLineChart = selectedTab == "DAILY",
                    onPointClicked = { label, value -> 
                        selectedInterval = com.example.data.service.IntervalDetails(
                            timeLabel = label,
                            screenTimeMs = value
                        )
                    }
                )
                selectedInterval?.let { details ->
                    ChartTooltipBottomSheet(
                        details = details,
                        onDismiss = { selectedInterval = null }
                    )
                }
            }

            // Personal Insights Card (Clean M3 card, no model/backend names)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, FrictionPrimary.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = com.example.R.drawable.mascot_analytics),
                                contentDescription = "Analytics Mascot",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                ResponsiveText(
                                    text = "Personal Insights",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                ResponsiveText(
                                    text = "Habit analysis & actionable recommendations",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (isAiLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = FrictionPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(onClick = onGenerateAiCoaching) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Insights",
                                    tint = FrictionAccent
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0x10FFFFFF))
                    Spacer(modifier = Modifier.height(16.dp))

                    if (aiCoachingText.isBlank() || aiCoachingText.contains("Tap 'Analyze My Habits'")) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ResponsiveText(
                                text = "Analyze your attention patterns using real measured usage data.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onGenerateAiCoaching,
                                colors = ButtonDefaults.buttonColors(containerColor = FrictionPrimary),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                ResponsiveText("Analyze My Habits", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        AiResponseRenderer(
                            responseContent = aiCoachingText,
                            onSetLimit = onSetLimit,
                            onOpenEngine = onOpenEngine,
                            onClassifyApps = onClassifyApps,
                            onReviewGoal = onReviewGoal,
                            onViewAnalytics = onViewAnalytics
                        )
                    }
                }
            }

            // 5. Detailed Usage Statistics Grid (Clean layout, proper padding, no overflow)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ResponsiveText(
                    text = "Usage Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dims.spacingSmall)
                ) {
                    InsightCard(
                        title = "Unlocks",
                        desc = "${currentDetailedAnalytics?.unlockCount ?: 0}",
                        sub = "Screen Pickups",
                        icon = Icons.Default.PhonelinkLock,
                        iconColor = FrictionPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    InsightCard(
                        title = "App Launches",
                        desc = "${currentDetailedAnalytics?.totalLaunches ?: 0}",
                        sub = "Intentions triggered",
                        icon = Icons.Default.Launch,
                        iconColor = FrictionPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dims.spacingSmall)
                ) {
                    InsightCard(
                        title = "Avg Session",
                        desc = formatTimeMs(currentDetailedAnalytics?.averageSessionMs ?: 0L),
                        sub = "Typical attention span",
                        icon = Icons.Default.Timer,
                        iconColor = FrictionPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    InsightCard(
                        title = "Longest Session",
                        desc = formatTimeMs(currentDetailedAnalytics?.longestSessionMs ?: 0L),
                        sub = "Peak focus strain",
                        icon = Icons.Default.HistoryToggleOff,
                        iconColor = FrictionError,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dims.spacingSmall)
                ) {
                    InsightCard(
                        title = "Peak Hours",
                        desc = currentDetailedAnalytics?.peakUsageHours ?: "No Data",
                        sub = "Most active slot",
                        icon = Icons.Default.Schedule,
                        iconColor = FrictionError,
                        modifier = Modifier.weight(1f)
                    )

                    if (selectedTab == "DAILY") {
                        InsightCard(
                            title = "Today's Focus",
                            desc = formatTimeMs(todayScreenTimeMs),
                            sub = "Active usage",
                            icon = Icons.Default.Equalizer,
                            iconColor = FrictionPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        val elapsedDays = when (selectedTab) {
                            "WEEKLY" -> {
                                val cal = java.util.Calendar.getInstance()
                                val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
                                val firstDay = cal.firstDayOfWeek
                                ((dayOfWeek - firstDay + 7) % 7 + 1).coerceAtLeast(1)
                            }
                            else -> {
                                val cal = java.util.Calendar.getInstance()
                                cal.get(java.util.Calendar.DAY_OF_MONTH).coerceAtLeast(1)
                            }
                        }
                        val periodTotal = if (selectedTab == "WEEKLY") weeklyScreenTimeMs else monthlyScreenTimeMs
                        val avgDaily = periodTotal / elapsedDays

                        InsightCard(
                            title = "Daily Average",
                            desc = formatTimeMs(avgDaily),
                            sub = "Over $elapsedDays days",
                            icon = Icons.Default.Equalizer,
                            iconColor = FrictionPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 6. Hourly Distribution mini Bar Chart
            currentDetailedAnalytics?.hourlyDistribution?.let { hourlyDist ->
                if (hourlyDist.values.any { it > 0 }) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x10FFFFFF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            ResponsiveText(
                                text = "Hourly Distribution of Launches",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            HourlySparkline(hourlyDist = hourlyDist)
                        }
                    }
                }
            }

            // 7. Staggered Top Apps list
            if (currentTopApps.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ResponsiveText(
                        text = when (selectedTab) {
                            "DAILY" -> "Most Used Apps Today"
                            "WEEKLY" -> "Most Used Apps This Week"
                            else -> "Most Used Apps This Month"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x10FFFFFF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            currentTopApps.forEach { app ->
                                val isDistracting = app.category == "Social Media" || app.category == "Games" || app.category == "Entertainment"
                                AppUsageRow(app = app, isDistracting = isDistracting)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    if (user != null) {
                        com.example.features.ads.FrictionBannerAd(user = user)
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
}

@Composable
fun HourlySparkline(hourlyDist: Map<Int, Int>) {
    val values = hourlyDist.values.toList()
    val maxVal = (values.maxOrNull() ?: 1).toFloat().coerceAtLeast(1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val pointsCount = values.size
            val stepX = w / (pointsCount - 1).coerceAtLeast(1)

            // Draw spark bar graph
            val itemWidth = (w / pointsCount) * 0.6f
            val spacingX = w / pointsCount

            for (i in 0 until pointsCount) {
                val value = values[i]
                val ratio = value.toFloat() / maxVal
                val barHeight = h * ratio
                val x = i * spacingX
                val y = h - barHeight

                drawRoundRect(
                    color = if (value > maxVal * 0.7f) FrictionError.copy(alpha = 0.75f) else FrictionPrimary.copy(alpha = 0.6f),
                    topLeft = Offset(x, y),
                    size = Size(itemWidth, barHeight.coerceAtLeast(2.dp.toPx())),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ResponsiveText("12 AM", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = TextMuted)
        ResponsiveText("12 PM", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = TextMuted)
        ResponsiveText("11 PM", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = TextMuted)
    }
}

@Composable
fun AppUsageRow(app: AppUsageInfo, isDistracting: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Styled Initial Emblem representing application logo
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isDistracting) FrictionError.copy(alpha = 0.12f) else FrictionPrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            ResponsiveText(
                text = app.appName.take(1).uppercase(),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDistracting) FrictionError else FrictionPrimary
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ResponsiveText(
                        text = app.appName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    if (isDistracting) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(FrictionError.copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            ResponsiveText(
                                text = "Distracting",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = FrictionError
                            )
                        }
                    }
                }
                ResponsiveText(
                    text = formatTimeMs(app.totalTimeInForegroundMs),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Smooth progress bar styled with high contrast
            LinearProgressIndicator(
                progress = { app.relativePercentage / 100f },
                color = if (isDistracting) FrictionError else FrictionPrimary,
                trackColor = Color(0x15FFFFFF),
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )
        }
    }
}

@Composable
fun InsightCard(
    title: String,
    desc: String,
    sub: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x10FFFFFF)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                ResponsiveText(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = TextMuted,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column {
                ResponsiveText(
                    text = desc,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                ResponsiveText(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun AnalyticsChart(
    historyMap: Map<String, Long>,
    isLineChart: Boolean,
    onPointClicked: (String, Long) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x10FFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ResponsiveText(
                text = "Usage Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            val keys = historyMap.keys.toList()
            val values = historyMap.values.toList()
            val maxVal = (values.maxOrNull() ?: 1L).coerceAtLeast(1L).toFloat()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    keys.forEachIndexed { index, label ->
                        val value = values.getOrNull(index) ?: 0L
                        val heightRatio = (value.toFloat() / maxVal).coerceIn(0.08f, 1f)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onPointClicked(label, value) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .fillMaxHeight(heightRatio)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(FrictionPrimary, FrictionSecondary)
                                        )
                                    )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            ResponsiveText(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}
