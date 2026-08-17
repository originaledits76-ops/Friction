package com.example.features.paywall

import com.example.core.widgets.ResponsiveText
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.widgets.FrictionButton
import com.example.core.widgets.GlassCard
import com.example.core.widgets.PremiumBackground
import com.example.core.widgets.BackgroundStyle
import com.example.data.model.User
import com.example.ui.theme.*

enum class PlanType {
    MONTHLY, YEARLY, LIFETIME
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    user: User? = null,
    initialStep: Int = 1,
    onLinkGoogle: () -> Unit = {},
    onDismiss: () -> Unit,
    onPurchaseSuccess: (PlanType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableIntStateOf(initialStep.coerceIn(1, 3)) }
    var selectedPlan by remember { mutableStateOf(PlanType.YEARLY) }
    var isProcessing by remember { mutableStateOf(false) }
    var showGuestDialog by remember { mutableStateOf(false) }

    val totalSteps = 3

    if (showGuestDialog) {
        AlertDialog(
            onDismissRequest = { showGuestDialog = false },
            title = {
                ResponsiveText(
                    text = "Google Account Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                ResponsiveText(
                    text = "Premium subscriptions require a Google account.\n\nGoogle login protects purchases across devices and prevents loss of access.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                FrictionButton(
                    text = "Link Google Account",
                    onClick = {
                        showGuestDialog = false
                        onLinkGoogle()
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showGuestDialog = false }) {
                    ResponsiveText("Cancel", color = TextMuted)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        PremiumBackground(style = BackgroundStyle.SETTINGS)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (currentStep > 1) {
                            currentStep--
                        } else {
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = if (currentStep > 1) Icons.Default.ArrowBack else Icons.Outlined.Close,
                        contentDescription = "Back or Close",
                        tint = TextPrimary
                    )
                }

                // Step Progress Indicator Badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = FrictionPrimary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, FrictionPrimary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ResponsiveText(
                            text = "Step $currentStep of $totalSteps",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = FrictionPrimary
                        )
                    }
                }

