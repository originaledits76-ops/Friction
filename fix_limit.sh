#!/bin/bash
sed -i '/com.example.core.widgets.FrictionButton(/,/)/c\
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {\
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextMuted)\
                }' app/src/main/java/com/example/features/settings/SettingsScreen.kt
