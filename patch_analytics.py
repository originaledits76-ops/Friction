import re

with open('./app/src/main/java/com/example/features/analytics/AnalyticsScreen.kt', 'r') as f:
    content = f.read()

# Add import
if 'import com.example.core.widgets.ResponsiveText' not in content:
    content = content.replace('import androidx.compose.material3.*', 'import androidx.compose.material3.*\nimport com.example.core.widgets.ResponsiveText')

# We must also remove AI terminology if it's there.
# Let's check for any AI terminology inside strings.
# But it's usually inside AiResponseRenderer or the repository?
# "The user-facing application must NEVER reveal: AI model names, LLM names, backend providers, API providers, Firebase implementation details, database implementation, internal architecture, model/version information, developer/debug information"

with open('./app/src/main/java/com/example/features/analytics/AnalyticsScreen.kt', 'w') as f:
    f.write(content)
