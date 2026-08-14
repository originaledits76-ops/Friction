import os

files = [
    './app/src/main/java/com/example/features/profile/ProfileScreen.kt',
    './app/src/main/java/com/example/features/feedback/FeedbackScreen.kt'
]

for file in files:
    if os.path.exists(file):
        with open(file, 'r') as f:
            content = f.read()
        
        content = content.replace('makeResponsiveText(', 'makeText(')
            
        with open(file, 'w') as f:
            f.write(content)
