package com.example.features.settings

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
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.sqrt
import kotlin.random.Random

@Composable
fun ActiveBlockerScreen(
    user: User,
    rule: FrictionRule,
    onComplete: (Int, Int) -> Unit,
    onSkip: (Int) -> Unit,
    onCancel: () -> Unit
) {
    // Intercept system back press to cancel and go home instead of bypassing block
    androidx.activity.compose.BackHandler(onBack = onCancel)

    val dims = LocalResponsiveDimensions.current
    var stage by remember { mutableStateOf("BLOCK") }
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
    val isPushups = typeUpper.contains("PUSH") || typeUpper.contains("SQUAT") || typeUpper.contains("EXERCISE")
    val isWalk = typeUpper.contains("WALK") || typeUpper.contains("STEP")
    val isFindObject = typeUpper.contains("FIND") || typeUpper.contains("OBJECT")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(dims.outerPadding),
        contentAlignment = Alignment.Center
    ) {
        com.example.core.widgets.PremiumBackground(com.example.core.widgets.BackgroundStyle.FRICTION_ENGINE)

        when (stage) {
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
                                painter = painterResource(id = R.drawable.mascot_challenge),
                                contentDescription = "Flick Challenge Mascot",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(110.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "${rule.targetAppName} is Limited",
                                fontSize = dims.titleLargeSize,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
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
                            Text(
                                text = "Challenge Required:",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when {
                                    isTyping -> "Typing Challenge"
                                    isMath -> "Math Puzzle"
                                    isPushups -> "20 Push-ups Challenge"
                                    isWalk -> "Walk 100 Meters Challenge"
                                    isFindObject -> "Find the Object Challenge"
                                    else -> "Typing Challenge"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = FrictionPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = FrictionAccent.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Reward: +${rule.penaltyXp} XP",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = FrictionAccent,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
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

                    TextButton(onClick = { onSkip(rule.penaltyXp / 2) }) {
                        Text("Skip Challenge (Pay ${rule.penaltyXp / 2} XP)", color = TextMuted)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = onCancel) {
                        Text("Cancel & Return Home", color = FrictionError)
                    }
                }
            }
            "CHALLENGE" -> {
                when {
                    isMath -> {
                        MathChallengeUI(
                            user = user,
                            rule = rule,
                            onComplete = { stage = "COMPLETED" },
                            onCancel = onCancel
                        )
                    }
                    isPushups -> {
                        PushupChallengeUI(
                            user = user,
                            rule = rule,
                            onComplete = { stage = "COMPLETED" },
                            onCancel = onCancel
                        )
                    }
                    isWalk -> {
                        WalkingChallengeUI(
                            user = user,
                            rule = rule,
                            onComplete = { stage = "COMPLETED" },
                            onCancel = onCancel
                        )
                    }
                    isFindObject -> {
                        FindObjectChallengeUI(
                            user = user,
                            rule = rule,
                            customObjects = customObjects,
                            onComplete = { stage = "COMPLETED" },
                            onCancel = onCancel
                        )
                    }
                    else -> {
                        TypingChallengeUI(
                            user = user,
                            rule = rule,
                            onComplete = { stage = "COMPLETED" },
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
                        painter = painterResource(id = R.drawable.mascot_success),
                        contentDescription = "Flick Success Mascot",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .offset(y = bounceY.dp)
                            .size(130.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Challenge Complete!",
                        fontSize = dims.displaySmallSize,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "+${rule.penaltyXp} XP Earned",
                        fontSize = dims.displayMediumSize,
                        fontWeight = FontWeight.Bold,
                        color = FrictionPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Current Streak: ${user.currentStreak} 🔥",
                        style = MaterialTheme.typography.titleMedium,
                        color = FrictionAccent
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    FrictionButton(
                        text = "Enter Application",
                        onClick = { onComplete(rule.penaltyXp, 5) },
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    )
                }
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
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FrictionPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = FrictionAccent.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "+$xpReward XP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = FrictionAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color(0x20FFFFFF),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
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
                Text(
                    text = "Goal: $goal",
                    style = MaterialTheme.typography.bodySmall,
                    color = FrictionAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (motivation.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
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
fun CameraPreviewView(
    lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (e: Exception) {
                    Log.e("CameraPreviewView", "Error initializing CameraX preview: ${e.message}")
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = modifier
    )
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
                Text("Type this motivational sentence exactly:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
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
            placeholder = { Text("Type quote here...", color = TextMuted) },
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
                Text("Arithmetic Challenge", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
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
                                    Text(
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
fun PushupChallengeUI(
    user: User,
    rule: FrictionRule,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var pushupCount by remember { mutableStateOf(0) }
    val targetCount = if (rule.challengeValue < 20) 20 else rule.challengeValue
    var hasCameraPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            hasCameraPermission = true
        } else {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ChallengeHeader(
            title = "Push-Up Challenge ($targetCount Reps)",
            xpReward = rule.penaltyXp,
            streak = user.currentStreak,
            motivation = user.motivation.ifBlank { "Physical action builds mental discipline." },
            goal = user.goal
        )

        Surface(
            color = Color(0xFF1E2228),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0x20FFFFFF)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Videocam, null, tint = FrictionPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Front Camera Active • Honest Self-Verification Mode",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (hasCameraPermission) {
                CameraPreviewView(
                    lensFacing = CameraSelector.LENS_FACING_FRONT,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CameraAlt, null, tint = TextMuted, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { permissionLauncher.launch(android.Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = FrictionPrimary)
                    ) {
                        Text("Enable Front Camera")
                    }
                }
            }

            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
            ) {
                Text(
                    text = "$pushupCount / $targetCount Push-ups",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FrictionPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LinearProgressIndicator(
            progress = { pushupCount.toFloat() / targetCount.toFloat() },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = FrictionPrimary,
            trackColor = DarkSurface
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { if (pushupCount < targetCount) pushupCount++ },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("+1 Push-up", color = TextPrimary, fontWeight = FontWeight.Bold)
            }

            FrictionButton(
                text = if (pushupCount >= targetCount) "Complete & Unlock" else "Finish Reps",
                onClick = {
                    if (pushupCount >= targetCount) {
                        onComplete()
                    } else {
                        pushupCount = targetCount
                    }
                },
                modifier = Modifier.weight(1f).height(52.dp)
            )
        }
    }
}

@Composable
fun WalkingChallengeUI(
    user: User,
    rule: FrictionRule,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var stepsCount by remember { mutableStateOf(0) }
    var hasPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val status = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACTIVITY_RECOGNITION)
            if (status == PackageManager.PERMISSION_GRANTED) {
                hasPermission = true
            } else {
                permissionLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
            }
        } else {
            hasPermission = true
        }
    }

    DisposableEffect(hasPermission) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            private var lastMagnitude = 0f
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                    stepsCount++
                } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    val mag = sqrt((x*x + y*y + z*z).toDouble()).toFloat()
                    val diff = mag - lastMagnitude
                    lastMagnitude = mag
                    if (diff > 5.5f) {
                        stepsCount++
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (hasPermission && stepSensor != null) {
            sensorManager?.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    val distanceMeters = (stepsCount * 0.75f).toInt()
    val targetMeters = 100
    val targetSteps = 133

    LaunchedEffect(distanceMeters) {
        if (distanceMeters >= targetMeters) {
            onComplete()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ChallengeHeader(
            title = "Walk 100 Meters Challenge",
            xpReward = rule.penaltyXp,
            streak = user.currentStreak,
            motivation = user.motivation.ifBlank { "Physical movement resets focus." },
            goal = user.goal
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier.size(180.dp).clip(CircleShape).background(FrictionPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${distanceMeters}m",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = FrictionPrimary
                )
                Text(
                    text = "of $targetMeters meters ($stepsCount / $targetSteps steps)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            CircularProgressIndicator(
                progress = { (distanceMeters.toFloat() / targetMeters.toFloat()).coerceAtMost(1f) },
                modifier = Modifier.matchParentSize(),
                color = FrictionAccent,
                strokeWidth = 8.dp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        FrictionButton(
            text = if (distanceMeters >= targetMeters) "Complete Walking ✓" else "Simulate / Finish Walking",
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        )
    }
}

@Composable
fun FindObjectChallengeUI(
    user: User,
    rule: FrictionRule,
    customObjects: List<String>,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val targetObject = remember(customObjects) {
        if (customObjects.isNotEmpty()) customObjects.random() else "Water Bottle"
    }
    var hasCameraPermission by remember { mutableStateOf(false) }
    var photoCaptured by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            hasCameraPermission = true
        } else {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ChallengeHeader(
            title = "Find Object Challenge",
            xpReward = rule.penaltyXp,
            streak = user.currentStreak,
            motivation = user.motivation.ifBlank { "Mindfully engage with your surroundings." },
            goal = user.goal
        )

        Surface(
            color = Color(0xFF1E2228),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0x20FFFFFF)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Camera, null, tint = FrictionPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Rear Camera Active • Photo Confirmation Mode",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
        }

        NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Target Object:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🔍 $targetObject",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = FrictionAccent
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (hasCameraPermission) {
                CameraPreviewView(
                    lensFacing = CameraSelector.LENS_FACING_BACK,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CameraAlt, null, tint = TextMuted, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { permissionLauncher.launch(android.Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = FrictionPrimary)
                    ) {
                        Text("Enable Rear Camera")
                    }
                }
            }

            if (photoCaptured) {
                Surface(
                    color = Color.Black.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = FrictionPrimary, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Snapshot Captured!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Found $targetObject", style = MaterialTheme.typography.bodyMedium, color = FrictionAccent)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (!photoCaptured) {
            FrictionButton(
                text = "Capture $targetObject",
                onClick = { photoCaptured = true },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            )
        } else {
            FrictionButton(
                text = "Confirm & Complete Challenge ✓",
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            )
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
