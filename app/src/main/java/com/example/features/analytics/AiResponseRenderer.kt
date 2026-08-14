package com.example.features.analytics

import com.example.core.widgets.ResponsiveText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun AiResponseRenderer(
    responseContent: String,
    onSetLimit: () -> Unit = {},
    onOpenEngine: () -> Unit = {},
    onClassifyApps: () -> Unit = {},
    onReviewGoal: () -> Unit = {},
    onViewAnalytics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lines = responseContent.split("\n")

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        var currentSectionTitle = ""

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue

            when {
                // Section Heading (#, ##, ###)
                trimmed.startsWith("#") -> {
                    val title = trimmed.replace("#", "").trim()
                    currentSectionTitle = title
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FrictionPrimary,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }

                // Action Triggers: [ACTION:TYPE|Label]
                trimmed.contains("[ACTION:") -> {
                    RenderActionRow(
                        line = trimmed,
                        onSetLimit = onSetLimit,
                        onOpenEngine = onOpenEngine,
                        onClassifyApps = onClassifyApps,
                        onReviewGoal = onReviewGoal,
                        onViewAnalytics = onViewAnalytics
                    )
                }

                // Bullet point
                trimmed.startsWith("•") || trimmed.startsWith("-") || trimmed.startsWith("*") -> {
                    val content = trimmed.substring(1).trim()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp, end = 10.dp)
                                .size(6.dp)
                                .background(FrictionAccent, shape = RoundedCornerShape(3.dp))
                        )
                        Text(
                            text = parseFormattedMarkdown(content),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            lineHeight = 22.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Standard paragraph
                else -> {
                    Text(
                        text = parseFormattedMarkdown(trimmed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RenderActionRow(
    line: String,
    onSetLimit: () -> Unit,
    onOpenEngine: () -> Unit,
    onClassifyApps: () -> Unit,
    onReviewGoal: () -> Unit,
    onViewAnalytics: () -> Unit
) {
    // Regex matches [ACTION:TYPE|Label]
    val regex = Regex("\\[ACTION:([A-Z_]+)\\|([^]]+)\\]")
    val matches = regex.findAll(line).toList()

    // Render plain text before/between action tags if present
    val plainText = line.replace(regex, "").trim()
    if (plainText.isNotBlank()) {
        Text(
            text = parseFormattedMarkdown(plainText),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (match in matches) {
            val actionType = match.groupValues[1]
            val label = match.groupValues[2]

            when (actionType) {
                "SET_LIMIT" -> {
                    Button(
                        onClick = onSetLimit,
                        colors = ButtonDefaults.buttonColors(containerColor = FrictionPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = label, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                "OPEN_ENGINE" -> {
                    OutlinedButton(
                        onClick = onOpenEngine,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FrictionAccent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
                "CLASSIFY_APPS" -> {
                    OutlinedButton(
                        onClick = onClassifyApps,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
                "REVIEW_GOAL" -> {
                    Button(
                        onClick = onReviewGoal,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = FrictionPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = label, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
                else -> {
                    Button(
                        onClick = onViewAnalytics,
                        colors = ButtonDefaults.buttonColors(containerColor = FrictionPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = label, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/**
 * Replaces Markdown bold syntax (**text**) with bold Compose AnnotatedString styles.
 */
private fun parseFormattedMarkdown(raw: String): androidx.compose.ui.text.AnnotatedString {
    val clean = raw.replace("`", "").replace("###", "").replace("##", "")
    val parts = clean.split("**")
    return buildAnnotatedString {
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) {
                    append(part)
                }
            } else {
                append(part)
            }
        }
    }
}
