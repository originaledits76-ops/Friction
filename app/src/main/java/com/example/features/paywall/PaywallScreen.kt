package com.example.features.paywall

import com.example.data.model.User
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.theme.*

enum class PlanType {
    MONTHLY, YEARLY, LIFETIME
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    user: User? = null,
    onLinkGoogle: () -> Unit = {},
    onDismiss: () -> Unit,
    onPurchaseSuccess: (PlanType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableStateOf(1) }
    var selectedPlan by remember { mutableStateOf(PlanType.YEARLY) }
    var isProcessing by remember { mutableStateOf(false) }
    var showGuestDialog by remember { mutableStateOf(false) }

    if (showGuestDialog) {
        AlertDialog(
            onDismissRequest = { showGuestDialog = false },
            title = {
                Text(
                    text = "Google Account Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
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
                    Text("Cancel", color = TextMuted)
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
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
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
                        Text(
                            text = "Step $currentStep/3",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = FrictionPrimary
                        )
                    }
                }

                TextButton(
                    onClick = {
                        onPurchaseSuccess(selectedPlan)
                    }
                ) {
                    Text(
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
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (step in 1..3) {
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
                        1 -> Step1FeaturesView()
                        2 -> Step2PlanComparisonView()
                        3 -> Step3PlanSelectionView(
                            selectedPlan = selectedPlan,
                            onSelectPlan = { selectedPlan = it }
                        )
                    }
                }
            }

            // Bottom Navigation Action Button
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FrictionButton(
                    text = when (currentStep) {
                        1 -> "Continue to Comparison"
                        2 -> "Continue to Select Plan"
                        else -> if (isProcessing) "Processing..." else "Continue to Checkout"
                    },
                    onClick = {
                        if (currentStep < 3) {
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

                Text(
                    text = if (currentStep == 3) "Cancel anytime. Safe & secure payment." else "Step $currentStep of 3 • Cancel anytime",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun Step1FeaturesView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Graphic & Title
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = R.drawable.mascot_premium),
                contentDescription = "Flick Premium Mascot",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(110.dp)
            )

            Text(
                text = "Unlock Your Full Focus",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Discover all the powerful capabilities designed to eliminate digital distractions.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }

        // Section: Included for Everyone (Free)
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            borderColor = Color.White.copy(alpha = 0.1f)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Free",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Included for Everyone (Free)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }

                FeatureItemRow(
                    icon = Icons.Default.Lock,
                    title = "Basic App Usage Limits",
                    subtitle = "Set up to 3 custom app rules with basic barriers",
                    isPremium = false
                )

                FeatureItemRow(
                    icon = Icons.Default.Calculate,
                    title = "Math & Typing Challenges",
                    subtitle = "Standard mental challenges before opening restricted apps",
                    isPremium = false
                )

                FeatureItemRow(
                    icon = Icons.Default.BarChart,
                    title = "Daily Screentime Overview",
                    subtitle = "Basic daily usage tracking and unlock counters",
                    isPremium = false
                )
            }
        }

        // Section: Premium Features
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            borderColor = FrictionPrimary.copy(alpha = 0.4f),
            backgroundColor = FrictionPrimary.copy(alpha = 0.08f)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Premium",
                        tint = FrictionAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Premium Features (Unlocked)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = FrictionAccent
                    )
                }

                FeatureItemRow(
                    icon = Icons.Default.AllInclusive,
                    title = "Unlimited App Rules & Barriers",
                    subtitle = "Block as many distraction apps as you want with no limits",
                    isPremium = true
                )

                FeatureItemRow(
                    icon = Icons.Default.FitnessCenter,
                    title = "All 5 Friction Challenges",
                    subtitle = "Physical Push-ups AI, Find Object Scanner, Walk 100m, Math & Typing",
                    isPremium = true
                )

                FeatureItemRow(
                    icon = Icons.Default.CenterFocusStrong,
                    title = "Custom Physical Object Scanner",
                    subtitle = "Configure up to 5 personal physical objects for AI camera verification",
                    isPremium = true
                )

                FeatureItemRow(
                    icon = Icons.Default.Psychology,
                    title = "AI Attention Coaching & Analytics",
                    subtitle = "Deep habit analysis, unlock patterns, and personalized insights",
                    isPremium = true
                )

                FeatureItemRow(
                    icon = Icons.Default.Leaderboard,
                    title = "Social Leaderboards & Friends",
                    subtitle = "Compete with friends on weekly and monthly focus ranks",
                    isPremium = true
                )

                FeatureItemRow(
                    icon = Icons.Default.Shield,
                    title = "Active App Blocker Service",
                    subtitle = "Instant overlay barrier whenever blocked apps are launched",
                    isPremium = true
                )
            }
        }
    }
}

