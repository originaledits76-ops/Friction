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
import com.example.data.model.FrictionRule
import com.example.features.home.HomeViewModel
import com.example.ui.theme.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLimitWizard(
    homeViewModel: HomeViewModel?,
    onDismiss: () -> Unit,
    onSave: (FrictionRule) -> Unit
) {
    val context = LocalContext.current
    val allInstalledApps = remember { getInstalledApps(context) }
    val savedClassificationsState = homeViewModel?.appClassifications?.collectAsState()
    val savedClassifications = savedClassificationsState?.value ?: emptyList()
    
    val appList = remember(allInstalledApps, savedClassifications) {
        allInstalledApps.filter { appInfo ->
            val clazz = savedClassifications.find { it.packageName == appInfo.packageName }?.classification
                ?: getDefaultClassification(appInfo.packageName, appInfo.appName)
            clazz == "DISTRACTING"
        }
    }
    var step by remember { mutableStateOf(1) }
    
    var selectedAppName by remember { mutableStateOf("") }
    var selectedAppPackage by remember { mutableStateOf("") }
    var selectedTriggerValue by remember { mutableStateOf(10) } // Minutes
    var selectedChallenge by remember { mutableStateOf("") }
    var challengeValue by remember { mutableStateOf(10) }
    var suggestionMessage by remember { mutableStateOf("") }

    var object1 by remember { mutableStateOf("Water Bottle") }
    var object2 by remember { mutableStateOf("Notebook") }
    var object3 by remember { mutableStateOf("Backpack") }
    var object4 by remember { mutableStateOf("Pen") }
    var object5 by remember { mutableStateOf("Chair") }

    val challengesList = listOf("Typing challenge", "Math challenge", "Push-ups", "Walk 100m", "Find Object")

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
                Text("Step $step of 4", style = MaterialTheme.typography.titleMedium, color = TextSecondary, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                }
            }
            
            // Progress Bar
            LinearProgressIndicator(
                progress = step / 4f,
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = FrictionPrimary,
                trackColor = DarkSurface
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // Content
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).weight(1f)) {
                when (step) {
                    1 -> {
                        Text("Choose an App", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Which app distracts you the most?", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(appList) { appInfo ->
                                AppSelectionCard(
                                    name = appInfo.appName,
                                    selected = selectedAppPackage == appInfo.packageName,
                                    onClick = { selectedAppName = appInfo.appName; selectedAppPackage = appInfo.packageName }
                                )
                            }
                        }
                    }
                    2 -> {
                        Text("Choose Trigger", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("When should Friction step in?", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text("Time spent continuously", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        listOf(5, 10, 15, 30, 45, 60).forEach { mins ->
                            SelectionCard(
                                label = "$mins minutes",
                                selected = selectedTriggerValue == mins,
                                onClick = { selectedTriggerValue = mins }
                            )
                        }
                    }
                    3 -> {
                        Text("Choose Challenge", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("What will you do to earn access?", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(challengesList) { chal ->
                                SelectionCard(
                                    label = chal,
                                    selected = selectedChallenge == chal,
                                    onClick = { selectedChallenge = chal }
                                )
                            }
                        }
                    }
                    4 -> {
                        Text("Configure Challenge", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Set up your challenge settings.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Spacer(modifier = Modifier.height(24.dp))

                        if (selectedChallenge == "Find Object") {
                            Text("Configure 5 Personal Objects", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("When triggered, Friction will ask you to find one of these:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Spacer(modifier = Modifier.height(12.dp))

                            com.example.core.widgets.NeumorphicTextField(value = object1, onValueChange = { object1 = it }, placeholder = { Text("Object 1 (e.g. Water Bottle)") })
                            Spacer(modifier = Modifier.height(8.dp))
                            com.example.core.widgets.NeumorphicTextField(value = object2, onValueChange = { object2 = it }, placeholder = { Text("Object 2 (e.g. Notebook)") })
                            Spacer(modifier = Modifier.height(8.dp))
                            com.example.core.widgets.NeumorphicTextField(value = object3, onValueChange = { object3 = it }, placeholder = { Text("Object 3 (e.g. Backpack)") })
                            Spacer(modifier = Modifier.height(8.dp))
                            com.example.core.widgets.NeumorphicTextField(value = object4, onValueChange = { object4 = it }, placeholder = { Text("Object 4 (e.g. Pen)") })
                            Spacer(modifier = Modifier.height(8.dp))
                            com.example.core.widgets.NeumorphicTextField(value = object5, onValueChange = { object5 = it }, placeholder = { Text("Object 5 (e.g. Chair)") })
                        } else {
                            Text(
                                text = when (selectedChallenge) {
                                    "Push-ups" -> "Target Push-ups"
                                    "Walk 100m" -> "Target Distance (meters)"
                                    "Math challenge" -> "Number of Math Questions"
                                    "Typing challenge" -> "Target Typing Rounds"
                                    else -> "Target Amount"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(onClick = { if (challengeValue > 1) challengeValue-- }) {
                                    Icon(Icons.Default.RemoveCircleOutline, null, tint = FrictionPrimary, modifier = Modifier.size(48.dp))
                                }
                                Spacer(modifier = Modifier.width(32.dp))
                                Text(
                                    text = if (selectedChallenge == "Push-ups" && challengeValue < 20) "20" else "$challengeValue",
                                    style = MaterialTheme.typography.displayLarge,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(32.dp))
                                IconButton(onClick = { challengeValue++ }) {
                                    Icon(Icons.Default.AddCircleOutline, null, tint = FrictionPrimary, modifier = Modifier.size(48.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        com.example.core.widgets.NeumorphicCard(
                            containerColor = FrictionAccent.copy(alpha = 0.1f),
                            borderColor = FrictionAccent.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lightbulb, null, tint = FrictionAccent)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = when (selectedChallenge) {
                                        "Push-ups" -> "20 Push-ups earns 7 minutes of unlock access."
                                        "Find Object" -> "5 objects configured. A random object will be selected at challenge start."
                                        "Walk 100m" -> "Walk 100m using motion sensor step verification."
                                        "Math challenge" -> "Solve medium-hard arithmetic questions."
                                        "Typing challenge" -> "Type motivational sentences with exact matching."
                                        else -> "Every mindful choice shapes tomorrow."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = FrictionAccent
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Nav
            com.example.core.widgets.FrictionButton(
                text = if (step < 4) "Continue" else "Save Limit",
                onClick = {
                    if (step < 4) {
                        if (step == 1 && selectedAppPackage.isEmpty()) return@FrictionButton
                        if (step == 3 && selectedChallenge.isEmpty()) return@FrictionButton
                        step++
                    } else {
                        val mappedType = when (selectedChallenge) {
                            "Typing challenge" -> "TYPING"
                            "Math challenge" -> "MATH"
                            "Push-ups" -> "PUSHUPS"
                            "Walk 100m" -> "WALK"
                            "Find Object" -> "FIND_OBJECT"
                            else -> "TYPING"
                        }
                        if (mappedType == "FIND_OBJECT") {
                            val objList = listOf(object1, object2, object3, object4, object5)
                            homeViewModel?.updateCustomObjects(objList)
                        }
                        val finalVal = if (mappedType == "PUSHUPS" && challengeValue < 20) 20 else challengeValue
                        onSave(
                            FrictionRule(
                                id = UUID.randomUUID().toString(),
                                name = selectedAppName,
                                targetAppPackage = selectedAppPackage,
                                targetAppName = selectedAppName,
                                triggerType = "TIME",
                                thresholdMinutes = selectedTriggerValue,
                                challengeType = mappedType,
                                challengeValue = finalVal,
                                penaltyXp = 30
                            )
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
fun AppSelectionCard(name: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) FrictionPrimary.copy(alpha = 0.2f) else DarkSurface),
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, FrictionPrimary) else null,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.height(64.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Apps, null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.weight(1f))
            if (selected) {
                Icon(Icons.Default.CheckCircle, null, tint = FrictionPrimary)
            }
        }
    }
}

@Composable
fun SelectionCard(label: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) FrictionPrimary.copy(alpha = 0.2f) else DarkSurface),
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, FrictionPrimary) else null,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { onClick() }.height(56.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = TextPrimary)
            Spacer(modifier = Modifier.weight(1f))
            if (selected) {
                Icon(Icons.Default.CheckCircle, null, tint = FrictionPrimary)
            }
        }
    }
}
