#!/bin/bash
cat << 'INNER_EOF' > app/src/main/java/com/example/features/settings/SettingsScreen.kt
$(cat app/src/main/java/com/example/features/settings/SettingsScreen.kt | sed '/com.example.core.widgets.FrictionButton(/,/}/d')
INNER_EOF