@Composable
private fun Step2PlanComparisonView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Plan Comparison",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Compare features across Free, Monthly, Yearly, and Lifetime access.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Table Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Feature",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        modifier = Modifier.weight(1.8f)
                    )
                    Text(
                        text = "Free",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Paid",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = FrictionPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1.2f)
                    )
                }

                Divider(color = Color.White.copy(alpha = 0.08f))

                ComparisonTableRow(
                    feature = "App Usage Rules",
                    freeValue = "Up to 3",
                    paidValue = "Unlimited",
                    isHighlighted = true
                )

                ComparisonTableRow(
                    feature = "Friction Challenges",
                    freeValue = "2 Types",
                    paidValue = "All 5 Types",
                    isHighlighted = true
                )

                ComparisonTableRow(
                    feature = "Push-ups AI Camera",
                    freeValue = "—",
                    paidValue = "Included",
                    isHighlighted = true
                )

                ComparisonTableRow(
                    feature = "Find Object AI",
                    freeValue = "—",
                    paidValue = "5 Custom",
                    isHighlighted = true
                )

                ComparisonTableRow(
                    feature = "AI Habit Insights",
                    freeValue = "—",
                    paidValue = "Full Access",
                    isHighlighted = true
                )

                ComparisonTableRow(
                    feature = "Leaderboards & Friends",
                    freeValue = "Basic",
                    paidValue = "Full Access",
                    isHighlighted = false
                )

                ComparisonTableRow(
                    feature = "Active App Blocker",
                    freeValue = "Standard",
                    paidValue = "Priority",
                    isHighlighted = false
                )
            }
        }

        // Highlight Best Value Box
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = FrictionAccent.copy(alpha = 0.12f),
            borderColor = FrictionAccent.copy(alpha = 0.4f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Best Value",
                    tint = FrictionAccent,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "Yearly Plan is Best Value",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Save 25% with our launch offer. Includes 3-day free trial.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun Step3PlanSelectionView(
    selectedPlan: PlanType,
    onSelectPlan: (PlanType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Select Your Plan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Choose the plan that fits your goals best. Cancel anytime.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Yearly Plan (Highlighted Best Value)
            PricingCard(
                title = "Yearly Plan",
                strikethroughPrice = "",
                discountPrice = "₹486 / month",
                billingDetails = "Billed annually at ₹5,832/yr",
                badgeText = "Launch Offer – Save 25%",
                isBestValue = true,
                isSelected = selectedPlan == PlanType.YEARLY,
                onClick = { onSelectPlan(PlanType.YEARLY) }
            )

            // Monthly Plan
            PricingCard(
                title = "Monthly Plan",
                strikethroughPrice = "₹799",
                discountPrice = "₹649 / month",
                billingDetails = "Flexible monthly subscription",
                badgeText = "",
                isBestValue = false,
                isSelected = selectedPlan == PlanType.MONTHLY,
                onClick = { onSelectPlan(PlanType.MONTHLY) }
            )

            // Lifetime Plan
            PricingCard(
                title = "Lifetime License",
                strikethroughPrice = "",
                discountPrice = "₹8,999",
                billingDetails = "One-time payment • Own forever",
                badgeText = "Launch Offer – Save 25%",
                isBestValue = false,
                isSelected = selectedPlan == PlanType.LIFETIME,
                onClick = { onSelectPlan(PlanType.LIFETIME) }
            )
        }
    }
}

@Composable
private fun FeatureItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isPremium: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (isPremium) FrictionAccent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isPremium) FrictionAccent else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ComparisonTableRow(
    feature: String,
    freeValue: String,
    paidValue: String,
    isHighlighted: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = feature,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            modifier = Modifier.weight(1.8f)
        )
        Text(
            text = freeValue,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = paidValue,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlighted) FrictionPrimary else TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1.2f)
        )
    }
}

@Composable
private fun PricingCard(
    title: String,
    strikethroughPrice: String,
    discountPrice: String,
    billingDetails: String,
    badgeText: String,
    isBestValue: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) FrictionPrimary else Color.White.copy(alpha = 0.08f)
    val containerBg = if (isSelected) FrictionPrimary.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.03f)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = containerBg,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (isBestValue) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(FrictionAccent)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "BEST VALUE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                if (badgeText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = FrictionAccent,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = billingDetails,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                if (strikethroughPrice.isNotEmpty()) {
                    Text(
                        text = strikethroughPrice,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        textDecoration = TextDecoration.LineThrough
                    )
                }

                Text(
                    text = discountPrice,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FrictionPrimary
                )

                RadioButton(
                    selected = isSelected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = FrictionPrimary,
                        unselectedColor = TextMuted
                    )
                )
            }
        }
    }
}
