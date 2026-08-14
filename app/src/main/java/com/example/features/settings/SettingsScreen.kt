package com.example.features.settings

import com.example.core.widgets.ResponsiveText
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Challenge
import com.example.data.model.FrictionRule
import com.example.data.model.RuleType
import com.example.data.service.FrictionAccessibilityService
import com.example.features.home.HomeViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.util.UUID

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val service = "${context.packageName}/${FrictionAccessibilityService::class.java.canonicalName}"
    val enabledServicesSetting = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
    return enabledServicesSetting?.contains(service) == true
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    user: com.example.data.model.User? = null,
    rules: List<FrictionRule>,
    challenges: List<Challenge>,
    onToggleRule: (String, Boolean) -> Unit,
    onAddRule: (FrictionRule) -> Unit,
    onDeleteRule: (String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel? = null,
    onOpenPaywall: () -> Unit = {},
    onOpenFeedback: () -> Unit = {},
    onOpenPermissions: () -> Unit = {}
) {
    val context = LocalContext.current
    var showAppClassification by remember { mutableStateOf(false) }
    var showWizard by remember { mutableStateOf(false) }
    var showLockedSheet by remember { mutableStateOf(false) }
    var lockedTitle by remember { mutableStateOf("Limit Threshold Reached") }
    var lockedDesc by remember { mutableStateOf("Free users can create up to 2 active limits. Upgrade to Premium for unlimited app limits and custom friction rules.") }

    var isAccessibilityEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
    }

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

    if (showAppClassification) {
        AppClassificationScreen(
            onDismiss = { showAppClassification = false },
            homeViewModel = homeViewModel
        )
        return
    }

    if (showWizard) {
        AddLimitWizard(
            homeViewModel = homeViewModel,
            isPremium = user?.premium == true,
            onOpenPaywall = onOpenPaywall,
            onDismiss = { showWizard = false },
            onSave = { rule ->
                onAddRule(rule)
                showWizard = false
            }
        )
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            com.example.core.widgets.PremiumBackground(com.example.core.widgets.BackgroundStyle.SETTINGS)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        ResponsiveText(
                            text = "Friction Engine",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        ResponsiveText(
                            text = "Control your attention.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Default.Logout, contentDescription = "Sign Out", tint = TextMuted)
                    }
                }
            }

            // Premium Banner Card
            item {
                com.example.core.widgets.GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenPaywall,
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = FrictionPrimary.copy(alpha = 0.12f),
                    borderColor = FrictionPrimary.copy(alpha = 0.35f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(FrictionPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Premium",
                                    tint = Color.White
                                )
                            }
                            Column {
                                ResponsiveText(
                                    text = "Friction Premium",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                ResponsiveText(
                                    text = "Unlock AI Coach & Unlimited Limits",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Upgrade",
                            tint = FrictionPrimary
                        )
                    }
                }
            }
            
            // Accessibility Warning
            if (!isAccessibilityEnabled) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = FrictionError.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth().clickable {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        }
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = FrictionError)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                ResponsiveText("Accessibility Required", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                                ResponsiveText("Tap to enable the Friction blocker service in Settings.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                        }
                    }
                }
            }

            if (rules.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = FrictionPrimary.copy(alpha = 0.5f),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        ResponsiveText(
                            text = "No limits yet.",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        ResponsiveText(
                            text = "Create your first limit and make your screen time intentional.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(48.dp))
                        com.example.core.widgets.FrictionButton(
                            text = "Add Limit",
                            icon = { Icon(Icons.Default.Add, contentDescription = null) },
                            onClick = { showWizard = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                // Dashboard summary
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val activeCount = rules.count { it.active }
                        StatCard("Active Limits", "$activeCount", modifier = Modifier.weight(1f))
                        StatCard("Today's XP", "+${homeViewModel?.userXp?.collectAsState()?.value ?: 0}", modifier = Modifier.weight(1f))
                    }
                }

                // Quick Add Limit button
                item {
                    com.example.core.widgets.FrictionButton(
                        text = "Add Limit",
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        onClick = {
                            if (rules.size >= 2) {
                                if (user != null && homeViewModel != null) {
                                    homeViewModel.verifyPremiumEntitlement(user) { isEntitled ->
                                        if (isEntitled) {
                                            showWizard = true
                                        } else {
                                            lockedTitle = "Unlimited App Barriers"
                                            lockedDesc = "Free users can create up to 2 app limits. Upgrade to Premium or start your 3-day trial to create unlimited app barriers."
                                            showLockedSheet = true
                                        }
                                    }
                                } else {
                                    showLockedSheet = true
                                }
                            } else {
                                showWizard = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Active Limits
                item {
                    ResponsiveText("Your Limits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                
                items(rules) { rule ->
                    LimitCard(
                        rule = rule,
                        onToggle = { active -> onToggleRule(rule.id, active) },
                        onDelete = { onDeleteRule(rule.id) }
                    )
                }

                // Permission Manager Card
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenPermissions() }
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
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(FrictionPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = "Permissions",
                                        tint = FrictionPrimary
                                    )
                                }
                                Column {
                                    ResponsiveText(
                                        text = "Permission Manager",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    ResponsiveText(
                                        text = "Manage usage, overlay, activity & battery permissions",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Permissions",
                                tint = TextMuted
                            )
                        }
                    }
                }

                // Feedback & Support Card
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenFeedback() }
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
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(FrictionPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RateReview,
                                        contentDescription = "Feedback",
                                        tint = FrictionPrimary
                                    )
                                }
                                Column {
                                    ResponsiveText(
                                        text = "Feedback & Support",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    ResponsiveText(
                                        text = "Report a bug, request features, or share thoughts",
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
}
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ResponsiveText(text = title, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(modifier = Modifier.height(4.dp))
            ResponsiveText(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

@Composable
fun TemplateCard(title: String, description: String, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(FrictionPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = FrictionPrimary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                ResponsiveText(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                ResponsiveText(text = description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Icon(Icons.Default.AddCircleOutline, contentDescription = "Apply", tint = FrictionPrimary)
        }
    }
}

@Composable
fun LimitCard(
    rule: FrictionRule,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(FrictionAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Apps, contentDescription = null, tint = FrictionAccent)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        ResponsiveText(text = rule.targetAppName.takeIf { it.isNotEmpty() } ?: rule.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        ResponsiveText(
                            text = if (rule.thresholdMinutes > 0) "Every ${rule.thresholdMinutes} mins" else "Triggers on app open",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                Switch(
                    checked = rule.active,
                    onCheckedChange = { onToggle(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = FrictionPrimary)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            val challengeLabel = when (rule.challengeType.uppercase()) {
                "MATH" -> "Math Puzzle"
                "TYPING" -> "Typing Challenge"
                "BOX_BREATHING" -> "Box Breathing"
                "PARAGRAPH_SUMMARY" -> "Paragraph Summary"
                "REMEMBER_PATTERN", "PATTERN" -> "Remember the Pattern"
                else -> rule.challengeType.replace("_", " ")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                ResponsiveText(text = challengeLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = FrictionPrimary)
                Spacer(modifier = Modifier.weight(1f))
                ResponsiveText(text = "+${rule.penaltyXp} XP", style = MaterialTheme.typography.labelMedium, color = FrictionAccent, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextMuted)
                }
            }
        }
    }
}

