package com.example.features.paywall

import com.example.core.widgets.ResponsiveText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.core.widgets.FrictionButton
import com.example.core.widgets.GlassCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockedFeatureSheet(
    featureTitle: String,
    featureDescription: String,
    icon: ImageVector = Icons.Default.Lock,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(FrictionPrimary.copy(alpha = 0.2f), FrictionAccent.copy(alpha = 0.2f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = FrictionPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ResponsiveText(
                    text = featureTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                ResponsiveText(
                    text = featureDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = FrictionAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    ResponsiveText(
                        text = "Unlock 100% ad-free experience, all 5 friction blockers, unlimited app limits, and AI coaching.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FrictionButton(
                    text = "Upgrade to Premium",
                    onClick = onUpgrade,
                    modifier = Modifier.fillMaxWidth()
                )

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ResponsiveText(
                        text = "Not Now",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
