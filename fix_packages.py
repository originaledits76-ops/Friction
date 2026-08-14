import os
import re

for root, _, files in os.walk('./app/src/main/java/com/example'):
    for file in files:
        if file.endswith('.kt'):
            path = os.path.join(root, file)
            with open(path, 'r') as f:
                content = f.read()
            
            # The goal is to extract the actual package from the file path
            # and replace whatever mess is at the top of the file with the correct package.
            rel_path = os.path.relpath(path, './app/src/main/java/')
            pkg_name = os.path.dirname(rel_path).replace('/', '.')
            
            # Now we need to remove all existing package declarations and the bad imports
            # Let's just find the first "import " and everything before it should be the package.
            # But wait, there are multiple "package" and "import" messes.
            # Let's remove any line starting with "package "
            lines = content.split('\n')
            new_lines = []
            package_found = False
            for line in lines:
                if line.startswith('package '):
                    continue
                elif line.startswith('import com.example.core.widgets.ResponsiveText.core') or \
                     line.startswith('import com.example.core.widgets.ResponsiveText.features') or \
                     line.startswith('import com.example.core.widgets.ResponsiveText'):
                    # Skip these bad imports
                    continue
                else:
                    new_lines.append(line)
            
            # Clean up empty lines at the top
            while new_lines and new_lines[0].strip() == '':
                new_lines.pop(0)
            
            # Now, does it need ResponsiveText?
            content_str = '\n'.join(new_lines)
            needs_import = 'ResponsiveText(' in content_str
            
            final_content = f"package {pkg_name}\n\n"
            if needs_import:
                final_content += "import com.example.core.widgets.ResponsiveText\n"
            
            final_content += content_str
            
            with open(path, 'w') as f:
                f.write(final_content)

