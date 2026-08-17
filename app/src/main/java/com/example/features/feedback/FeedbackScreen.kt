package com.example.features.feedback

import com.example.core.widgets.ResponsiveText
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.widgets.BackgroundStyle
import com.example.core.widgets.FrictionButton
import com.example.core.widgets.FrictionSpinner
import com.example.core.widgets.PremiumBackground
import com.example.data.model.User
import com.example.data.repository.FeedbackRepository
import com.example.data.repository.FeedbackSubmission
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    user: User,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val feedbackRepository = remember { FeedbackRepository() }

    // State for expanded card sections (1 = Bug, 2 = Feature, 3 = General Feedback)
    var expandedSection by remember { mutableStateOf<Int?>(3) } // default General Feedback expanded

    // Bug report form state
    var bugSubject by remember { mutableStateOf("") }
    var bugDescription by remember { mutableStateOf("") }

    // Feature request form state
    var featureTitle by remember { mutableStateOf("") }
    var featureDescription by remember { mutableStateOf("") }
    var featureWhyHelpful by remember { mutableStateOf("") }

    // General feedback form state
    var starRating by remember { mutableStateOf(5) }
    var feedbackText by remember { mutableStateOf("") }
    var feedbackSuggestion by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        PremiumBackground(style = BackgroundStyle.ANALYTICS)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header with Back Button and Title
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

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ResponsiveText(
                        text = "Feedback & Support",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    ResponsiveText(
                        text = "Help us build a better Friction",
                        style = MaterialTheme.typography.labelMedium,
                        color = FrictionSecondaryText
                    )
                }

                // Mascot Avatar Icon removed per user request
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 1. Bug Report Card
            ExpandableGlassCard(
                title = "Report a Bug",
                subtitle = "Found an unexpected issue or glitch?",
                icon = Icons.Default.BugReport,
                isExpanded = expandedSection == 1,
                onClick = {
                    expandedSection = if (expandedSection == 1) null else 1
                }
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    GlassTextField(
                        value = bugSubject,
                        onValueChange = { bugSubject = it },
                        label = "Subject",
                        placeholder = "e.g. App limit screen didn't unlock"
                    )

                    GlassTextField(
                        value = bugDescription,
                        onValueChange = { bugDescription = it },
                        label = "Description",
                        placeholder = "Explain what happened and steps to reproduce...",
                        singleLine = false,
                        minLines = 3
                    )

                    FrictionButton(
                        text = "Submit Bug Report",
                        onClick = {
                            if (bugSubject.isBlank() || bugDescription.isBlank()) {
                                Toast.makeText(context, "Please enter a subject and description", Toast.LENGTH_SHORT).show()
                                return@FrictionButton
                            }
                            val submission = FeedbackSubmission(
                                userId = user.uid.ifEmpty { "anonymous_user" },
                                authType = if (user.guest) "Guest" else "Google",
                                category = "bug_report",
                                subjectOrTitle = bugSubject,
                                description = bugDescription,
                                extraDetails = "Submitted via mobile app"
                            )
                            val success = feedbackRepository.sendEmailFeedback(context, submission)
                            if (success) {
                                successMessage = "Opening your email app to send the bug report to alokchoubey892@gmail.com."
                                showSuccessDialog = true
                                bugSubject = ""
                                bugDescription = ""
                            }
                        },
                        isLoading = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 2. Feature Request Card
            ExpandableGlassCard(
                title = "Request a Feature",
                subtitle = "Have an idea to make focus easier?",
                icon = Icons.Default.Lightbulb,
                isExpanded = expandedSection == 2,
                onClick = {
                    expandedSection = if (expandedSection == 2) null else 2
                }
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    GlassTextField(
                        value = featureTitle,
                        onValueChange = { featureTitle = it },
                        label = "Feature Title",
                        placeholder = "e.g. Scheduled Focus Zones for weekends"
                    )

                    GlassTextField(
                        value = featureDescription,
                        onValueChange = { featureDescription = it },
                        label = "Detailed Description",
                        placeholder = "Describe how this feature should work...",
                        singleLine = false,
                        minLines = 3
                    )

                    GlassTextField(
                        value = featureWhyHelpful,
                        onValueChange = { featureWhyHelpful = it },
                        label = "Why It Would Help",
                        placeholder = "e.g. Prevents mindless scrolling during study hours...",
                        singleLine = false,
                        minLines = 2
                    )

                    FrictionButton(
                        text = "Submit Feature Request",
                        onClick = {
                            if (featureTitle.isBlank() || featureDescription.isBlank()) {
                                Toast.makeText(context, "Please enter a feature title and description", Toast.LENGTH_SHORT).show()
                                return@FrictionButton
                            }
                            val submission = FeedbackSubmission(
                                userId = user.uid.ifEmpty { "anonymous_user" },
                                authType = if (user.guest) "Guest" else "Google",
                                category = "feature_request",
                                subjectOrTitle = featureTitle,
                                description = featureDescription,
                                extraDetails = featureWhyHelpful
                            )
                            val success = feedbackRepository.sendEmailFeedback(context, submission)
                            if (success) {
                                successMessage = "Opening your email app to send the feature request to alokchoubey892@gmail.com."
                                showSuccessDialog = true
                                featureTitle = ""
                                featureDescription = ""
                                featureWhyHelpful = ""
                            }
                        },
                        isLoading = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 3. General Feedback Card
            ExpandableGlassCard(
                title = "General Feedback",
                subtitle = "Rate your experience & share your thoughts",
                icon = Icons.Default.RateReview,
                isExpanded = expandedSection == 3,
                onClick = {
                    expandedSection = if (expandedSection == 3) null else 3
                }
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    // Star Rating Picker
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        ResponsiveText(
                            text = "How are you enjoying Friction?",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            (1..5).forEach { star ->
                                val isSelected = star <= starRating
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "$star Stars",
                                    tint = if (isSelected) Color(0xFFFFB800) else TextMuted,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable { starRating = star }
                                )
                            }
                        }
                    }

                    GlassTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        label = "Your Thoughts",
                        placeholder = "What do you love? What can we improve?",
                        singleLine = false,
                        minLines = 3
                    )

                    GlassTextField(
                        value = feedbackSuggestion,
                        onValueChange = { feedbackSuggestion = it },
                        label = "Compliment / Suggestion (Optional)",
                        placeholder = "A quick note for Flick...",
                        singleLine = true
                    )

                    FrictionButton(
                        text = "Send Feedback",
                        onClick = {
                            if (feedbackText.isBlank()) {
                                Toast.makeText(context, "Please share your thoughts before submitting", Toast.LENGTH_SHORT).show()
                                return@FrictionButton
                            }
                            val submission = FeedbackSubmission(
                                userId = user.uid.ifEmpty { "anonymous_user" },
                                authType = if (user.guest) "Guest" else "Google",
                                category = "feedback",
                                subjectOrTitle = "$starRating Star Rating",
                                description = feedbackText,
                                starRating = starRating,
                                extraDetails = feedbackSuggestion
                            )
                            val success = feedbackRepository.sendEmailFeedback(context, submission)
                            if (success) {
                                successMessage = "Opening your email app to send your feedback to alokchoubey892@gmail.com."
                                showSuccessDialog = true
                                feedbackText = ""
                                feedbackSuggestion = ""
                            }
                        },
                        isLoading = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Success Dialog
        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showSuccessDialog = false },
                shape = RoundedCornerShape(24.dp),
                containerColor = DarkSurface,
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Mascot removed per user request
                        Spacer(modifier = Modifier.height(12.dp))
                        ResponsiveText(
                            text = "Submission Received!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                text = {
                    ResponsiveText(
                        text = successMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    FrictionButton(
                        text = "Done",
                        onClick = { showSuccessDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
        }
    }
}

@Composable
fun ExpandableGlassCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = DarkSurface.copy(alpha = 0.88f),
        border = BorderStroke(
            1.dp,
            if (isExpanded) FrictionPrimary.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f)
        ),
        shadowElevation = if (isExpanded) 12.dp else 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isExpanded) FrictionPrimary.copy(alpha = 0.18f) else DarkBackground.copy(alpha = 0.6f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = if (isExpanded) FrictionPrimary else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        ResponsiveText(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        ResponsiveText(
                            text = subtitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = FrictionSecondaryText
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = TextMuted
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                content()
            }
        }
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ResponsiveText(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                ResponsiveText(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            },
            singleLine = singleLine,
            minLines = minLines,
            maxLines = if (singleLine) 1 else 6,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkBackground.copy(alpha = 0.7f),
                unfocusedContainerColor = DarkBackground.copy(alpha = 0.5f),
                focusedBorderColor = FrictionPrimary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
