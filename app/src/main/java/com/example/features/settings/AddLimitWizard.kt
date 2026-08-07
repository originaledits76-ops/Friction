package com.example.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.widgets.FrictionButton
import com.example.core.widgets.NeumorphicCard
import com.example.core.widgets.NeumorphicTextField
import com.example.data.model.FrictionRule
import com.example.features.home.HomeViewModel
import com.example.ui.theme.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLimitWizard(
    homeViewModel: HomeViewModel?,
    isPremium: Boolean = false,
    onOpenPaywall: () -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (FrictionRule) -> Unit
) {
    val context = LocalContext.current
    val allInstalledApps = remember { getInstalledApps(context) }
    var searchQuery by remember { mutableStateOf("") }
    var showLockedSheet by remember { mutableStateOf(false) }

    if (showLockedSheet) {
        com.example.features.paywall.LockedFeatureSheet(
            featureTitle = "Premium Challenge",
            featureDescription = "Push-ups are free for everyone. Upgrade to Premium to unlock Math, Typing, Walking, and Object Detection challenges!",
            onUpgrade = {
                showLockedSheet = false
                onOpenPaywall()
            },
            onDismiss = { showLockedSheet = false }
        )
    }

    val filteredApps = remember(allInstalledApps, searchQuery) {
        if (searchQuery.isBlank()) {
            allInstalledApps
        } else {
            allInstalledApps.filter { app ->
                app.appName.contains(searchQuery, ignoreCase = true) ||
                        app.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    var step by remember { mutableStateOf(1) }

    var selectedAppName by remember { mutableStateOf("") }
    var selectedAppPackage by remember { mutableStateOf("") }
    var selectedChallenge by remember { mutableStateOf("") }
    var challengeValue by remember { mutableStateOf(20) }

    var object1 by remember { mutableStateOf("Water Bottle") }
    var object2 by remember { mutableStateOf("Notebook") }
    var object3 by remember { mutableStateOf("Backpack") }
    var object4 by remember { mutableStateOf("Pen") }
    var object5 by remember { mutableStateOf("Chair") }

    val challengesList = listOf("Typing challenge", "Math challenge", "Push-ups", "Walk 100m", "Find Object")

    val requiresConfig = selectedChallenge == "Push-ups" || selectedChallenge == "Find Object"
    val totalSteps = if (requiresConfig && step >= 2) 3 else 2

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (step > 1) step-- else onDismiss()
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text("Step $step of $totalSteps", style = MaterialTheme.typography.titleMedium, color = TextSecondary, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                }
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = step / totalSteps.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = FrictionPrimary,
                trackColor = DarkSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Content
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .weight(1f)) {
                when (step) {
                    1 -> {
                        Text("Choose an App", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Select any installed application to create a barrier for.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Spacer(modifier = Modifier.height(16.dp))

                        NeumorphicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search apps...", color = TextMuted) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextMuted) },
                            trailingIcon = if (searchQuery.isNotEmpty()) {
                                {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                                    }
                                }
                            } else null,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(filteredApps) { appInfo ->
                                AppSelectionCard(
                                    appInfo = appInfo,
                                    selected = selectedAppPackage == appInfo.packageName,
                                    onClick = {
                                        selectedAppName = appInfo.appName
                                        selectedAppPackage = appInfo.packageName
                                    }
                                )
                            }
                        }
                    }
                    2 -> {
                        Text("Choose Challenge", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Select what you must do to earn access when opening ${selectedAppName.ifEmpty { "the app" }}.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Spacer(modifier = Modifier.height(24.dp))

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(challengesList) { chal ->
                                val isFreeChal = chal == "Push-ups"
                                SelectionCard(
                                    label = chal,
                                    selected = selectedChallenge == chal,
                                    isPremiumLocked = !isFreeChal && !isPremium,
                                    onClick = {
                                        if (!isFreeChal && !isPremium) {
                                            showLockedSheet = true
                                        } else {
                                            selectedChallenge = chal
                                        }
                                    }
                                )
                            }
                        }
                    }
                    3 -> {
                        Text("Configure Challenge", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Set up your challenge details.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Spacer(modifier = Modifier.height(24.dp))

                        if (selectedChallenge == "Find Object") {
                            Text("Configure 5 Personal Objects", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("When triggered, Friction will ask you to find one of these:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Spacer(modifier = Modifier.height(12.dp))

                            NeumorphicTextField(value = object1, onValueChange = { object1 = it }, placeholder = { Text("Object 1 (e.g. Water Bottle)") })
                            Spacer(modifier = Modifier.height(8.dp))
                            NeumorphicTextField(value = object2, onValueChange = { object2 = it }, placeholder = { Text("Object 2 (e.g. Notebook)") })
                            Spacer(modifier = Modifier.height(8.dp))
                            NeumorphicTextField(value = object3, onValueChange = { object3 = it }, placeholder = { Text("Object 3 (e.g. Backpack)") })
                            Spacer(modifier = Modifier.height(8.dp))
                            NeumorphicTextField(value = object4, onValueChange = { object4 = it }, placeholder = { Text("Object 4 (e.g. Pen)") })
                            Spacer(modifier = Modifier.height(8.dp))
                            NeumorphicTextField(value = object5, onValueChange = { object5 = it }, placeholder = { Text("Object 5 (e.g. Chair)") })
                        } else if (selectedChallenge == "Push-ups") {
                            Text("Target Push-ups", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(onClick = { if (challengeValue > 5) challengeValue -= 5 }) {
                                    Icon(Icons.Default.RemoveCircleOutline, null, tint = FrictionPrimary, modifier = Modifier.size(48.dp))
                                }
                                Spacer(modifier = Modifier.width(32.dp))
                                Text(
                                    text = "$challengeValue",
                                    style = MaterialTheme.typography.displayLarge,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(32.dp))
                                IconButton(onClick = { challengeValue += 5 }) {
                                    Icon(Icons.Default.AddCircleOutline, null, tint = FrictionPrimary, modifier = Modifier.size(48.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        NeumorphicCard(
                            containerColor = FrictionAccent.copy(alpha = 0.1f),
                            borderColor = FrictionAccent.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lightbulb, null, tint = FrictionAccent)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (selectedChallenge == "Push-ups") "$challengeValue Push-ups earns unlock access." else "5 objects configured. A random object will be selected at challenge start.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = FrictionAccent
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Action Button
            val isFinalStep = (step == 2 && !requiresConfig) || (step == 3)
            val buttonText = if (isFinalStep) "Save Limit" else "Continue"

            FrictionButton(
                text = buttonText,
                onClick = {
                    if (step == 1) {
                        if (selectedAppPackage.isNotEmpty()) step = 2
                    } else if (step == 2) {
                        if (selectedChallenge.isNotEmpty()) {
                            if (requiresConfig) {
                                step = 3
                            } else {
                                saveLimitRule(
                                    selectedAppName = selectedAppName,
                                    selectedAppPackage = selectedAppPackage,
                                    selectedChallenge = selectedChallenge,
                                    challengeValue = challengeValue,
                                    objectList = listOf(object1, object2, object3, object4, object5),
                                    homeViewModel = homeViewModel,
                                    onSave = onSave
                                )
                            }
                        }
                    } else if (step == 3) {
                        saveLimitRule(
                            selectedAppName = selectedAppName,
                            selectedAppPackage = selectedAppPackage,
                            selectedChallenge = selectedChallenge,
                            challengeValue = challengeValue,
                            objectList = listOf(object1, object2, object3, object4, object5),
                            homeViewModel = homeViewModel,
                            onSave = onSave
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            )
        }
    }
}

private fun saveLimitRule(
    selectedAppName: String,
    selectedAppPackage: String,
    selectedChallenge: String,
    challengeValue: Int,
    objectList: List<String>,
    homeViewModel: HomeViewModel?,
    onSave: (FrictionRule) -> Unit
) {
    val mappedType = when (selectedChallenge) {
        "Typing challenge" -> "TYPING"
        "Math challenge" -> "MATH"
        "Push-ups" -> "PUSHUPS"
        "Walk 100m" -> "WALK"
        "Find Object" -> "FIND_OBJECT"
        else -> "TYPING"
    }

    if (mappedType == "FIND_OBJECT") {
        homeViewModel?.updateCustomObjects(objectList)
    }

    val finalVal = when (mappedType) {
        "PUSHUPS" -> maxOf(10, challengeValue)
        "WALK" -> 100
        "MATH" -> 3
        "TYPING" -> 1
        "FIND_OBJECT" -> 5
        else -> 1
    }

    onSave(
        FrictionRule(
            id = UUID.randomUUID().toString(),
            name = selectedAppName,
            targetAppPackage = selectedAppPackage,
            targetAppName = selectedAppName,
            triggerType = "APP_OPEN",
            thresholdMinutes = 0,
            challengeType = mappedType,
            challengeValue = finalVal,
            penaltyXp = 30
        )
    )
}

@Composable
fun AppSelectionCard(appInfo: AppInfo, selected: Boolean, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) FrictionPrimary.copy(alpha = 0.2f) else DarkSurface),
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, FrictionPrimary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .height(64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (appInfo.icon != null) {
                    AsyncImage(
                        model = appInfo.icon,
                        contentDescription = appInfo.appName,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Icon(Icons.Default.Apps, null, tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = appInfo.appName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            if (selected) {
                Icon(Icons.Default.CheckCircle, null, tint = FrictionPrimary)
            }
        }
    }
}

@Composable
fun SelectionCard(
    label: String,
    selected: Boolean,
    isPremiumLocked: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) FrictionPrimary.copy(alpha = 0.2f) else DarkSurface),
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, FrictionPrimary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable { onClick() }
            .height(56.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = TextPrimary)
            Spacer(modifier = Modifier.weight(1f))
            if (isPremiumLocked) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = FrictionAccent.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Lock, null, tint = FrictionAccent, modifier = Modifier.size(12.dp))
                        Text("PREMIUM", style = MaterialTheme.typography.labelSmall, color = FrictionAccent, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            } else if (selected) {
                Icon(Icons.Default.CheckCircle, null, tint = FrictionPrimary)
            }
        }
    }
}