                TextButton(
                    onClick = {
                        onPurchaseSuccess(selectedPlan) // Mock restore
                    }
                ) {
                    ResponsiveText(
                        text = "Restore",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        textDecoration = TextDecoration.Underline
                    )
                }
            }

            // Step Horizontal Progress Line
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (step in 1..totalSteps) {
                    val isActive = step <= currentStep
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (isActive) FrictionPrimary else Color.White.copy(alpha = 0.1f)
                            )
                    )
                }
            }

            // Multi-step Animated Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { width -> width } + fadeIn() togetherWith
                                    slideOutHorizontally { width -> -width } + fadeOut()
                        } else {
                            slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                    slideOutHorizontally { width -> width } + fadeOut()
                        }
                    },
                    label = "PaywallStepTransition"
                ) { step ->
                    when (step) {
                        1 -> Step1BenefitsAndFeatures()
                        2 -> Step2ClearComparison()
                        3 -> Step3PlanSelectionAndPurchase(
                            selectedPlan = selectedPlan,
                            onSelectPlan = { selectedPlan = it }
                        )
                    }
                }
            }

            // Bottom Navigation Action Button
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FrictionButton(
                    text = when (currentStep) {
                        1 -> "Compare Plans"
                        2 -> "Select Plan"
                        else -> if (isProcessing) "Processing Purchase..." else "Continue with 3-Day Trial"
                    },
                    onClick = {
                        if (currentStep < totalSteps) {
                            currentStep++
                        } else {
                            if (user?.guest == true) {
                                showGuestDialog = true
                            } else {
                                isProcessing = true
                                onPurchaseSuccess(selectedPlan)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = FrictionSecondaryText,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    ResponsiveText(
                        text = if (currentStep == totalSteps) "🔒 Encrypted & Secure Payment via Google Play" else "Step $currentStep of $totalSteps • Cancel Anytime",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun Step1BenefitsAndFeatures() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Mascot removed per user request
            ResponsiveText(
                text = "Unlock Complete Focus",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            ResponsiveText(
                text = "Build deep intentional habits with Pro friction barriers & AI insights.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            borderColor = FrictionPrimary.copy(alpha = 0.35f),
            backgroundColor = FrictionPrimary.copy(alpha = 0.08f)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(4.dp)) {
                FeatureRowCompact(Icons.Default.Block, "100% Ad-Free Experience", "Enjoy Friction without any ads or disruptive interruptions.")
                FeatureRowCompact(Icons.Default.Psychology, "AI Habit Analysis & Coaching", "Personalized recommendations to reduce screen friction.")
                FeatureRowCompact(Icons.Default.SelfImprovement, "Box Breathing & Mindful Challenges", "Custom cycles to calm impulse before opening apps.")
                FeatureRowCompact(Icons.Default.AllInclusive, "Unlimited App Limits", "Create restrictions for as many apps as you need.")
                FeatureRowCompact(Icons.Default.Analytics, "Advanced Usage Analytics", "Hourly trends and deep category breakdowns.")
                FeatureRowCompact(Icons.Default.Shield, "Active App Blocker", "Instant overlay whenever opening blocked apps.")
            }
        }
    }
}

@Composable
private fun FeatureRowCompact(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(FrictionAccent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = FrictionAccent, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            ResponsiveText(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            ResponsiveText(text = subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
private fun Step2ClearComparison() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            ResponsiveText(
                text = "Compare Plans",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            ResponsiveText(
                text = "Choose the right tier for your digital wellness journey.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }

        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ResponsiveText(text = "Feature", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.weight(1.5f))
                    ResponsiveText(text = "Free", style = MaterialTheme.typography.labelSmall, color = TextMuted, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f))
                    ResponsiveText(text = "Monthly", style = MaterialTheme.typography.labelSmall, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.weight(0.9f))
                    ResponsiveText(text = "Yearly (Pro)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = FrictionPrimary, textAlign = TextAlign.Center, modifier = Modifier.weight(1.1f))
                }
                Divider(color = Color.White.copy(alpha = 0.08f))
                ComparisonTableRow3("Ad-Free Experience", "Ad-supported", "100% Ad-Free", "100% Ad-Free")
                ComparisonTableRow3("App Limits", "Max 2", "Unlimited", "Unlimited")
                ComparisonTableRow3("Challenges", "Typing & Math", "All Challenges", "All Challenges")
                ComparisonTableRow3("Box Breathing", "Locked", "Available", "Custom Cycles")
                ComparisonTableRow3("AI Analyse", "Locked", "Available", "Priority AI")
                ComparisonTableRow3("Smart Analytics", "Basic", "Full", "Full + Trends")
                ComparisonTableRow3("Streak Savers", "4 / Month", "4 / Month", "4 / Month")
            }
        }
    }
}

@Composable
private fun ComparisonTableRow3(feature: String, freeVal: String, monthlyVal: String, yearlyVal: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ResponsiveText(text = feature, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1.5f))
        ResponsiveText(text = freeVal, style = MaterialTheme.typography.labelSmall, color = TextMuted, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f))
        ResponsiveText(text = monthlyVal, style = MaterialTheme.typography.labelSmall, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.weight(0.9f))
        ResponsiveText(text = yearlyVal, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = FrictionPrimary, textAlign = TextAlign.Center, modifier = Modifier.weight(1.1f))
    }
}

@Composable
private fun Step3PlanSelectionAndPurchase(selectedPlan: PlanType, onSelectPlan: (PlanType) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            ResponsiveText(text = "Select Your Plan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            ResponsiveText(
                text = "Includes 3-day free trial. Instant activation, cancel anytime.",
                style = MaterialTheme.typography.bodySmall,
                color = FrictionAccent,
                textAlign = TextAlign.Center
            )
        }

        PricingCard("Yearly Pro", "₹549 / mo", "Billed annually at ₹6,588/yr", "BEST VALUE • SAVE 15%", true, selectedPlan == PlanType.YEARLY) { onSelectPlan(PlanType.YEARLY) }
        PricingCard("Monthly Pro", "₹649 / mo", "Flexible monthly subscription", "PAY AS YOU GO", false, selectedPlan == PlanType.MONTHLY) { onSelectPlan(PlanType.MONTHLY) }
        PricingCard("Lifetime Access", "₹12,000", "One-time payment forever", "OWN FOREVER", false, selectedPlan == PlanType.LIFETIME) { onSelectPlan(PlanType.LIFETIME) }

        // Trust & Security Badges Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = Color.White.copy(alpha = 0.03f),
            borderColor = Color.White.copy(alpha = 0.08f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrustBadgeItem(
                    icon = Icons.Default.Lock,
                    label = "Encrypted",
                    modifier = Modifier.padding(end = 8.dp)
                )
                TrustBadgeItem(
                    icon = Icons.Default.VerifiedUser,
                    label = "Google Play",
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                TrustBadgeItem(
                    icon = Icons.Default.CheckCircle,
                    label = "Cancel Anytime",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun TrustBadgeItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = FrictionAccent, modifier = Modifier.size(14.dp))
        ResponsiveText(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PricingCard(
    title: String,
    price: String,
    details: String,
    badgeText: String,
    isBestValue: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) FrictionPrimary else Color.White.copy(alpha = 0.08f)
    val containerBg = if (isSelected) FrictionPrimary.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.03f)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = containerBg,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ResponsiveText(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    if (isBestValue) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(FrictionAccent).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            ResponsiveText(text = "POPULAR", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
                if (badgeText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    ResponsiveText(text = badgeText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = FrictionAccent)
                }
                Spacer(modifier = Modifier.height(2.dp))
                ResponsiveText(text = details, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                ResponsiveText(text = price, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = FrictionPrimary)
                RadioButton(selected = isSelected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = FrictionPrimary, unselectedColor = TextMuted))
            }
        }
    }
}

