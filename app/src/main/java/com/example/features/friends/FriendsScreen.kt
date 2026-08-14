package com.example.features.friends

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.widgets.BackgroundStyle
import com.example.core.widgets.FrictionButton
import com.example.core.widgets.GlassCard
import com.example.core.widgets.PremiumBackground
import com.example.core.widgets.ResponsiveText
import com.example.data.model.BuddyAppUsage
import com.example.data.model.BuddyDetails
import com.example.data.model.FriendInfo
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    user: com.example.data.model.User? = null,
    friends: List<FriendInfo>,
    browseFriendsList: List<FriendInfo> = emptyList(),
    leaderboardWeekly: List<FriendInfo> = emptyList(),
    leaderboardMonthly: List<FriendInfo> = emptyList(),
    selectedBuddyDetails: BuddyDetails? = null,
    isBuddyDetailsLoading: Boolean = false,
    onLoadBrowseFriends: () -> Unit = {},
    onSendRequest: (String) -> Unit = {},
    onSendRequestToUid: (String) -> Unit = {},
    onAcceptRequest: (String) -> Unit = {},
    onRejectRequest: (String) -> Unit = {},
    onSelectBuddy: (String) -> Unit = {},
    onDismissBuddyDetails: () -> Unit = {},
    onOpenPaywall: () -> Unit = {},
    onVerifyEntitlement: ((Boolean) -> Unit) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableStateOf("BUDDIES") } // "BUDDIES", "LEADERBOARD"
    var leaderboardType by remember { mutableStateOf("WEEKLY") } // "WEEKLY", "MONTHLY"
    var searchQuery by remember { mutableStateOf("") }
    var browseSearchQuery by remember { mutableStateOf("") }
    var showBrowseSheet by remember { mutableStateOf(false) }
    var showLockedSheet by remember { mutableStateOf(false) }
    var lockedTitle by remember { mutableStateOf("") }
    var lockedDesc by remember { mutableStateOf("") }

    var isPremiumVerified by remember(user?.premium, user?.isTrialActive) {
        mutableStateOf(user?.premium == true || (user?.isTrialActive == true && !user.hasTrialExpired()))
    }

    LaunchedEffect(Unit) {
        onVerifyEntitlement { isPremiumVerified = it }
    }

    val pendingReceived = friends.filter { it.status == "RECEIVED" }
    val pendingSent = friends.filter { it.status == "SENT" }
    val activeFriends = friends.filter { it.status == "FRIEND" }

    if (showLockedSheet) {
        com.example.features.paywall.LockedFeatureSheet(
            featureTitle = lockedTitle,
            featureDescription = lockedDesc,
            onUpgrade = {
                showLockedSheet = false
                onOpenPaywall()
            },
            onDismiss = { showLockedSheet = false }
        )
    }

    // Browse Friends Bottom Sheet / Modal
    if (showBrowseSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBrowseSheet = false },
            containerColor = DarkBackground,
            scrimColor = Color.Black.copy(alpha = 0.7f),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            BrowseFriendsSheetContent(
                browseFriendsList = browseFriendsList,
                searchQuery = browseSearchQuery,
                onSearchQueryChange = { browseSearchQuery = it },
                onSendRequestToUid = onSendRequestToUid,
                onAcceptRequest = { targetUid ->
                    if (activeFriends.size >= 2 && !isPremiumVerified) {
                        lockedTitle = "Friend Limit Reached"
                        lockedDesc = "Free plan allows up to 2 Focus Buddies. Upgrade to Premium for unlimited focus buddies!"
                        showLockedSheet = true
                    } else {
                        onAcceptRequest(targetUid)
                    }
                },
                onRejectRequest = onRejectRequest,
                onDismiss = { showBrowseSheet = false }
            )
        }
    }

    // Buddy Details Bottom Sheet
    if (selectedBuddyDetails != null || isBuddyDetailsLoading) {
        ModalBottomSheet(
            onDismissRequest = onDismissBuddyDetails,
            containerColor = DarkBackground,
            scrimColor = Color.Black.copy(alpha = 0.7f),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            BuddyDetailsSheetContent(
                buddyDetails = selectedBuddyDetails,
                isLoading = isBuddyDetailsLoading,
                onDismiss = onDismissBuddyDetails
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        PremiumBackground(BackgroundStyle.LEADERBOARD)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            Column(modifier = Modifier.fillMaxWidth()) {
                ResponsiveText(
                    text = "Buddies & Leaderboard",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                ResponsiveText(
                    text = "Mindful focus is better together",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            // Sub Tab Selector (Buddies vs Leaderboard)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("BUDDIES", "LEADERBOARD").forEach { tab ->
                    val isSelected = activeSubTab == tab
                    val animatedColor by animateColorAsState(
                        targetValue = if (isSelected) DarkCardBg else Color.Transparent,
                        animationSpec = tween(250)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(animatedColor)
                            .clickable {
                                activeSubTab = tab
                                if (tab == "LEADERBOARD" && !isPremiumVerified) {
                                    lockedTitle = "Leaderboard is Pro Only"
                                    lockedDesc = "Track weekly and monthly focus rankings and compete with focus champions with Friction Pro."
                                    showLockedSheet = true
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ResponsiveText(
                            text = if (tab == "BUDDIES") "Focus Buddies" else "Leaderboard",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) FrictionPrimary else TextSecondary
                        )
                    }
                }
            }

            // Search & Invite input row (Only show on Buddies tab)
            if (activeSubTab == "BUDDIES") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { ResponsiveText("Invite buddy by email or username...", style = MaterialTheme.typography.bodyMedium, color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = FrictionPrimary,
                            unfocusedBorderColor = Color(0x15FFFFFF),
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )

                    FrictionButton(
                        text = "Invite",
                        onClick = {
                            if (activeFriends.size >= 2 && !isPremiumVerified) {
                                lockedTitle = "Friend Limit Reached"
                                lockedDesc = "Free plan allows up to 2 Focus Buddies. Upgrade to Premium for unlimited focus buddies!"
                                showLockedSheet = true
                            } else if (searchQuery.isNotEmpty()) {
                                onSendRequest(searchQuery)
                                searchQuery = ""
                            }
                        },
                        modifier = Modifier.width(90.dp)
                    )
                }
            } else {
                // Leaderboard toggler (Weekly vs Monthly)
                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .align(Alignment.End)
                        .background(DarkSurface, shape = RoundedCornerShape(10.dp))
                        .padding(2.dp)
                ) {
                    listOf("WEEKLY", "MONTHLY").forEach { type ->
                        val isSelected = leaderboardType == type
                        ResponsiveText(
                            text = type,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) FrictionPrimary else TextSecondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) DarkCardBg else Color.Transparent)
                                .clickable { leaderboardType = type }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Scrollable Content area
            Box(modifier = Modifier.weight(1f)) {
                if (activeSubTab == "BUDDIES") {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        // Prominent "Browse Friends" Button
                        item {
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onLoadBrowseFriends()
                                        showBrowseSheet = true
                                    },
                                shape = RoundedCornerShape(20.dp),
                                backgroundColor = DarkSurface,
                                borderColor = FrictionPrimary.copy(alpha = 0.3f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = FrictionPrimary.copy(alpha = 0.15f),
                                            modifier = Modifier.size(42.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.GroupAdd,
                                                    contentDescription = "Browse Friends",
                                                    tint = FrictionPrimary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }

                                        Column {
                                            ResponsiveText(
                                                text = "Browse All Friends",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            ResponsiveText(
                                                text = "Discover & connect with focus users",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = FrictionPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        // Pending Incoming Requests section
                        if (pendingReceived.isNotEmpty()) {
                            item {
                                ResponsiveText(
                                    text = "Pending Invitations (${pendingReceived.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FrictionPrimary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            items(pendingReceived) { req ->
                                PendingRequestCard(
                                    req = req,
                                    onAccept = {
                                        if (activeFriends.size >= 2 && !isPremiumVerified) {
                                            lockedTitle = "Friend Limit Reached"
                                            lockedDesc = "Free plan allows up to 2 Focus Buddies. Upgrade to Premium for unlimited focus buddies!"
                                            showLockedSheet = true
                                        } else {
                                            onAcceptRequest(req.uid)
                                        }
                                    },
                                    onReject = { onRejectRequest(req.uid) }
                                )
                            }
                        }

                        // Sent Requests section
                        if (pendingSent.isNotEmpty()) {
                            item {
                                ResponsiveText(
                                    text = "Sent Requests (${pendingSent.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FrictionAccent,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            items(pendingSent) { req ->
                                SentRequestCard(req = req)
                            }
                        }

                        // Active Friends List
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ResponsiveText(
                                    text = "My Focus Buddies",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isPremiumVerified) FrictionPrimary.copy(alpha = 0.15f) else DarkCardBg,
                                    border = BorderStroke(1.dp, if (isPremiumVerified) FrictionPrimary.copy(alpha = 0.3f) else Color(0x15FFFFFF))
                                ) {
                                    ResponsiveText(
                                        text = if (isPremiumVerified) "${activeFriends.size} Buddies (Unlimited)" else "${activeFriends.size}/2 Free Buddies",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isPremiumVerified) FrictionPrimary else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        if (activeFriends.isEmpty()) {
                            item {
                                EmptyBuddiesState(
                                    onBrowse = {
                                        onLoadBrowseFriends()
                                        showBrowseSheet = true
                                    }
                                )
                            }
                        } else {
                            items(activeFriends) { friend ->
                                BuddyCard(
                                    friend = friend,
                                    onClick = { onSelectBuddy(friend.uid) }
                                )
                            }
                        }

                        // Banner Ad positioned at absolute bottom of friends list content
                        if (user != null) {
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                                com.example.features.ads.FrictionBannerAd(user = user)
                            }
                        }
                    }
                } else {
                    // Leaderboard list - Premium entitlement enforced
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        if (!isPremiumVerified) {
                            item {
                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    backgroundColor = DarkCardBg,
                                    borderColor = FrictionAccent.copy(alpha = 0.4f)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(CircleShape)
                                                .background(FrictionAccent.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Locked Feature",
                                                tint = FrictionAccent,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }

                                        ResponsiveText(
                                            text = "Leaderboard is Pro Only",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            textAlign = TextAlign.Center
                                        )

                                        ResponsiveText(
                                            text = "Compare focus streaks, track rankings, and compete with focus champions by upgrading to Friction Pro.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary,
                                            textAlign = TextAlign.Center
                                        )

                                        FrictionButton(
                                            text = "Unlock Leaderboard",
                                            onClick = { onOpenPaywall() },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            val rankedList = if (leaderboardType == "WEEKLY") leaderboardWeekly else leaderboardMonthly
                            if (rankedList.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        ResponsiveText(
                                            text = "Loading leaderboard rankings...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextMuted,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                items(rankedList.withIndex().toList()) { (index, item) ->
                                    LeaderboardRankRow(
                                        rank = index + 1,
                                        item = item,
                                        isCurrentUser = item.uid == user?.uid
                                    )
                                }
                            }
                        }

                        // Banner Ad positioned at absolute bottom of leaderboard list
                        if (user != null) {
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                                com.example.features.ads.FrictionBannerAd(user = user)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BuddyCard(
    friend: FriendInfo,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, Color(0x15FFFFFF)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF818CF8), Color(0xFFC084FC))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    ResponsiveText(
                        text = friend.displayName.take(1).uppercase().ifBlank { "F" },
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    ResponsiveText(
                        text = friend.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    ResponsiveText(
                        text = "Level ${friend.level} • ${friend.xp} XP",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Streak indicator badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(Color(0x15F59E0B), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(12.dp)
                    )
                    ResponsiveText(
                        text = "${friend.currentStreak}d",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFF59E0B)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View Details",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun PendingRequestCard(
    req: FriendInfo,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = FrictionPrimary.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, FrictionPrimary.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                ResponsiveText(
                    text = req.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                ResponsiveText(
                    text = "Incoming focus request",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onAccept,
                    modifier = Modifier
                        .size(36.dp)
                        .background(FrictionPrimary, CircleShape)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Accept", tint = DarkBackground, modifier = Modifier.size(18.dp))
                }

                IconButton(
                    onClick = onReject,
                    modifier = Modifier
                        .size(36.dp)
                        .background(DarkCardBg, CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Reject", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun SentRequestCard(req: FriendInfo) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, Color(0x15FFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                ResponsiveText(
                    text = req.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                ResponsiveText(
                    text = "Invitation sent",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Surface(
                color = FrictionAccent.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, FrictionAccent.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.HourglassEmpty, null, tint = FrictionAccent, modifier = Modifier.size(14.dp))
                    ResponsiveText(
                        text = "Waiting",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = FrictionAccent
                    )
                }
            }
        }
    }
}

@Composable
fun LeaderboardRankRow(
    rank: Int,
    item: FriendInfo,
    isCurrentUser: Boolean = false
) {
    val highlightColor = when (rank) {
        1 -> Color(0xFFFBBF24) // Gold
        2 -> Color(0xFFCBD5E1) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> Color.Transparent
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCurrentUser -> FrictionPrimary.copy(alpha = 0.15f)
                rank == 1 -> highlightColor.copy(alpha = 0.08f)
                else -> DarkSurface
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = when {
                isCurrentUser -> FrictionPrimary.copy(alpha = 0.4f)
                rank <= 3 -> highlightColor.copy(alpha = 0.3f)
                else -> Color(0x10FFFFFF)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if (rank <= 3) highlightColor.copy(alpha = 0.15f) else Color(0x0FFFFFFF),
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = if (rank <= 3) highlightColor.copy(alpha = 0.3f) else Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    ResponsiveText(
                        text = "$rank",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (rank <= 3) highlightColor else TextSecondary
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ResponsiveText(
                            text = item.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        if (isCurrentUser) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = FrictionPrimary,
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                ResponsiveText(
                                    text = "YOU",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkBackground,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    ResponsiveText(
                        text = "Level ${item.level} • ${item.currentStreak}d streak",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            ResponsiveText(
                text = "${item.xp} XP",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = FrictionPrimary
            )
        }
    }
}

@Composable
fun EmptyBuddiesState(onBrowse: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = Icons.Default.SupervisedUserCircle,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(56.dp)
        )
        ResponsiveText(
            text = "No focus buddies connected yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        ResponsiveText(
            text = "Browse app users to send requests or invite friends to keep each other accountable.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        FrictionButton(
            text = "Browse App Users",
            onClick = onBrowse,
            modifier = Modifier.width(180.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseFriendsSheetContent(
    browseFriendsList: List<FriendInfo>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSendRequestToUid: (String) -> Unit,
    onAcceptRequest: (String) -> Unit,
    onRejectRequest: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val filteredList = remember(browseFriendsList, searchQuery) {
        if (searchQuery.isBlank()) {
            browseFriendsList
        } else {
            browseFriendsList.filter {
                it.displayName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                ResponsiveText(
                    text = "Browse Friends",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                ResponsiveText(
                    text = "Connect with authenticated Friction users",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
            }
        }

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { ResponsiveText("Search user by username...", style = MaterialTheme.typography.bodyMedium, color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = FrictionPrimary,
                unfocusedBorderColor = Color(0x15FFFFFF),
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                ResponsiveText(
                    text = if (searchQuery.isBlank()) "No users found in the app yet." else "No users matching '$searchQuery'.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList) { userItem ->
                    BrowseUserCard(
                        userItem = userItem,
                        onSendRequest = { onSendRequestToUid(userItem.uid) },
                        onAccept = { onAcceptRequest(userItem.uid) },
                        onReject = { onRejectRequest(userItem.uid) }
                    )
                }
            }
        }
    }
}

@Composable
fun BrowseUserCard(
    userItem: FriendInfo,
    onSendRequest: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, Color(0x15FFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF38BDF8), Color(0xFF818CF8))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    ResponsiveText(
                        text = userItem.displayName.take(1).uppercase().ifBlank { "U" },
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    ResponsiveText(
                        text = userItem.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    ResponsiveText(
                        text = "Level ${userItem.level} • ${userItem.xp} XP",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Relationship Status Action Button
            when (userItem.status) {
                "FRIEND" -> {
                    Surface(
                        color = FrictionPrimary.copy(alpha = 0.15f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, FrictionPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.HowToReg,
                                contentDescription = "Already Focus Buddies",
                                tint = FrictionPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                "SENT" -> {
                    Surface(
                        color = FrictionAccent.copy(alpha = 0.15f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, FrictionAccent.copy(alpha = 0.3f)),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Friend Request Pending",
                                tint = FrictionAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                "RECEIVED" -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onAccept,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(FrictionPrimary)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Accept Request", tint = DarkBackground, modifier = Modifier.size(20.dp))
                        }
                        IconButton(
                            onClick = onReject,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DarkCardBg)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Reject Request", tint = TextSecondary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                else -> {
                    // "NONE" - Replace wide Send Request text with compact person_add icon
                    IconButton(
                        onClick = onSendRequest,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(FrictionPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Send Friend Request",
                            tint = DarkBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BuddyDetailsSheetContent(
    buddyDetails: BuddyDetails?,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f)
            .padding(horizontal = 22.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ResponsiveText(
                text = "Focus Buddy Profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
            }
        }

        if (isLoading || buddyDetails == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = FrictionPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Card
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = DarkSurface,
                        borderColor = FrictionPrimary.copy(alpha = 0.2f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFF818CF8), Color(0xFFC084FC))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                ResponsiveText(
                                    text = buddyDetails.displayName.take(1).uppercase().ifBlank { "B" },
                                    color = Color.White,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                ResponsiveText(
                                    text = buddyDetails.displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = FrictionPrimary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        ResponsiveText(
                                            text = "Level ${buddyDetails.level}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = FrictionPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Surface(
                                        color = FrictionAccent.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        ResponsiveText(
                                            text = "${buddyDetails.xp} XP",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = FrictionAccent,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Surface(
                                        color = Color(0x15F59E0B),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Whatshot, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                                            ResponsiveText(
                                                text = "${buddyDetails.currentStreak}d Streak",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFFF59E0B)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Today's Screen Time Summary Card
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        backgroundColor = DarkSurface,
                        borderColor = Color(0x15FFFFFF)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = FrictionPrimary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.AccessTime,
                                            contentDescription = null,
                                            tint = FrictionPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Column {
                                    ResponsiveText(
                                        text = "Today's Screen Time",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    ResponsiveText(
                                        text = "Real-time tracked usage today",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            ResponsiveText(
                                text = formatDurationMs(buddyDetails.todayScreenTimeMs),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = FrictionPrimary
                            )
                        }
                    }
                }

                // Top Apps Today Section
                item {
                    ResponsiveText(
                        text = "Most-Used Apps Today",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (buddyDetails.topAppsToday.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                ResponsiveText(
                                    text = "No app usage synced for today yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(buddyDetails.topAppsToday) { app ->
                        BuddyAppUsageRow(app = app)
                    }
                }
            }
        }
    }
}

@Composable
fun BuddyAppUsageRow(app: BuddyAppUsage) {
    val isProductive = app.classification == "PRODUCTIVE"
    val badgeColor = if (isProductive) Color(0xFF10B981) else Color(0xFFF59E0B)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, Color(0x10FFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = badgeColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isProductive) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    ResponsiveText(
                        text = app.appName.ifBlank { app.packageName },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        color = badgeColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        ResponsiveText(
                            text = if (isProductive) "PRODUCTIVE" else "DISTRACTING",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            ResponsiveText(
                text = formatDurationMs(app.totalTimeMs),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

fun formatDurationMs(ms: Long): String {
    if (ms <= 0) return "0m"
    val totalMinutes = ms / (1000 * 60)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
