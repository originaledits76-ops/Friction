import os

files = [
    './app/src/main/java/com/example/features/profile/ProfileScreen.kt',
    './app/src/main/java/com/example/features/settings/SettingsScreen.kt',
    './app/src/main/java/com/example/features/feedback/FeedbackScreen.kt',
    './app/src/main/java/com/example/features/onboarding/OnboardingScreen.kt',
    './app/src/main/java/com/example/features/home/HomeScreen.kt'
]

for file in files:
    if os.path.exists(file):
        with open(file, 'r') as f:
            content = f.read()
        
        content = content.replace('Text(', 'ResponsiveText(')
        
        if 'import com.example.core.widgets.ResponsiveText' not in content:
            content = content.replace('import androidx.compose.material3.*', 'import androidx.compose.material3.*\nimport com.example.core.widgets.ResponsiveText')
            
        with open(file, 'w') as f:
            f.write(content)
