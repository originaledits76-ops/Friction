package com.example.features.profile

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.widgets.BackgroundStyle
import com.example.core.widgets.FrictionButton
import com.example.core.widgets.PremiumBackground
import com.example.data.model.User
import com.example.features.home.HomeViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: User,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    homeViewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Editable form state initialized from current User
    var nameState by remember(user) { mutableStateOf(user.displayName) }
    var ageState by remember(user) { mutableStateOf(if (user.age > 0) user.age.toString() else "") }
    var goalState by remember(user) { mutableStateOf(user.goal.ifEmpty { "Build better focus habits" }) }
    var motivationState by remember(user) { mutableStateOf(user.motivation) }
    var focusPreference by remember { mutableStateOf("Productivity & Social") }

    var isSaving by remember { mutableStateOf(false) }

    val rules by homeViewModel.rules.collectAsState()
    val challenges by homeViewModel.challenges.collectAsState()
    val completedChallengesCount = challenges.count { it.solved }
    val monitoredAppsCount = rules.count { it.active }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        PremiumBackground(style = BackgroundStyle.SETTINGS)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Top Bar with Back Button and Top-Right Gear Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = CircleShape,
                    color = DarkSurface.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { onBack() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                }

                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                // Gear Icon for Settings Entry
                Surface(
                    shape = CircleShape,
                    color = DarkSurface.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { onOpenSettings() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = FrictionPrimary
                        )
                    }
                }
            }

            // 1. Profile Header Glass Card
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = DarkSurface.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.04f)))),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Large Avatar
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(FrictionPrimary, FrictionSecondary)
                                )
                            )
                    ) {
                        Text(
                            text = nameState.ifEmpty { "C" }.take(1).uppercase(),
                            color = Color(0xFF111315),
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = nameState.ifEmpty { "Friction Companion" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Auth Type Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (user.guest) FrictionSecondary.copy(alpha = 0.15f) else FrictionPrimary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (user.guest) FrictionSecondary.copy(alpha = 0.3f) else FrictionPrimary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (user.guest) Icons.Default.PersonOutline else Icons.Default.Verified,
                                contentDescription = null,
                                tint = if (user.guest) FrictionSecondary else FrictionPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (user.guest) "Guest Member" else "Google Member",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (user.guest) FrictionSecondary else FrictionPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Quick Stats Pills Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileHeaderPill(
                            icon = Icons.Default.LocalFireDepartment,
                            value = "${user.currentStreak} Days",
                            label = "Current Streak",
                            iconColor = Color(0xFFFF6B00)
                        )
                        ProfileHeaderPill(
                            icon = Icons.Default.Star,
                            value = "${user.xp} XP",
                            label = "Total Experience",
                            iconColor = Color(0xFFFFB800)
                        )
                        ProfileHeaderPill(
                            icon = Icons.Default.Psychology,
                            value = "Lvl ${user.level}",
                            label = "Mind Level",
                            iconColor = FrictionPrimary
                        )
                    }
                }
            }

            // 2. Statistics Section
            Text(
                text = "Mindfulness Statistics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatGlassTile(
                    title = "Monitored Apps",
                    value = "$monitoredAppsCount active",
                    icon = Icons.Default.Apps,
                    modifier = Modifier.weight(1f)
                )
                StatGlassTile(
                    title = "Challenges Completed",
                    value = "$completedChallengesCount done",
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
            }

            // 3. Personal Information Form Section
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = DarkSurface.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = "Personal Information",
                            tint = FrictionPrimary
                        )
                        Text(
                            text = "Personal Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    com.example.features.feedback.GlassTextField(
                        value = nameState,
                        onValueChange = { nameState = it },
                        label = "Display Name",
                        placeholder = "Enter your name"
                    )

                    com.example.features.feedback.GlassTextField(
                        value = ageState,
                        onValueChange = { ageState = it.filter { char -> char.isDigit() } },
                        label = "Age",
                        placeholder = "e.g. 24"
                    )

                    com.example.features.feedback.GlassTextField(
                        value = goalState,
                        onValueChange = { goalState = it },
                        label = "Focus Goal",
                        placeholder = "e.g. Reduce social media screen time"
                    )

                    com.example.features.feedback.GlassTextField(
                        value = motivationState,
                        onValueChange = { motivationState = it },
                        label = "Personal Motivation",
                        placeholder = "Why do you want to build better habits?",
                        singleLine = false,
                        minLines = 2
                    )

                    // Classification Preference Choice
                    Column {
                        Text(
                            text = "App Classification Preference",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("Productivity & Social", "Mindfulness", "Deep Focus").forEach { pref ->
                                val isSelected = focusPreference == pref
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) FrictionPrimary.copy(alpha = 0.2f) else DarkBackground.copy(alpha = 0.6f),
                                    border = BorderStroke(1.dp, if (isSelected) FrictionPrimary else Color.White.copy(alpha = 0.08f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { focusPreference = pref }
                                ) {
                                    Text(
                                        text = pref,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) FrictionPrimary else TextSecondary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    FrictionButton(
                        text = "Save Profile Changes",
                        onClick = {
                            isSaving = true
                            coroutineScope.launch {
                                val updated = user.copy(
                                    displayName = nameState,
                                    age = ageState.toIntOrNull() ?: user.age,
                                    goal = goalState,
                                    motivation = motivationState
                                )
                                homeViewModel.updateUserProfile(updated)
                                isSaving = false
                                Toast.makeText(context, "Profile updated & saved to Firestore!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        isLoading = isSaving,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileHeaderPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    iconColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            fontSize = 11.sp
        )
    }
}

@Composable
fun StatGlassTile(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = DarkSurface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier
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
                    imageVector = icon,
                    contentDescription = title,
                    tint = FrictionPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
