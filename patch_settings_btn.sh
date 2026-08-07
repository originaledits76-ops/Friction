#!/bin/bash
sed -i '/com.example.core.widgets.FrictionButton(/,/}/c\
                        com.example.core.widgets.FrictionButton(\
                            text = "Add Limit",\
                            icon = Icons.Default.Add,\
                            onClick = { showWizard = true },\
                            modifier = Modifier.fillMaxWidth()\
                        )' app/src/main/java/com/example/features/settings/SettingsScreen.kt
