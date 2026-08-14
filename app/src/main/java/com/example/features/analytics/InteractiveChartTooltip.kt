package com.example.features.analytics

import com.example.core.widgets.ResponsiveText
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.service.IntervalDetails
import com.example.features.dashboard.formatTimeMs
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartTooltipBottomSheet(
    details: IntervalDetails,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = com.example.ui.theme.DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ResponsiveText(
                text = details.timeLabel,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            
            ResponsiveText(
                text = "Screen Time: ${formatTimeMs(details.screenTimeMs)}",
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )
            
            ResponsiveText(
                text = "Most Used App: ${details.mostUsedApp}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            
            ResponsiveText(
                text = "Launches: ${details.launches} | Unlocks: ${details.unlocks}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
