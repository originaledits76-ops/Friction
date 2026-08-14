import re

with open('./app/src/main/java/com/example/features/dashboard/DashboardScreen.kt', 'r') as f:
    content = f.read()

# Add import
if 'import com.example.core.widgets.ResponsiveText' not in content:
    content = content.replace('import com.example.core.widgets.PremiumBackground\n', 'import com.example.core.widgets.PremiumBackground\nimport com.example.core.widgets.ResponsiveText\n')

# Add Feedback Card before or after "Quick Actions / Features Grid" Row
feedback_card = """
            // FEEDBACK CARD (Req #5)
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToTab("feedback") },
                shape = RoundedCornerShape(24.dp),
                backgroundColor = DarkCardBg.copy(alpha = 0.85f),
                borderColor = Color.White.copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(FrictionSecondary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Feedback,
                                contentDescription = "Feedback",
                                tint = FrictionSecondary,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column {
                            ResponsiveText(
                                text = "Feedback & Support",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            ResponsiveText(
                                text = "Report bugs, request features, or share your thoughts.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Open Feedback",
                        tint = FrictionSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
"""

if 'FEEDBACK CARD' not in content:
    content = content.replace('// Quick Actions / Features Grid', feedback_card + '\n            // Quick Actions / Features Grid')

with open('./app/src/main/java/com/example/features/dashboard/DashboardScreen.kt', 'w') as f:
    f.write(content)
