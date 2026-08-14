package com.example.features.permission

import com.example.core.widgets.ResponsiveText
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.widgets.FrictionButton
import com.example.ui.theme.*

@Composable
fun UsagePermissionScreen(
    onCheckPermissionAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            
            // 1. Core Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                // Beautiful glowing radial halo behind lock icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(FrictionPrimary.copy(alpha = 0.15f), DarkSurface)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = FrictionPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                ResponsiveText(
                    text = "Screen Time Access",
                    style = MaterialTheme.typography.displayMedium,
                    color = FrictionText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                ResponsiveText(
                    text = "To help you break mindless scrolling habits, Friction needs to analyze which apps are taking your attention.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = FrictionSecondaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            // 2. Clear Benefits List (Duolingo Style Cards)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PermissionValueRow(
                    icon = Icons.Outlined.Timeline,
                    title = "Accurate Screen Time Metrics",
                    desc = "Compute precise minute-by-minute stats for your customized attention blocks."
                )

                PermissionValueRow(
                    icon = Icons.Outlined.CheckCircle,
                    title = "Habit Loop Interruption",
                    desc = "Activate gentle delays and focus exercises when you open distracting apps."
                )

                PermissionValueRow(
                    icon = Icons.Outlined.Lock,
                    title = "100% Private & Local",
                    desc = "All calculations happen entirely on your device. We never sell or share your data."
                )
            }

            // 3. Action Call-To-Action Footer
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FrictionButton(
                    text = "Grant Usage Access",
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Safe fallback in case intent fails on custom Android forks
                            try {
                                val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(fallbackIntent)
                            } catch (err: Exception) {
                                // Ignore
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Check Again Link Button
                TextButton(
                    onClick = onCheckPermissionAgain,
                    colors = ButtonDefaults.textButtonColors(contentColor = FrictionPrimary)
                ) {
                    ResponsiveText(
                        text = "I have granted access. Check status.",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionValueRow(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x10FFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x10FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = FrictionPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                ResponsiveText(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = FrictionText
                )
                Spacer(modifier = Modifier.height(2.dp))
                ResponsiveText(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = FrictionSecondaryText,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
