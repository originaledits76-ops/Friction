package com.example.features.settings

import com.example.core.widgets.ResponsiveText
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.core.widgets.NeumorphicCard
import com.example.core.widgets.NeumorphicTextField
import com.example.core.widgets.FrictionButton
import com.example.data.model.FrictionRule
import com.example.data.model.User
import kotlinx.coroutines.launch
import kotlin.random.Random
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.sqrt

@Composable
fun ActiveBlockerScreen(
    user: User,
    rule: FrictionRule,
    isExpiredMode: Boolean = false,
    onComplete: (xp: Int, coins: Int, durationMinutes: Int) -> Unit,
    onSkip: (xpPenalty: Int, durationMinutes: Int) -> Unit,
    onCancel: () -> Unit
) {
    androidx.activity.compose.BackHandler(onBack = onCancel)

    val dims = LocalResponsiveDimensions.current
    var stage by remember { mutableStateOf(if (isExpiredMode) "EXPIRED" else "BLOCK") }
    var isSkipping by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val prefs = remember { context.getSharedPreferences("friction_progress_prefs", Context.MODE_PRIVATE) }
    val customObjects = remember {
        prefs.getString("custom_objects", "Water Bottle,Notebook,Backpack,Pen,Chair")
            ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?: listOf("Water Bottle", "Notebook", "Backpack", "Pen", "Chair")
    }

    val typeUpper = rule.challengeType.uppercase()
    val isTyping = typeUpper.contains("TYPING") || typeUpper.contains("KEY")
    val isMath = typeUpper.contains("MATH")
    val isBoxBreathing = typeUpper.contains("BREATH") || typeUpper.contains("BOX")
    val isParagraphSummary = typeUpper.contains("PARAGRAPH") || typeUpper.contains("SUMMARY")
    val isRememberPattern = typeUpper.contains("PATTERN") || typeUpper.contains("REMEMBER")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(dims.outerPadding),
        contentAlignment = Alignment.Center
    ) {
        com.example.core.widgets.PremiumBackground(com.example.core.widgets.BackgroundStyle.FRICTION_ENGINE)

        when (stage) {
            "EXPIRED" -> {
                ExpirationOverlayUI(
                    user = user,
                    appName = rule.targetAppName.ifBlank { "this app" },
                    onCloseApp = onCancel,
                    onOpenAnyway = { stage = "BLOCK" }
                )
            }
            "BLOCK" -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                ) {
                    NeumorphicCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = Color(0xFF191C20)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.challenge),
                                contentDescription = "Flick Challenge Mascot",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(110.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            ResponsiveText(
                                text = "${rule.targetAppName} is Limited",
                                fontSize = dims.titleLargeSize,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ResponsiveText(
                                text = "Take a mindful break before opening this application.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    NeumorphicCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = Color(0xFF1A1D22)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            ResponsiveText(
                                text = "Challenge Required:",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            ResponsiveText(
                                text = when {
                                    isRememberPattern -> "Remember the Pattern"
                                    isBoxBreathing -> "Box Breathing"
                                    isParagraphSummary -> "Paragraph Summary"
                                    isMath -> "Math Puzzle"
                                    isTyping -> "Typing Challenge"
                                    else -> "Mindful Challenge"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = FrictionPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ResponsiveText(
                                    text = "🔥 ${user.currentStreak} Day Streak",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    FrictionButton(
                        text = "Start Challenge",
                        onClick = { stage = "CHALLENGE" },
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = {
                        isSkipping = true
                        stage = "DURATION_SELECTION"
                    }) {
                        ResponsiveText("Bypass Challenge & Open App", color = TextMuted)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = onCancel) {
                        ResponsiveText("Cancel & Return Home", color = FrictionError)
                    }
                }
            }
            "CHALLENGE" -> {
                when {
                    isRememberPattern -> {
                        RememberPatternChallengeUI(
                            user = user,
                            rule = rule,
                            onComplete = {
                                isSkipping = false
                                stage = "COMPLETED"
                            },
                            onCancel = onCancel
                        )
                    }
                    isBoxBreathing -> {
                        BoxBreathingChallengeUI(
                            user = user,
                            rule = rule,
                            onComplete = {
                                isSkipping = false
                                stage = "COMPLETED"
                            },
                            onCancel = onCancel
                        )
                    }
                    isParagraphSummary -> {
                        ParagraphSummaryChallengeUI(
                            user = user,
                            rule = rule,
                            onComplete = {
                                isSkipping = false
                                stage = "COMPLETED"
                            },
                            onCancel = onCancel
                        )
                    }
                    isMath -> {
                        MathChallengeUI(
                            user = user,
                            rule = rule,
                            onComplete = {
                                isSkipping = false
                                stage = "COMPLETED"
                            },
                            onCancel = onCancel
                        )
                    }
                    else -> {
                        TypingChallengeUI(
                            user = user,
                            rule = rule,
                            onComplete = {
                                isSkipping = false
                                stage = "COMPLETED"
                            },
                            onCancel = onCancel
                        )
                    }
                }
            }
            "COMPLETED" -> {
                ConfettiEffect()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val bounceTransition = rememberInfiniteTransition(label = "success_bounce")
                    val bounceY by bounceTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = -16f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(400, easing = EaseOutQuad),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "bounce"
                    )

                    Image(
                        painter = painterResource(id = R.drawable.success),
                        contentDescription = "Flick Success Mascot",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .offset(y = bounceY.dp)
                            .size(130.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    ResponsiveText(
                        text = "Challenge Complete!",
                        fontSize = dims.displaySmallSize,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ResponsiveText(
                        text = "Current Streak: ${user.currentStreak} 🔥",
                        style = MaterialTheme.typography.titleMedium,
                        color = FrictionAccent
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    FrictionButton(
                        text = "Choose Viewing Time",
                        onClick = { stage = "DURATION_SELECTION" },
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    )
                }
            }
            "DURATION_SELECTION" -> {
                DurationSelectionUI(
                    appName = rule.targetAppName.ifBlank { "this app" },
                    onSelectDuration = { minutes ->
                        if (isSkipping) {
                            onSkip(15, minutes)
                        } else {
                            onComplete(0, 5, minutes)
                        }
                    },
                    onCancel = onCancel
                )
            }
        }
    }
}

@Composable
fun ChallengeHeader(
    title: String,
    xpReward: Int,
    streak: Int,
    motivation: String,
    goal: String
) {
    NeumorphicCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        containerColor = Color(0xFF181C20),
        borderColor = Color(0x20FFFFFF)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ResponsiveText(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FrictionPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0x20FFFFFF),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        ResponsiveText(
                            text = "🔥 $streak",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            if (goal.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                ResponsiveText(
                    text = "Goal: $goal",
                    style = MaterialTheme.typography.bodySmall,
                    color = FrictionAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (motivation.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                ResponsiveText(
                    text = "\"$motivation\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

@Composable
fun TypingChallengeUI(
    user: User,
    rule: FrictionRule,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val sentences = remember {
        listOf(
            "I am productive and I choose my future.",
            "I control my phone, my phone does not control me.",
            "My goals matter more than endless scrolling.",
            "I choose progress over distraction."
        )
    }
    val targetSentence = remember { sentences.random() }
    var typedText by remember { mutableStateOf("") }

    val normalizedTarget = targetSentence.trim().lowercase()
    val normalizedTyped = typedText.trim().lowercase()
    val isMatched = normalizedTyped == normalizedTarget

    val progress = if (targetSentence.isNotEmpty()) {
        (typedText.length.toFloat() / targetSentence.length.toFloat()).coerceAtMost(1f)
    } else 0f

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ChallengeHeader(
            title = "Typing Challenge",
            xpReward = rule.penaltyXp,
            streak = user.currentStreak,
            motivation = user.motivation.ifBlank { "I choose progress over distraction." },
            goal = user.goal
        )

        Spacer(modifier = Modifier.height(8.dp))

        NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ResponsiveText("Type this motivational sentence exactly:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                ResponsiveText(
                    text = targetSentence,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FrictionPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = if (isMatched) FrictionAccent else FrictionPrimary,
            trackColor = DarkSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        NeumorphicTextField(
            value = typedText,
            onValueChange = { typedText = it },
            placeholder = { ResponsiveText("Type quote here...", color = TextMuted) },
            singleLine = false
        )

        Spacer(modifier = Modifier.height(24.dp))

        FrictionButton(
            text = if (isMatched) "Complete Challenge ✓" else "Type Sentence to Unlock",
            onClick = { if (isMatched) onComplete() },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        )
    }
}

@Composable
fun MathChallengeUI(
    user: User,
    rule: FrictionRule,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    var question by remember { mutableStateOf("") }
    var options by remember { mutableStateOf<List<Int>>(emptyList()) }
    var correctAnswer by remember { mutableStateOf(0) }
    var questionsSolved by remember { mutableStateOf(0) }
    val totalRequired = 3

    fun generateNewQuestion() {
        val ops = listOf("+", "-", "*", "/")
        val op = ops.random()
        var qStr = ""
        var ans = 0
        when (op) {
            "+" -> {
                val a = Random.nextInt(15, 85)
                val b = Random.nextInt(15, 85)
                qStr = "$a + $b"
                ans = a + b
            }
            "-" -> {
                val a = Random.nextInt(50, 150)
                val b = Random.nextInt(10, 49)
                qStr = "$a - $b"
                ans = a - b
            }
            "*" -> {
                val a = Random.nextInt(4, 15)
                val b = Random.nextInt(4, 15)
                qStr = "$a × $b"
                ans = a * b
            }
            "/" -> {
                val b = Random.nextInt(3, 12)
                val ansVal = Random.nextInt(4, 15)
                val a = b * ansVal
                qStr = "$a ÷ $b"
                ans = ansVal
            }
        }
        question = qStr
        correctAnswer = ans

        val optionSet = mutableSetOf(ans)
        val offsets = listOf(-3, 2, -5, 4, -10, 10, -1, 3).shuffled()
        for (offset in offsets) {
            val distractor = ans + offset
            if (distractor != ans && distractor >= 0) {
                optionSet.add(distractor)
            }
            if (optionSet.size == 4) break
        }
        while (optionSet.size < 4) {
            optionSet.add(ans + Random.nextInt(1, 20))
        }
        options = optionSet.toList().shuffled()
    }

    LaunchedEffect(Unit) {
        generateNewQuestion()
    }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ChallengeHeader(
            title = "Math Puzzle ($questionsSolved / $totalRequired)",
            xpReward = rule.penaltyXp,
            streak = user.currentStreak,
            motivation = user.motivation.ifBlank { "Solve arithmetic to activate focus." },
            goal = user.goal
        )

        Spacer(modifier = Modifier.height(12.dp))

        NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ResponsiveText("Arithmetic Challenge", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                ResponsiveText(
                    text = "$question = ?",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (i in 0 until 2) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (j in 0 until 2) {
                        val index = i * 2 + j
                        if (index < options.size) {
                            val optionVal = options[index]
                            NeumorphicCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .clickable {
                                        if (optionVal == correctAnswer) {
                                            questionsSolved++
                                            if (questionsSolved >= totalRequired) {
                                                onComplete()
                                            } else {
                                                generateNewQuestion()
                                            }
                                        } else {
                                            generateNewQuestion()
                                        }
                                    },
                                containerColor = DarkCardBg,
                                borderColor = Color(0x30FFFFFF)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ResponsiveText(
                                        text = "$optionVal",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfettiEffect() {
    var trigger by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        trigger = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (trigger) 0f else 1f,
        animationSpec = tween(durationMillis = 2000, delayMillis = 1000)
    )
    Canvas(modifier = Modifier.fillMaxSize().alpha(alpha)) {
        for (i in 0..50) {
            val x = Random.nextFloat() * size.width
            val y = Random.nextFloat() * size.height
            val radius = Random.nextFloat() * 10f + 5f
            val color = listOf(Color(0xFFE2B714), Color(0xFF4F46E5), Color(0xFFF0F0F0)).random()
            drawCircle(color = color, radius = radius, center = Offset(x, y))
        }
    }
}

@Composable
fun RememberPatternChallengeUI(
    user: User,
    rule: FrictionRule,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    var attemptCount by remember { mutableIntStateOf(1) }
    var pattern by remember(attemptCount) {
        mutableStateOf((0..19).shuffled().take(5))
    }
    var phase by remember(attemptCount) { mutableStateOf("MEMORIZE") }
    var secondsLeft by remember(attemptCount) { mutableIntStateOf(5) }
    var userTappedIndices by remember(attemptCount) { mutableStateOf(listOf<Int>()) }
    var errorMessage by remember(attemptCount) { mutableStateOf("") }

    LaunchedEffect(phase, attemptCount) {
        if (phase == "MEMORIZE") {
            secondsLeft = 5
            while (secondsLeft > 0) {
                delay(1000L)
                secondsLeft--
            }
            phase = "RESPONSE"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ChallengeHeader(
            title = "Remember the Pattern",
            xpReward = rule.penaltyXp,
            streak = user.currentStreak,
            motivation = when (phase) {
                "MEMORIZE" -> "Memorize tiles 1 to 5 ($secondsLeft s)"
                "RESPONSE" -> "Tap the 5 tiles in order: 1 → 2 → 3 → 4 → 5"
                "ERROR" -> errorMessage
                else -> "Tap the tiles in sequence"
            },
            goal = user.goal.ifBlank { "Build focus & memory" }
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (phase == "MEMORIZE") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = FrictionPrimary)
                ResponsiveText(
                    text = "Memorization: ${secondsLeft}s",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FrictionPrimary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = secondsLeft / 5f,
                color = FrictionPrimary,
                trackColor = DarkSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
            )
        } else if (phase == "RESPONSE") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.TouchApp, contentDescription = null, tint = FrictionAccent)
                ResponsiveText(
                    text = "Sequence Tapped: ${userTappedIndices.size} / 5",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FrictionAccent
                )
            }
        } else if (phase == "ERROR") {
            ResponsiveText(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = FrictionError,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4 x 5 Grid (20 Tiles)
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (row in 0 until 5) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (col in 0 until 4) {
                        val tileIndex = row * 4 + col
                        val isPatternTile = tileIndex in pattern
                        val patternNum = if (isPatternTile) pattern.indexOf(tileIndex) + 1 else null
                        val wasTapped = tileIndex in userTappedIndices
                        val tappedOrder = if (wasTapped) userTappedIndices.indexOf(tileIndex) + 1 else null

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        phase == "MEMORIZE" && isPatternTile -> FrictionPrimary.copy(alpha = 0.35f)
                                        wasTapped -> FrictionAccent.copy(alpha = 0.35f)
                                        phase == "ERROR" -> FrictionError.copy(alpha = 0.15f)
                                        else -> DarkSurface
                                    }
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = when {
                                        phase == "MEMORIZE" && isPatternTile -> FrictionPrimary
                                        wasTapped -> FrictionAccent
                                        phase == "ERROR" -> FrictionError
                                        else -> Color.White.copy(alpha = 0.12f)
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = phase == "RESPONSE") {
                                    if (wasTapped) return@clickable

                                    val expectedTile = pattern[userTappedIndices.size]
                                    if (tileIndex == expectedTile) {
                                        val nextTapped = userTappedIndices + tileIndex
                                        userTappedIndices = nextTapped
                                        if (nextTapped.size == 5) {
                                            onComplete()
                                        }
                                    } else {
                                        phase = "ERROR"
                                        errorMessage = "Incorrect tile! Tap below to try a new pattern."
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (phase == "MEMORIZE" && patternNum != null) {
                                ResponsiveText(
                                    text = "$patternNum",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = FrictionPrimary
                                )
                            } else if (phase == "RESPONSE" && tappedOrder != null) {
                                ResponsiveText(
                                    text = "✓ $tappedOrder",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = FrictionAccent
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (phase == "ERROR") {
            FrictionButton(
                text = "Restart with New Pattern",
                onClick = { attemptCount++ },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            )
        } else {
            TextButton(onClick = onCancel) {
                ResponsiveText("Cancel & Go Back", color = TextMuted)
            }
        }
    }
}

@Composable
fun BoxBreathingChallengeUI(
    user: User,
    rule: FrictionRule,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val maxCycles = remember(rule.challengeValue) { rule.challengeValue.coerceIn(1, 3) }
    var cycle by remember { mutableIntStateOf(1) }
    var stepName by remember { mutableStateOf("Inhale") }
    var secondsLeft by remember { mutableIntStateOf(4) }

    LaunchedEffect(cycle) {
        if (cycle <= maxCycles) {
            val steps = listOf("Inhale", "Hold", "Exhale", "Hold")
            for (s in steps) {
                stepName = s
                for (sec in 4 downTo 1) {
                    secondsLeft = sec
                    delay(1000L)
                }
            }
            if (cycle < maxCycles) {
                cycle++
            } else {
                onComplete()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ChallengeHeader(
            title = "Box Breathing",
            xpReward = rule.penaltyXp,
            streak = user.currentStreak,
            motivation = "Calm your mind before entering ${rule.targetAppName}",
            goal = user.goal.ifBlank { "Practice mindfulness" }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(FrictionPrimary.copy(alpha = 0.2f))
                .border(2.dp, FrictionPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ResponsiveText(
                    text = stepName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = FrictionPrimary
                )
                ResponsiveText(
                    text = "${secondsLeft}s",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        ResponsiveText(
            text = "Cycle $cycle of $maxCycles",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = FrictionAccent
        )

        Spacer(modifier = Modifier.height(12.dp))

        ResponsiveText(
            text = "💡 Mindfulness guidance tool. Physical or medical metrics are not tracked.",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        TextButton(onClick = onCancel) {
            ResponsiveText("Cancel & Go Back", color = TextMuted)
        }
    }
}

@Composable
fun ParagraphSummaryChallengeUI(
    user: User,
    rule: FrictionRule,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val passages = remember {
        listOf(
            "Digital mindfulness is the practice of engaging with technology intentionally and consciously. By setting clear boundaries and pausing before opening social media apps, you preserve cognitive energy for high-priority tasks and long-term personal goals.",
            "Deep focus requires eliminating minor interruptions. Everyday distractions fragment attention and increase cognitive load. Pausing for a quick challenge helps rebuild focus and intentional decision-making.",
            "Building long-term habits requires consistent daily friction against impulsive behaviors. Small daily decisions accumulate over weeks, establishing strong mental discipline and self-control."
        )
    }
    val passage = remember { passages.random() }
    var userSummary by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ChallengeHeader(
            title = "Paragraph Summary",
            xpReward = rule.penaltyXp,
            streak = user.currentStreak,
            motivation = "Summarize the key idea in your own words (10+ words)",
            goal = user.goal.ifBlank { "Enhance comprehension" }
        )

        Spacer(modifier = Modifier.height(12.dp))

        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = DarkSurface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ResponsiveText(
                    text = "Passage to Summarize:",
                    style = MaterialTheme.typography.labelMedium,
                    color = FrictionSecondaryText,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                ResponsiveText(
                    text = passage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = userSummary,
            onValueChange = {
                userSummary = it
                errorMessage = ""
            },
            placeholder = { ResponsiveText("Write your summary here (around 10-20 words)...", color = TextMuted) },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedBorderColor = FrictionPrimary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        if (errorMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            ResponsiveText(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = FrictionError,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        FrictionButton(
            text = "Submit Summary",
            isLoading = isChecking,
            onClick = {
                val wordCount = userSummary.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
                if (wordCount < 8) {
                    errorMessage = "Summary is too short. Please write at least 10 words."
                    return@FrictionButton
                }
                isChecking = true
                coroutineScope.launch {
                    val service = com.example.data.service.GeminiService()
                    val (isAccurate, feedback) = service.verifySummary(passage, userSummary)
                    isChecking = false
                    if (isAccurate) {
                        onComplete()
                    } else {
                        errorMessage = feedback.ifBlank { "Summary doesn't capture the main idea. Try again." }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onCancel) {
            ResponsiveText("Cancel & Go Back", color = TextMuted)
        }
    }
}

@Composable
fun DurationSelectionUI(
    appName: String,
    onSelectDuration: (minutes: Int) -> Unit,
    onCancel: () -> Unit
) {
    var selectedDuration by remember { mutableIntStateOf(5) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.challenge),
            contentDescription = "Mascot",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        ResponsiveText(
            text = "How long do you want to watch?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        ResponsiveText(
            text = "Select how long you intend to stay on $appName.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        val durationOptions = listOf(1, 2, 5, 10)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            durationOptions.forEach { mins ->
                val isSelected = selectedDuration == mins
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) FrictionPrimary.copy(alpha = 0.25f) else DarkSurface)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) FrictionPrimary else Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { selectedDuration = mins }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ResponsiveText(
                            text = "$mins",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) FrictionPrimary else TextPrimary
                        )
                        ResponsiveText(
                            text = if (mins == 1) "min" else "mins",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        FrictionButton(
            text = "Set Timer & Open $appName",
            onClick = { onSelectDuration(selectedDuration) },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onCancel) {
            ResponsiveText("Cancel & Return Home", color = TextMuted)
        }
    }
}

@Composable
fun ExpirationOverlayUI(
    user: User,
    appName: String,
    onCloseApp: () -> Unit,
    onOpenAnyway: () -> Unit
) {
    val goalText = user.goal.ifBlank { "Achieve daily focus & build healthier screen time habits." }
    val motivationText = user.motivation.ifBlank { "Stay disciplined and protect your energy." }

    var aiMotivationalSentence by remember {
        mutableStateOf("Your time on $appName is up! Remember: $motivationText")
    }

    LaunchedEffect(user.goal, user.motivation) {
        val prompt = "Generate a single 1-sentence punchy, encouraging reminder for someone whose watch timer on $appName just expired. Their long term goal is: \"$goalText\" and their core motivation is: \"$motivationText\"."
        try {
            val service = com.example.data.service.GeminiService()
            val result = service.generateText(prompt)
            if (result.isNotBlank()) {
                aiMotivationalSentence = result.trim().removeSurrounding("\"")
            }
        } catch (e: Exception) {
            // Keep default fallback sentence
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color(0xFF191C20)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = FrictionError,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                ResponsiveText(
                    text = "Time's Up!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = FrictionError,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                ResponsiveText(
                    text = "Your allowed viewing window for $appName has ended.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color(0xFF1A1D22)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                ResponsiveText(
                    text = "🎯 YOUR GOAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = FrictionAccent,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                ResponsiveText(
                    text = goalText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                ResponsiveText(
                    text = "💡 YOUR MOTIVATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = FrictionPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                ResponsiveText(
                    text = motivationText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(FrictionPrimary.copy(alpha = 0.12f))
                        .padding(12.dp)
                ) {
                    ResponsiveText(
                        text = "✨ \"$aiMotivationalSentence\"",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        FrictionButton(
            text = "Close App",
            onClick = onCloseApp,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onOpenAnyway,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
        ) {
            ResponsiveText(
                text = "Open Anyway (Re-trigger Challenge)",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
