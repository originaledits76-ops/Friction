package com.example.features.home

import com.example.core.widgets.ResponsiveText
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
import androidx.compose.ui.text.font.FontWeight
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity
    var selectedTab by remember { mutableStateOf("dashboard") }
    var showPaywall by remember { mutableStateOf(false) }
    var paywallInitialStep by remember { mutableIntStateOf(1) }
    var showRewardedAdDialog by remember { mutableStateOf(false) }
    val isPermissionGranted by homeViewModel.isPermissionGranted.collectAsState()

    // Trigger check on layout loading
    LaunchedEffect(Unit) {
        homeViewModel.checkPermission()
        homeViewModel.loadBuddiesAndLeaderboard(user.uid)
    }

    val isPremium = user.premium || (user.isTrialActive && !user.hasTrialExpired())
    LaunchedEffect(isPremium) {
        com.example.features.ads.AdManager.updateUserPremiumStatus(isPremium, context)
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, showPaywall) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_START) {
                if (!showPaywall) {
                    com.example.features.ads.AdManager.handleAppOpen(context, activity) {
                        !showPaywall
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }


    if (showRewardedAdDialog) {
        AlertDialog(
            onDismissRequest = { showRewardedAdDialog = false },
            title = { ResponsiveText("Unlock AI Analysis", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { ResponsiveText("Watch a short ad to unlock one AI analysis.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary) },
            containerColor = DarkSurface,
            confirmButton = {
                Button(onClick = {
                    showRewardedAdDialog = false
                    activity?.let { act -> com.example.features.ads.AdManager.showRewardedAd(act, onRewardEarned = {
                        homeViewModel.markRewardedAiConsumed()
                        homeViewModel.generateAiCoachingForced(user)
                    }, onDismissed = {}) }
                }, colors = ButtonDefaults.buttonColors(containerColor = FrictionPrimary)) { 
                    ResponsiveText("Watch Ad & Analyse", color = Color.Black, fontWeight = FontWeight.Bold) 
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRewardedAdDialog = false
                    paywallInitialStep = 3
                    showPaywall = true
                }) { 
                    ResponsiveText("Upgrade to Pro", color = TextMuted) 
                }
            }
        )
    }
    if (showPaywall) {
        PaywallScreen(
            user = user,
            initialStep = paywallInitialStep,
            onLinkGoogle = { activity?.let { loginViewModel.linkGoogleAccount(it) } },
            onDismiss = { showPaywall = false },
            onPurchaseSuccess = { plan ->
                homeViewModel.purchasePlan(user, plan.name)
                showPaywall = false
            },
            onRedeemCoupon = { code ->
                if (user == null) {
                    com.example.data.repository.CouponResult.Error("Please log in first.")
                } else {
                    loginViewModel.redeemCoupon(code, user)
                }
            }
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
        val todayHourlyScreenTimeMs by homeViewModel.todayHourlyScreenTimeMs.collectAsState()
        val dailyScreenTimeLimitMs by homeViewModel.dailyScreenTimeLimitMs.collectAsState()
        val weeklyScreenTimeMs by homeViewModel.weeklyScreenTimeMs.collectAsState()
        val monthlyScreenTimeMs by homeViewModel.monthlyScreenTimeMs.collectAsState()

        val topApps by homeViewModel.topApps.collectAsState()
        val weeklyTopApps by homeViewModel.weeklyTopApps.collectAsState()
        val monthlyTopApps by homeViewModel.monthlyTopApps.collectAsState()

        val detailedAnalytics by homeViewModel.detailedAnalytics.collectAsState()
        val weeklyDetailedAnalytics by homeViewModel.weeklyDetailedAnalytics.collectAsState()
        val monthlyDetailedAnalytics by homeViewModel.monthlyDetailedAnalytics.collectAsState()

        val dailyHistory by homeViewModel.dailyHistory.collectAsState()
        val weeklyHistory by homeViewModel.weeklyHistory.collectAsState()
        val monthlyHistory by homeViewModel.monthlyHistory.collectAsState()
        val rules by homeViewModel.rules.collectAsState()
        val challenges by homeViewModel.challenges.collectAsState()
        val friends by homeViewModel.friends.collectAsState()
        val browseFriendsList by homeViewModel.browseFriendsList.collectAsState()
        val selectedBuddyDetails by homeViewModel.selectedBuddyDetails.collectAsState()
        val isBuddyDetailsLoading by homeViewModel.isBuddyDetailsLoading.collectAsState()
        val leaderboardWeekly by homeViewModel.leaderboardWeekly.collectAsState()
        val leaderboardMonthly by homeViewModel.leaderboardMonthly.collectAsState()
        val aiCoachingText by homeViewModel.aiCoachingState.collectAsState()
        val isAiLoading by homeViewModel.isAiLoading.collectAsState()

        var showTrialDialog by remember { mutableStateOf(false) }

        if (showTrialDialog) {
            AlertDialog(
                onDismissRequest = { showTrialDialog = false },
                title = { ResponsiveText("Free Trial Activated", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge) },
                text = {
                    ResponsiveText(
                        "Your 3-day free trial has started. Restart Friction to access Pro features.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showTrialDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = FrictionPrimary)
                    ) {
                        ResponsiveText("Got it", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = DarkCardBg,
                titleContentColor = TextPrimary,
                textContentColor = TextSecondary
            )
        }

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
                        label = { ResponsiveText("Dashboard", maxLines = 1, softWrap = false) },
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
                        label = { ResponsiveText("Analytics", maxLines = 1, softWrap = false) },
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
                        label = { ResponsiveText("Buddies", maxLines = 1, softWrap = false) },
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
                        label = { ResponsiveText("Settings", maxLines = 1, softWrap = false) },
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
                                todayHourlyScreenTimeMs = todayHourlyScreenTimeMs,
                                dailyScreenTimeLimitMs = dailyScreenTimeLimitMs,
                                unlocksToday = detailedAnalytics?.unlockCount ?: 0,
                                onNavigateToTab = { selectedTab = it },
                                onSetDailyLimit = { homeViewModel.setDailyScreenTimeLimit(it) },
                                onLinkGoogleAccount = { activity?.let { loginViewModel.linkGoogleAccount(it) } },
                                homeViewModel = homeViewModel,
                                onStartFreeTrial = {
                                    homeViewModel.startFreeTrial(user) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Your 3-day free trial has started. Restart Friction to access Pro features.",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                        showTrialDialog = true
                                    }
                                },
                                onMarkOfferSeen = { homeViewModel.markEarlyBirdOfferSeen(user) },
                                onOpenPaywall = { paywallInitialStep = 1; showPaywall = true }
                            )
                        }
                        "analytics" -> {
                            AnalyticsScreen(
                                user = user,
                                todayScreenTimeMs = todayScreenTimeMs,
                                weeklyScreenTimeMs = weeklyScreenTimeMs,
                                monthlyScreenTimeMs = monthlyScreenTimeMs,
                                dailyHistory = dailyHistory,
                                weeklyHistory = weeklyHistory,
                                monthlyHistory = monthlyHistory,
                                topApps = topApps,
                                weeklyTopApps = weeklyTopApps,
                                monthlyTopApps = monthlyTopApps,
                                detailedAnalytics = detailedAnalytics,
                                weeklyDetailedAnalytics = weeklyDetailedAnalytics,
                                monthlyDetailedAnalytics = monthlyDetailedAnalytics,
                                aiCoachingText = aiCoachingText,
                                isAiLoading = isAiLoading,
                                onGenerateAiCoaching = {
                                    homeViewModel.generateAiCoaching(
                                        user = user,
                                        onPaywallRequired = {
                                            paywallInitialStep = 3
                                            showPaywall = true
                                        },
                                        onAdPromptRequired = {
                                            showRewardedAdDialog = true
                                        }
                                    )
                                },
                                onSetLimit = { selectedTab = "settings" },
                                onOpenEngine = { selectedTab = "settings" },
                                onClassifyApps = { selectedTab = "dashboard" },
                                onReviewGoal = { selectedTab = "settings" },
                                onViewAnalytics = { selectedTab = "analytics" },
                                onOpenPaywall = { paywallInitialStep = 3; showPaywall = true },
                                onVerifyEntitlement = { cb -> user?.let { homeViewModel.verifyPremiumEntitlement(it, cb) } }
                            )
                        }
                        "friends" -> {
                            FriendsScreen(
                                user = user,
                                friends = friends,
                                browseFriendsList = browseFriendsList,
                                leaderboardWeekly = leaderboardWeekly,
                                leaderboardMonthly = leaderboardMonthly,
                                selectedBuddyDetails = selectedBuddyDetails,
                                isBuddyDetailsLoading = isBuddyDetailsLoading,
                                onLoadBrowseFriends = { user?.let { homeViewModel.loadBrowseFriends(it.uid) } },
                                onSendRequest = { user?.let { u -> homeViewModel.sendFriendRequest(u.uid, it) } },
                                onSendRequestToUid = { targetUid -> user?.let { u -> homeViewModel.sendFriendRequestToUid(u.uid, targetUid) } },
                                onAcceptRequest = { friendUid ->
                                    user?.let { u ->
                                        val activeFriendsCount = friends.count { f -> f.status == "FRIEND" }
                                        val isPremium = u.premium || (u.isTrialActive && !u.hasTrialExpired())
                                        homeViewModel.acceptFriendRequest(
                                            userUid = u.uid,
                                            friendUid = friendUid,
                                            activeFriendsCount = activeFriendsCount,
                                            isPremium = isPremium,
                                            onLimitReached = { paywallInitialStep = 3; showPaywall = true }
                                        )
                                    }
                                },
                                onRejectRequest = { friendUid -> user?.let { u -> homeViewModel.rejectFriendRequest(u.uid, friendUid) } },
                                onSelectBuddy = { buddyUid -> homeViewModel.loadBuddyDetails(buddyUid) },
                                onDismissBuddyDetails = { homeViewModel.clearBuddyDetails() },
                                onOpenPaywall = { paywallInitialStep = 3; showPaywall = true },
                                onVerifyEntitlement = { cb -> user?.let { homeViewModel.verifyPremiumEntitlement(it, cb) } }
                            )
                        }
                        "settings" -> {
                            SettingsScreen(
                                user = user,
                                rules = rules,
                                challenges = challenges,
                                onToggleRule = { id, active -> homeViewModel.toggleRule(id, active) },
                                onAddRule = { homeViewModel.addRule(it) },
                                onDeleteRule = { homeViewModel.deleteRule(it) },
                                onSignOut = { loginViewModel.logout() },
                                homeViewModel = homeViewModel,
                                onOpenPaywall = { paywallInitialStep = 3; showPaywall = true },

                                onOpenFeedback = { selectedTab = "feedback" },
                                onOpenPermissions = { selectedTab = "permissions" }
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
                        "permissions" -> {
                            com.example.features.permission.PermissionManagerScreen(
                                homeViewModel = homeViewModel,
                                onBack = { selectedTab = "dashboard" }
                            )
                        }
                        "xp" -> {
                            com.example.features.xp.XpManagementScreen(
                                user = user,
                                homeViewModel = homeViewModel,
                                onBack = { selectedTab = "dashboard" },
                                onNavigateToTab = { selectedTab = it }
                            )
                        }
                    }
                }
            }
        }
    }
}
