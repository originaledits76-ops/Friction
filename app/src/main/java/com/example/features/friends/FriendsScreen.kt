package com.example.features.friends

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import com.example.core.widgets.PremiumBackground
import com.example.data.model.FriendInfo
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    user: com.example.data.model.User? = null,
    friends: List<FriendInfo>,
    leaderboardWeekly: List<FriendInfo>,
    leaderboardMonthly: List<FriendInfo>,
    onSendRequest: (String) -> Unit,
    onAcceptRequest: (String) -> Unit,
    onRejectRequest: (String) -> Unit,
    onOpenPaywall: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableStateOf("BUDDIES") } // "BUDDIES", "LEADERBOARD"
    var leaderboardType by remember { mutableStateOf("WEEKLY") } // "WEEKLY", "MONTHLY"
    var searchQuery by remember { mutableStateOf("") }
    var showLockedSheet by remember { mutableStateOf(false) }
    var lockedTitle by remember { mutableStateOf("") }
    var lockedDesc by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val pendingReceived = friends.filter { it.status == "RECEIVED" }
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

    Box(modifier = modifier.fillMaxSize()) {
        PremiumBackground(BackgroundStyle.LEADERBOARD)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
        
        // Header
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Buddies & Ranks",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Mindful focus is better together",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Sub Tab Selector (Buddies vs Leaderboard - Premium dark pill bar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
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
                        .clip(RoundedCornerShape(8.dp))
                        .background(animatedColor)
                        .clickable { activeSubTab = tab }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (tab == "BUDDIES") "Attention Buddies" else "Leaderboards",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) FrictionPrimary else TextSecondary
                    )
                }
            }
        }

        // Search buddy input row (Only show on Buddies tab)
        if (activeSubTab == "BUDDIES") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by email address...", style = MaterialTheme.typography.bodyMedium, color = TextMuted) },
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
                        if (activeFriends.size >= 2 && user?.premium != true) {
                            lockedTitle = "Friend Limit Reached"
                            lockedDesc = "Free plan allows up to 2 attention buddies. Upgrade to Premium for unlimited focus friends!"
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
                    Text(
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

        // Scrollable Lists matching state
        Box(modifier = Modifier.weight(1f)) {
            if (activeSubTab == "BUDDIES") {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Pending Requests section
                    if (pendingReceived.isNotEmpty()) {
                        item {
                            Text(
                                text = "Pending Invitations (${pendingReceived.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = FrictionPrimary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        items(pendingReceived) { req ->
                            PendingRequestCard(
                                req = req,
                                onAccept = { onAcceptRequest(req.uid) },
                                onReject = { onRejectRequest(req.uid) }
                            )
                        }
                    }

                    // Friends List
                    item {
                        Text(
                            text = "My Focus Buddies (${activeFriends.size}/2 Free)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    if (activeFriends.isEmpty()) {
                        item {
                            EmptyBuddiesState()
                        }
                    } else {
                        items(activeFriends) { friend ->
                            BuddyCard(friend = friend)
                        }
                    }
                }
            } else if (user?.premium != true) {
                // Locked Leaderboard for Free Plan
                com.example.core.widgets.GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = DarkCardBg.copy(alpha = 0.9f),
                    borderColor = FrictionAccent.copy(alpha = 0.3f)
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
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = FrictionAccent,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Text(
                            text = "Leaderboard is a Premium Feature",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Upgrade to Premium to unlock live weekly & monthly focus rankings and compare screen time with your buddies!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        FrictionButton(
                            text = "Unlock Leaderboard",
                            onClick = onOpenPaywall,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                // Leaderboard list
                val rankedList = if (leaderboardType == "WEEKLY") leaderboardWeekly else leaderboardMonthly
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (rankedList.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Not enough data yet. Start using Friction to unlock leaderboard insights.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(rankedList.withIndex().toList()) { (index, item) ->
                            LeaderboardRankRow(rank = index + 1, item = item)
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun BuddyCard(friend: FriendInfo) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x10FFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF818CF8), Color(0xFFC084FC))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = friend.displayName.take(1).uppercase(),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = friend.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Level ${friend.level} • ${friend.xp} XP",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Streak indicator badge (Premium subtle orange container in Dark Mode)
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
                Text(
                    text = "${friend.currentStreak}d",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFF59E0B)
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
        border = androidx.compose.foundation.BorderStroke(1.dp, FrictionPrimary.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = req.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = req.email,
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
fun LeaderboardRankRow(rank: Int, item: FriendInfo) {
    val highlightColor = when (rank) {
        1 -> Color(0xFFFBBF24) // Gold
        2 -> Color(0xFFCBD5E1) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> Color.Transparent
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rank == 1) highlightColor.copy(alpha = 0.08f) else DarkSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (rank == 1) highlightColor.copy(alpha = 0.2f) else Color(0x10FFFFFF)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Rank Number / Crown visual indicator
                Box(
                    modifier = Modifier
                        .size(32.dp)
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
                    Text(
                        text = "$rank",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (rank <= 3) highlightColor else TextSecondary
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = item.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Level ${item.level}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // XP Score
            Text(
                text = "${item.xp} XP",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = FrictionPrimary
            )
        }
    }
}

@Composable
fun EmptyBuddiesState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.SupervisedUserCircle,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(56.dp)
        )
        Text(
            text = "No focus buddies yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Text(
            text = "Invite friends to keep each other motivated & accountable.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

