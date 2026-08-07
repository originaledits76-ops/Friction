package com.example.features.home

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.User
import com.example.ui.theme.*
import com.example.features.analytics.AnalyticsScreen
import com.example.features.auth.LoginViewModel
import com.example.features.dashboard.DashboardScreen
import com.example.features.feedback.FeedbackScreen
import com.example.features.friends.FriendsScreen
import com.example.features.paywall.PaywallScreen
import com.example.features.permission.UsagePermissionScreen
import com.example.features.profile.ProfileScreen
import com.example.features.settings.SettingsScreen

@Composable
fun HomeScreen(
    user: User,
    loginViewModel: LoginViewModel,
    homeViewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf("dashboard") }
    var showPaywall by remember { mutableStateOf(false) }
    val isPermissionGranted by homeViewModel.isPermissionGranted.collectAsState()

    // Trigger check on layout loading
    LaunchedEffect(Unit) {
        homeViewModel.checkPermission()
        homeViewModel.loadBuddiesAndLeaderboard(user.uid)
    }

    if (showPaywall) {
        PaywallScreen(
            onDismiss = { showPaywall = false },
            onPurchaseSuccess = { showPaywall = false }
        )
        return
    }

    if (!isPermissionGranted) {
        UsagePermissionScreen(
            onCheckPermissionAgain = { homeViewModel.checkPermission() },
            modifier = modifier
        )
    } else {
        // Collect flows reactively
        val todayScreenTimeMs by homeViewModel.todayScreenTimeMs.collectAsState()
        val topApps by homeViewModel.topApps.collectAsState()
        val detailedAnalytics by homeViewModel.detailedAnalytics.collectAsState()
        val rules by homeViewModel.rules.collectAsState()
        val challenges by homeViewModel.challenges.collectAsState()
        val friends by homeViewModel.friends.collectAsState()
        val leaderboardWeekly by homeViewModel.leaderboardWeekly.collectAsState()
        val leaderboardMonthly by homeViewModel.leaderboardMonthly.collectAsState()
        val aiCoachingText by homeViewModel.aiCoachingState.collectAsState()
        val isAiLoading by homeViewModel.isAiLoading.collectAsState()

        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = DarkSurface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    NavigationBarItem(
                        selected = selectedTab == "dashboard",
                        onClick = { selectedTab = "dashboard" },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == "dashboard") Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "Dashboard"
                            )
                        },
                        label = { Text("Dashboard") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = FrictionPrimary,
                            selectedTextColor = TextPrimary,
                            indicatorColor = FrictionPrimary.copy(alpha = 0.12f),
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )

                    NavigationBarItem(
                        selected = selectedTab == "analytics",
                        onClick = { selectedTab = "analytics" },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == "analytics") Icons.Filled.Timeline else Icons.Outlined.Timeline,
                                contentDescription = "Analytics"
                            )
                        },
                        label = { Text("Analytics") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = FrictionPrimary,
                            selectedTextColor = TextPrimary,
                            indicatorColor = FrictionPrimary.copy(alpha = 0.12f),
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )

                    NavigationBarItem(
                        selected = selectedTab == "friends",
                        onClick = { selectedTab = "friends" },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == "friends") Icons.Filled.Group else Icons.Outlined.Group,
                                contentDescription = "Buddies"
                            )
                        },
                        label = { Text("Buddies") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = FrictionPrimary,
                            selectedTextColor = TextPrimary,
                            indicatorColor = FrictionPrimary.copy(alpha = 0.12f),
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )

                    NavigationBarItem(
                        selected = selectedTab == "settings",
                        onClick = { selectedTab = "settings" },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == "settings") Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = "Settings"
                            )
                        },
                        label = { Text("Settings") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = FrictionPrimary,
                            selectedTextColor = TextPrimary,
                            indicatorColor = FrictionPrimary.copy(alpha = 0.12f),
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )

                }
            },
            containerColor = DarkBackground,
            modifier = modifier
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Crossfade animation between sub pages for a beautiful, smooth fluid flow
                Crossfade(
                    targetState = selectedTab,
                    animationSpec = tween(350),
                    label = "HomeSubPageCrossfade"
                ) { tab ->
                    when (tab) {
                        "dashboard" -> {
                            DashboardScreen(
                                user = user,
                                todayScreenTimeMs = todayScreenTimeMs,
                                unlocksToday = if (todayScreenTimeMs > 0) (todayScreenTimeMs / 600000L).toInt().coerceAtLeast(1) else 0,
                                onNavigateToTab = { selectedTab = it }
                            )
                        }
                        "analytics" -> {
                            AnalyticsScreen(
                                todayScreenTimeMs = todayScreenTimeMs,
                                dailyHistory = homeViewModel.dailyHistory,
                                weeklyHistory = homeViewModel.weeklyHistory,
                                monthlyHistory = homeViewModel.monthlyHistory,
                                topApps = topApps,
                                detailedAnalytics = detailedAnalytics,
                                aiCoachingText = aiCoachingText,
                                isAiLoading = isAiLoading,
                                onGenerateAiCoaching = { homeViewModel.generateAiCoaching() },
                                onSetLimit = { selectedTab = "settings" },
                                onOpenEngine = { selectedTab = "settings" },
                                onClassifyApps = { selectedTab = "dashboard" },
                                onReviewGoal = { selectedTab = "settings" },
                                onViewAnalytics = { selectedTab = "analytics" }
                            )
                        }
                        "friends" -> {
                            FriendsScreen(
                                friends = friends,
                                leaderboardWeekly = leaderboardWeekly,
                                leaderboardMonthly = leaderboardMonthly,
                                onSendRequest = { homeViewModel.sendFriendRequest(user.uid, it) },
                                onAcceptRequest = { homeViewModel.acceptFriendRequest(user.uid, it) },
                                onRejectRequest = { homeViewModel.rejectFriendRequest(user.uid, it) }
                            )
                        }
                        "settings" -> {
                            SettingsScreen(
                                rules = rules,
                                challenges = challenges,
                                onToggleRule = { id, active -> homeViewModel.toggleRule(id, active) },
                                onAddRule = { homeViewModel.addRule(it) },
                                onDeleteRule = { homeViewModel.deleteRule(it) },
                                onSignOut = { loginViewModel.logout() },
                                homeViewModel = homeViewModel,
                                onOpenPaywall = { showPaywall = true },
                                onOpenFeedback = { selectedTab = "feedback" }
                            )
                        }
                        "profile" -> {
                            ProfileScreen(
                                user = user,
                                onBack = { selectedTab = "dashboard" },
                                onOpenSettings = { selectedTab = "settings" },
                                homeViewModel = homeViewModel
                            )
                        }
                        "feedback" -> {
                            FeedbackScreen(
                                user = user,
                                onBack = { selectedTab = "dashboard" }
                            )
                        }
                    }
                }
            }
        }
    }
}
