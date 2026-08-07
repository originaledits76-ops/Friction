package com.example.features.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import com.example.features.home.HomeViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppClassificationScreen(
    onDismiss: () -> Unit,
    homeViewModel: HomeViewModel?
) {
    val context = LocalContext.current
    val installedApps = remember { getInstalledApps(context) }
    
    val savedClassifications by homeViewModel?.appClassifications?.collectAsState() ?: mutableStateOf(emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "PRODUCTIVE", "DISTRACTING"
    var selectedSort by remember { mutableStateOf("AZ") } // "AZ", "ZA", "CATEGORY"
    var showSortMenu by remember { mutableStateOf(false) }

    // Filter and Sort apps
    val processedApps = remember(installedApps, savedClassifications, searchQuery, selectedFilter, selectedSort) {
        var list = installedApps.map { app ->
            val classification = savedClassifications.find { it.packageName == app.packageName }?.classification
                ?: getDefaultClassification(app.packageName, app.appName)
            app to classification
        }

        // Search
        if (searchQuery.isNotBlank()) {
            list = list.filter { (app, _) ->
                app.appName.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)
            }
        }

        // Filter
        if (selectedFilter != "ALL") {
            list = list.filter { (_, classification) ->
                classification == selectedFilter
            }
        }

        // Sort
        list = when (selectedSort) {
            "AZ" -> list.sortedBy { it.first.appName.lowercase() }
            "ZA" -> list.sortedByDescending { it.first.appName.lowercase() }
            "CATEGORY" -> list.sortedWith(compareBy({ it.second }, { it.first.appName.lowercase() }))
            else -> list
        }

        list
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Title Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "App Classification",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // Description Info Block
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x10FFFFFF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = FrictionPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Friction uses these classifications to intelligently restrict distracting applications while protecting productive workflow tools.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search & Sort row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search bar
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps...", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        disabledContainerColor = DarkSurface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                // Sort button
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = DarkSurface),
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort Options", tint = TextPrimary)
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Name (A-Z)", color = TextPrimary) },
                            leadingIcon = { if (selectedSort == "AZ") Icon(Icons.Default.Check, contentDescription = null, tint = FrictionPrimary) },
                            onClick = { selectedSort = "AZ"; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Name (Z-A)", color = TextPrimary) },
                            leadingIcon = { if (selectedSort == "ZA") Icon(Icons.Default.Check, contentDescription = null, tint = FrictionPrimary) },
                            onClick = { selectedSort = "ZA"; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Category", color = TextPrimary) },
                            leadingIcon = { if (selectedSort == "CATEGORY") Icon(Icons.Default.Check, contentDescription = null, tint = FrictionPrimary) },
                            onClick = { selectedSort = "CATEGORY"; showSortMenu = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL", "PRODUCTIVE", "DISTRACTING").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    val label = when (filter) {
                        "ALL" -> "All (${installedApps.size})"
                        "PRODUCTIVE" -> "Productive"
                        else -> "Distracting"
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FrictionPrimary.copy(alpha = 0.15f),
                            selectedLabelColor = FrictionPrimary,
                            containerColor = DarkSurface,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) FrictionPrimary else Color(0x10FFFFFF),
                            selectedBorderColor = FrictionPrimary,
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.dp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // LazyColumn to list apps
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (processedApps.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No applications match your criteria.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(processedApps, key = { it.first.packageName }) { (appInfo, classification) ->
                        ClassificationCard(
                            appInfo = appInfo,
                            currentClassification = classification,
                            onClassificationChange = { newClass ->
                                homeViewModel?.saveAppClassification(appInfo.packageName, appInfo.appName, newClass)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClassificationCard(
    appInfo: AppInfo,
    currentClassification: String,
    onClassificationChange: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x08FFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Real app icon using AndroidView
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x08FFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    if (appInfo.icon != null) {
                        AndroidView(
                            factory = { ctx ->
                                ImageView(ctx).apply {
                                    setImageDrawable(appInfo.icon)
                                    scaleType = ImageView.ScaleType.FIT_CENTER
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        )
                    } else {
                        // Fallback letter avatar
                        Text(
                            text = appInfo.appName.take(1).uppercase(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = FrictionPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appInfo.appName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = appInfo.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = TextMuted,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Two standard categories: Productive and Distracting
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ClassificationChip(
                    text = "Distracting",
                    selected = currentClassification == "DISTRACTING",
                    onClick = { onClassificationChange("DISTRACTING") },
                    activeColor = FrictionError,
                    modifier = Modifier.weight(1f)
                )
                ClassificationChip(
                    text = "Productive",
                    selected = currentClassification == "PRODUCTIVE",
                    onClick = { onClassificationChange("PRODUCTIVE") },
                    activeColor = FrictionPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ClassificationChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) activeColor.copy(alpha = 0.12f) else Color.Transparent
    val contentColor = if (selected) activeColor else TextSecondary
    val borderColor = if (selected) activeColor else Color(0x15FFFFFF)
    
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        contentColor = contentColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
