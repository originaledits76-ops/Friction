import os
import re

for root, _, files in os.walk('./app/src/main/java/com/example'):
    for file in files:
        if file.endswith('.kt'):
            path = os.path.join(root, file)
            with open(path, 'r') as f:
                content = f.read()
            
            if 'ResponsiveText(' in content and 'import com.example.core.widgets.ResponsiveText' not in content:
                content = re.sub(r'^(package .*?)$', r'\1\n\nimport com.example.core.widgets.ResponsiveText', content, count=1, flags=re.MULTILINE)
                
                with open(path, 'w') as f:
                    f.write(content)
