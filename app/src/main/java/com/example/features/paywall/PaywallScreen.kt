package com.example.features.paywall

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onDismiss: () -> Unit,
    onPurchaseSuccess: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedPlan by remember { mutableStateOf(PlanType.YEARLY) }
    var isProcessing by remember { mutableStateOf(false) }
    var showSuccessToast by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        PremiumBackground(style = BackgroundStyle.SETTINGS)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = TextPrimary
                    )
                }

                TextButton(
                    onClick = {
                        // Restore purchases action
                        onPurchaseSuccess()
                    }
                ) {
                    Text(
                        text = "Restore Purchases",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        textDecoration = TextDecoration.Underline
                    )
                }
            }

            // Hero Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.mascot_premium),
                    contentDescription = "Flick Premium Mascot",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(130.dp)
                )

                Text(
                    text = "Unlock Friction Premium",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Master your focus with unlimited limits, AI attention coaching, and custom friction challenges.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Pricing Selectors
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Yearly Plan (Best Value)
                PlanCard(
                    title = "Yearly Access",
                    price = "₹549 / month",
                    subtitle = "Billed annually at ₹6,588/yr",
                    isBestValue = true,
                    isSelected = selectedPlan == PlanType.YEARLY,
                    onClick = { selectedPlan = PlanType.YEARLY }
                )

                // Monthly Plan
                PlanCard(
                    title = "Monthly Access",
                    price = "₹649 / month",
                    subtitle = "Flexible monthly subscription",
                    isBestValue = false,
                    isSelected = selectedPlan == PlanType.MONTHLY,
                    onClick = { selectedPlan = PlanType.MONTHLY }
                )

                // Lifetime Plan
                PlanCard(
                    title = "Lifetime License",
                    price = "₹12,000",
                    subtitle = "One-time payment • Own forever",
                    isBestValue = false,
                    isSelected = selectedPlan == PlanType.LIFETIME,
                    onClick = { selectedPlan = PlanType.LIFETIME }
                )
            }

            // Feature Comparison Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "Plan Comparison",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                ComparisonRow(feature = "App Usage Limits", free = "Up to 3", monthly = "Unlimited", yearly = "Unlimited")
                ComparisonRow(feature = "Friction Blocker", free = "Basic", monthly = "All 5 Types", yearly = "All 5 Types")
                ComparisonRow(feature = "AI Attention Coach", free = "—", monthly = "Included", yearly = "Included")
                ComparisonRow(feature = "Buddy Leaderboards", free = "Weekly", monthly = "Full Access", yearly = "Full Access")
                ComparisonRow(feature = "Bonus: Themes & Avatars", free = "—", monthly = "—", yearly = "Included Perks")
            }

            // Action Button
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FrictionButton(
                    text = if (isProcessing) "Processing..." else "Continue to Checkout",
                    onClick = {
                        isProcessing = true
                        onPurchaseSuccess()
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Cancel anytime. Safe & secure payment.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    price: String,
    subtitle: String,
    isBestValue: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) FrictionPrimary else Color.White.copy(alpha = 0.08f)
    val containerBg = if (isSelected) FrictionPrimary.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.03f)

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
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = price,
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

@Composable
private fun ComparisonRow(
    feature: String,
    free: String,
    monthly: String,
    yearly: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = feature,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Free: $free",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Text(
                text = "Monthly: $monthly",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Text(
                text = "Yearly: $yearly",
                style = MaterialTheme.typography.labelSmall,
                color = FrictionAccent,
                fontWeight = FontWeight.Bold
            )
        }
        Divider(
            color = Color.White.copy(alpha = 0.05f),
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
