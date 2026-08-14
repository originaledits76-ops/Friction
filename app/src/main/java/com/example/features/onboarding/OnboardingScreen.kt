package com.example.features.onboarding

import com.example.core.widgets.ResponsiveText
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class GoalOption(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val badge: String? = null
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    initialName: String = "",
    onComplete: (name: String, age: Int, goal: String, customGoal: String, motivation: String) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }

    var name by remember { mutableStateOf(initialName.ifEmpty { "Guest Companion" }) }
    var ageStr by remember { mutableStateOf("22") }
    var selectedGoalTitle by remember { mutableStateOf("") }
    var customGoal by remember { mutableStateOf("") }
    var motivation by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current

    val goalOptions = remember {
        listOf(
            GoalOption("Study & Academic Focus", "Ace exams & deep study sessions", Icons.Default.School, "Popular"),
            GoalOption("Reduce Screen Time", "Cut mindless daily phone usage", Icons.Default.PhoneAndroid, "Top Goal"),
            GoalOption("Digital Detox", "Unplug & reconnect with real life", Icons.Default.Park),
            GoalOption("Boost Productivity", "Get high-value work done faster", Icons.Default.Speed),
            GoalOption("Cut Social Media", "Stop endless Instagram & short video reels", Icons.Default.Public),
            GoalOption("Improve Sleep", "Eliminate late-night screen scrolling", Icons.Default.NightsStay),
            GoalOption("Read & Learn", "Build a consistent daily reading habit", Icons.AutoMirrored.Filled.MenuBook),
            GoalOption("Mindfulness & Peace", "Reduce anxiety and stay present", Icons.Default.SelfImprovement),
            GoalOption("Other", "Define your custom personal goal", Icons.Default.Edit)
        )
    }

    val motivationIdeas = remember {
        listOf(
            "Crack competitive exams 🎓",
            "Reclaim 2+ hours every day ⏰",
            "Be present with family & friends 💖",
            "Improve sleep quality & mental clarity 🧠",
            "Focus on health & workout routines 🏃"
        )
    }

    val ageChips = listOf("18", "21", "25", "30", "35", "40")

    val isStep1Valid = name.trim().length >= 2 && ageStr.toIntOrNull()?.let { it in 10..120 } == true
    val isStep2Valid = selectedGoalTitle.isNotBlank() && (selectedGoalTitle != "Other" || customGoal.trim().isNotBlank())
    val isStep3Valid = motivation.trim().isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .drawBehind {
                // Dual-tone atmospheric background radial glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(FrictionPrimary.copy(alpha = 0.08f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(size.width, 0f),
                        radius = size.width * 0.9f
                    ),
                    radius = size.width * 0.9f,
                    center = androidx.compose.ui.geometry.Offset(size.width, 0f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(FrictionAccent.copy(alpha = 0.04f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(0f, size.height),
                        radius = size.width * 0.8f
                    ),
                    radius = size.width * 0.8f,
                    center = androidx.compose.ui.geometry.Offset(0f, size.height)
                )
            }
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Navigation Bar & Beautiful Segmented Stepper
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step > 1) {
                        IconButton(
                            onClick = { step-- },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(DarkSurface)
                                .border(1.dp, Color(0x10FFFFFF), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(44.dp))
                    }

                    // Glassmorphic Step Indicator Pill
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = FrictionPrimary.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, FrictionPrimary.copy(alpha = 0.25f)),
                        modifier = Modifier.shadow(4.dp, RoundedCornerShape(24.dp))
                    ) {
                        ResponsiveText(
                            text = "STEP $step OF 3",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = FrictionPrimary,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.size(44.dp))
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Smooth Glowing Progress Segment
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (i in 1..3) {
                        val isCurrentOrPast = i <= step
                        val barBrush = if (isCurrentOrPast) {
                            Brush.linearGradient(listOf(FrictionPrimary, FrictionSecondary))
                        } else {
                            Brush.linearGradient(listOf(DarkSurface, DarkSurface))
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(barBrush)
                        )
                    }
                }
            }

            // Main Form Content Container with Animated Transition
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { width -> width } + fadeIn() togetherWith
                                    slideOutHorizontally { width -> -width } + fadeOut()
                        } else {
                            slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                    slideOutHorizontally { width -> width } + fadeOut()
                        }
                    },
                    label = "OnboardingStepTransition"
                ) { currentStep ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        when (currentStep) {
                            1 -> {
                                // STEP 1: Personal Details
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(FrictionPrimary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Spa,
                                            contentDescription = null,
                                            tint = FrictionPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    ResponsiveText(
                                        text = "Friction Profile",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = FrictionPrimary,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                ResponsiveText(
                                    text = "Claim your identity",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                ResponsiveText(
                                    text = "Friction is tailored around you. Let's start with basic setup to calibrate your offline challenge modules.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    lineHeight = 22.sp
                                )

                                Spacer(modifier = Modifier.height(32.dp))

                                // Name Input Field
                                ResponsiveText(
                                    text = "YOUR FULL NAME",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    placeholder = { ResponsiveText("e.g. Alex Rivera", color = TextMuted) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (name.isNotBlank()) FrictionPrimary else TextMuted
                                        )
                                    },
                                    trailingIcon = {
                                        if (name.trim().length >= 2) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Valid",
                                                tint = FrictionPrimary
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(20.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = DarkSurface,
                                        unfocusedContainerColor = DarkSurface,
                                        focusedBorderColor = FrictionPrimary,
                                        unfocusedBorderColor = Color(0x10FFFFFF),
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                // Age Input Field
                                ResponsiveText(
                                    text = "YOUR AGE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = ageStr,
                                    onValueChange = { ageStr = it.filter { char -> char.isDigit() }.take(3) },
                                    placeholder = { ResponsiveText("e.g. 24", color = TextMuted) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Cake,
                                            contentDescription = null,
                                            tint = if (ageStr.isNotBlank()) FrictionPrimary else TextMuted
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(20.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = DarkSurface,
                                        unfocusedContainerColor = DarkSurface,
                                        focusedBorderColor = FrictionPrimary,
                                        unfocusedBorderColor = Color(0x10FFFFFF),
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Quick Age Selector Chips
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    ResponsiveText(
                                        text = "Quick Select:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted,
                                        fontWeight = FontWeight.Medium
                                    )
                                    ageChips.forEach { chipAge ->
                                        val isSelected = ageStr == chipAge
                                        Surface(
                                            onClick = { ageStr = chipAge },
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isSelected) FrictionPrimary.copy(alpha = 0.15f) else DarkSurface,
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSelected) FrictionPrimary else Color(0x10FFFFFF)
                                            )
                                        ) {
                                            ResponsiveText(
                                                text = chipAge,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) FrictionPrimary else TextSecondary,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(36.dp))

                                // Dynamic Premium Focus Card Preview
                                if (name.isNotBlank()) {
                                    Card(
                                        shape = RoundedCornerShape(24.dp),
                                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                        border = BorderStroke(1.dp, FrictionPrimary.copy(alpha = 0.25f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .shadow(8.dp, RoundedCornerShape(24.dp))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(20.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        Brush.linearGradient(
                                                            colors = listOf(FrictionPrimary, FrictionSecondary)
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                ResponsiveText(
                                                    text = name.take(1).uppercase(),
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    ResponsiveText(
                                                        text = name.trim(),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimary
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = FrictionAccent.copy(alpha = 0.15f)
                                                    ) {
                                                        ResponsiveText(
                                                            text = "LEVEL 1 FOCUSED",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = FrictionAccent,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 9.sp,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                ResponsiveText(
                                                    text = if (ageStr.isNotBlank()) "Age: $ageStr • Ready to cultivate mindfulness." else "Focus Neophyte",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            2 -> {
                                // STEP 2: Goal Selection
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(FrictionPrimary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WorkspacePremium,
                                            contentDescription = null,
                                            tint = FrictionPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    ResponsiveText(
                                        text = "Intentionality Matrix",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = FrictionPrimary,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                ResponsiveText(
                                    text = "Your main focus goal",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                ResponsiveText(
                                    text = "Choose the core area you want Friction to monitor and guard against mindless dopamine loops.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    lineHeight = 22.sp
                                )

                                Spacer(modifier = Modifier.height(28.dp))

                                // Goal Selection List Cards with Premium Selection Ring
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    goalOptions.forEach { option ->
                                        val isSelected = selectedGoalTitle == option.title
                                        Card(
                                            onClick = { selectedGoalTitle = option.title },
                                            shape = RoundedCornerShape(20.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) DarkElevated else DarkSurface
                                            ),
                                            border = BorderStroke(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) FrictionPrimary else Color(0x10FFFFFF)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .shadow(
                                                    elevation = if (isSelected) 6.dp else 0.dp,
                                                    shape = RoundedCornerShape(20.dp),
                                                    spotColor = FrictionPrimary
                                                )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(18.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(46.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(
                                                            if (isSelected) FrictionPrimary.copy(alpha = 0.18f)
                                                            else Color(0x08FFFFFF)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = option.icon,
                                                        contentDescription = null,
                                                        tint = if (isSelected) FrictionPrimary else TextSecondary,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(16.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        ResponsiveText(
                                                            text = option.title,
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextPrimary
                                                        )
                                                        if (option.badge != null) {
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Surface(
                                                                shape = RoundedCornerShape(8.dp),
                                                                color = FrictionAccent.copy(alpha = 0.15f)
                                                            ) {
                                                                ResponsiveText(
                                                                    text = option.badge,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = FrictionAccent,
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    ResponsiveText(
                                                        text = option.description,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = TextSecondary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = { selectedGoalTitle = option.title },
                                                    colors = RadioButtonDefaults.colors(
                                                        selectedColor = FrictionPrimary,
                                                        unselectedColor = TextMuted
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                // Custom Goal Input Field if "Other" is selected
                                AnimatedVisibility(
                                    visible = selectedGoalTitle == "Other",
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(modifier = Modifier.padding(top = 20.dp)) {
                                        ResponsiveText(
                                            text = "DESCRIBE YOUR CUSTOM GOAL",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = customGoal,
                                            onValueChange = { customGoal = it },
                                            placeholder = { ResponsiveText("e.g. Master piano practice without distractions", color = TextMuted) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(20.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = DarkSurface,
                                                unfocusedContainerColor = DarkSurface,
                                                focusedBorderColor = FrictionPrimary,
                                                unfocusedBorderColor = Color(0x10FFFFFF),
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            3 -> {
                                // STEP 3: Motivation & Drive
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(FrictionAccent.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = FrictionAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    ResponsiveText(
                                        text = "Mindfulness Anchor",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = FrictionAccent,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                ResponsiveText(
                                    text = "Define your 'Why'",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                ResponsiveText(
                                    text = "Your sacred focus why will be displayed during delay counters to act as a psychological barrier and break instant scrolling.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    lineHeight = 22.sp
                                )

                                Spacer(modifier = Modifier.height(28.dp))

                                // Premium Motivation Text Card with Quote Symbol
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(DarkSurface)
                                        .border(1.dp, Color(0x10FFFFFF), RoundedCornerShape(24.dp))
                                        .padding(4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FormatQuote,
                                                contentDescription = null,
                                                tint = FrictionAccent.copy(alpha = 0.3f),
                                                modifier = Modifier.size(36.dp)
                                            )
                                            ResponsiveText(
                                                text = "${motivation.length}/180",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (motivation.length > 180) FrictionError else TextMuted,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        OutlinedTextField(
                                            value = motivation,
                                            onValueChange = { if (it.length <= 180) motivation = it },
                                            placeholder = {
                                                ResponsiveText(
                                                    text = "I want to reclaim my life, focus on my exam, and be present with those who matter to me...",
                                                    color = TextMuted,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            },
                                            shape = RoundedCornerShape(16.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedBorderColor = Color.Transparent,
                                                unfocusedBorderColor = Color.Transparent,
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary
                                            ),
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium,
                                                lineHeight = 22.sp
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(110.dp),
                                            maxLines = 4
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // Tap to Insert Suggestion Chips
                                ResponsiveText(
                                    text = "SUGGESTIONS (TAP TO INSERT)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    motivationIdeas.forEach { idea ->
                                        Surface(
                                            onClick = { motivation = idea },
                                            shape = RoundedCornerShape(16.dp),
                                            color = DarkSurface,
                                            border = BorderStroke(1.dp, Color(0x08FFFFFF)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = FrictionAccent,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                ResponsiveText(
                                                    text = idea,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TextPrimary
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(36.dp))

                                // Fully Completed "Focus Pact" Certificate
                                Card(
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                    border = BorderStroke(1.dp, FrictionAccent.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(8.dp, RoundedCornerShape(24.dp))
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            ResponsiveText(
                                                text = "THE FOCUS COVENANT",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = FrictionAccent,
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 1.5.sp
                                            )
                                            Icon(
                                                imageVector = Icons.Default.EmojiEvents,
                                                contentDescription = null,
                                                tint = FrictionAccent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))
                                        
                                        HorizontalDivider(color = Color(0x08FFFFFF))
                                        Spacer(modifier = Modifier.height(14.dp))

                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            ResponsiveText("Sovereign:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                            ResponsiveText(name.ifBlank { "Unsigned" }, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            ResponsiveText("Caliber:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                            ResponsiveText(if (ageStr.isNotBlank()) "$ageStr Winters" else "N/A", style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            ResponsiveText("Focus Vector:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                            ResponsiveText(
                                                text = if (selectedGoalTitle == "Other") customGoal.ifBlank { "Custom Vector" } else selectedGoalTitle.ifBlank { "Unspecified" },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(36.dp))
                    }
                }
            }

            // Bottom Sticky Beautiful Action Button
            Surface(
                color = DarkBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                val isCurrentValid = when (step) {
                    1 -> isStep1Valid
                    2 -> isStep2Valid
                    3 -> isStep3Valid
                    else -> false
                }

                Button(
                    onClick = {
                        if (step < 3) {
                            step++
                        } else {
                            val ageInt = ageStr.toIntOrNull() ?: 18
                            onComplete(name.trim(), ageInt, selectedGoalTitle, customGoal.trim(), motivation.trim())
                        }
                    },
                    enabled = isCurrentValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .shadow(
                            elevation = if (isCurrentValid) 8.dp else 0.dp,
                            shape = RoundedCornerShape(18.dp),
                            spotColor = FrictionPrimary
                        ),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FrictionPrimary,
                        disabledContainerColor = DarkElevated,
                        contentColor = Color(0xFF111315),
                        disabledContentColor = TextMuted
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        ResponsiveText(
                            text = if (step < 3) "Proceed" else "Seal the Pact",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = if (step < 3) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
