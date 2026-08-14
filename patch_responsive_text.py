import re

with open('./app/src/main/java/com/example/core/widgets/ResponsiveText.kt', 'r') as f:
    content = f.read()

content = content.replace('scaledTextStyle.fontSize * 0.9f', 'if (scaledTextStyle.fontSize.value > 8f) scaledTextStyle.fontSize * 0.9f else scaledTextStyle.fontSize')
content = content.replace('readyToDraw = true', 'readyToDraw = true\n            } else if (scaledTextStyle.fontSize.value <= 8f) {\n                readyToDraw = true')

with open('./app/src/main/java/com/example/core/widgets/ResponsiveText.kt', 'w') as f:
    f.write(content)
