package com.example.features.settings

import com.example.core.widgets.ResponsiveText
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import android.util.Log
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

    // Observe active rules in real-time to hide already-limited apps (Req #11 & #12)
    val activeRulesState = homeViewModel?.rules?.collectAsState()
    val activeRules = activeRulesState?.value ?: emptyList()
    val activeLimitedPackages = remember(activeRules) {
        activeRules.filter { it.active }.map { it.targetAppPackage }.toSet()
    }

    if (showLockedSheet) {
        com.example.features.paywall.LockedFeatureSheet(
            featureTitle = "Premium Challenge",
            featureDescription = "Upgrade to Premium to unlock Math, Typing, Box Breathing, Paragraph Summary, and Remember Pattern challenges!",
            onUpgrade = {
                showLockedSheet = false
                onOpenPaywall()
            },
            onDismiss = { showLockedSheet = false }
        )
    }

    // Filter out apps that already have an active limit (Req #11 & #12)
    val filteredApps = remember(allInstalledApps, activeLimitedPackages, searchQuery) {
        allInstalledApps.filter { app ->
            app.packageName !in activeLimitedPackages &&
            (searchQuery.isBlank() ||
             app.appName.contains(searchQuery, ignoreCase = true) ||
             app.packageName.contains(searchQuery, ignoreCase = true))
        }
    }

    var step by remember { mutableStateOf(1) }

    var selectedAppName by remember { mutableStateOf("") }
    var selectedAppPackage by remember { mutableStateOf("") }
    var selectedChallenge by remember { mutableStateOf("") }
    var challengeValue by remember { mutableStateOf(20) }

    // Exactly 10 allowed object types for Find Object challenge (Req #8)
    val allowedObjectTypes = remember {
        emptyList<String>()
    }


    val challengesList = listOf(
        "Typing challenge",
        "Math challenge",
        "Box Breathing",
        "Paragraph Summary",
        "Remember the Pattern"
    )

    val requiresConfig = selectedChallenge == "Box Breathing"
    val totalSteps = if (requiresConfig && step >= 2) 3 else 2

    if (showLockedSheet) {
        com.example.features.paywall.LockedFeatureSheet(
            featureTitle = "Pro Challenge",
            featureDescription = "Typing and Math Puzzle challenges are free for everyone. Upgrade to Pro to unlock Box Breathing, Paragraph Summary and all advanced challenges!",
            onUpgrade = {
                showLockedSheet = false
                onOpenPaywall()
            },
            onDismiss = { showLockedSheet = false }
        )
    }

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
                ResponsiveText("Step $step of $totalSteps", style = MaterialTheme.typography.titleMedium, color = TextSecondary, fontWeight = FontWeight.Bold)
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
                        ResponsiveText("Choose an App", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        ResponsiveText("Select any installed application to create a barrier for.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Spacer(modifier = Modifier.height(16.dp))

                        NeumorphicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { ResponsiveText("Search apps...", color = TextMuted) },
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
                        ResponsiveText("Choose Challenge", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        ResponsiveText("Select what you must do to earn access when opening ${selectedAppName.ifEmpty { "the app" }}.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Spacer(modifier = Modifier.height(24.dp))

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(challengesList) { chal ->
                                val isFree = chal == "Typing challenge" || chal == "Math challenge"
                                val isLocked = !isPremium && !isFree
                                SelectionCard(
                                    label = chal,
                                    selected = selectedChallenge == chal,
                                    isPremiumLocked = isLocked,
                                    onClick = {
                                        if (isLocked) {
                                            showLockedSheet = true
                                        } else {
                                            selectedChallenge = chal
                                            if (chal == "Box Breathing") {
                                                challengeValue = 1 // Default 1 cycle
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                    3 -> {
                        ResponsiveText("Configure Challenge", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        ResponsiveText("Set up your challenge details.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Spacer(modifier = Modifier.height(24.dp))

                        if (selectedChallenge == "Box Breathing") {
                            ResponsiveText("Breathing Cycles", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            ResponsiveText("Select number of cycles (1 to 3 cycles). Each cycle guides 4s inhale, 4s hold, 4s exhale, 4s hold.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(onClick = { if (challengeValue > 1) challengeValue-- }) {
                                    Icon(Icons.Default.RemoveCircleOutline, null, tint = FrictionPrimary, modifier = Modifier.size(48.dp))
                                }
                                Spacer(modifier = Modifier.width(24.dp))
                                ResponsiveText(
                                    text = "$challengeValue ${if (challengeValue == 1) "Cycle" else "Cycles"}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(24.dp))
                                IconButton(onClick = { if (challengeValue < 3) challengeValue++ }) {
                                    Icon(Icons.Default.AddCircleOutline, null, tint = FrictionPrimary, modifier = Modifier.size(48.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
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
                                    objectList = emptyList(),
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
                            objectList = emptyList(),
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

@Composable
fun ObjectSlotDropdown(
    label: String,
    selectedOption: String,
    availableOptions: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DarkSurface,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    ResponsiveText(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = FrictionSecondaryText
                    )
                    ResponsiveText(
                        text = selectedOption,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = FrictionAccent
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select Object",
                    tint = TextPrimary
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(DarkSurface)
        ) {
            availableOptions.forEach { option ->
                DropdownMenuItem(
                    text = {
                        ResponsiveText(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
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
    // Validate active rules again to prevent duplicates (Req #11 & #12)
    val existingRules = homeViewModel?.rules?.value ?: emptyList()
    val isDuplicate = existingRules.any { it.active && it.targetAppPackage == selectedAppPackage }
    if (isDuplicate) {
        Log.w("AddLimitWizard", "Rejected duplicate limit rule for package: $selectedAppPackage")
        return
    }

    val mappedType = when (selectedChallenge) {
        "Typing challenge" -> "TYPING"
        "Math challenge" -> "MATH"
        "Box Breathing" -> "BOX_BREATHING"
        "Paragraph Summary" -> "PARAGRAPH_SUMMARY"
        "Remember the Pattern" -> "REMEMBER_PATTERN"
        else -> "TYPING"
    }

    val finalVal = when (mappedType) {
        "MATH" -> 3
        "TYPING" -> 1
        "REMEMBER_PATTERN" -> 5
        "BOX_BREATHING" -> challengeValue
        "PARAGRAPH_SUMMARY" -> 1
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
            penaltyXp = 0
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
            ResponsiveText(
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
            ResponsiveText(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = TextPrimary)
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
                        ResponsiveText("PREMIUM", style = MaterialTheme.typography.labelSmall, color = FrictionAccent, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            } else if (selected) {
                Icon(Icons.Default.CheckCircle, null, tint = FrictionPrimary)
            }
        }
    }
}
