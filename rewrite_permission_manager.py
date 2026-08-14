import re

with open('./app/src/main/java/com/example/features/permission/PermissionManagerScreen.kt', 'r') as f:
    content = f.read()

# Make sure we don't accidentally ruin the file if it's large. Let's just create a new script to update it or rewrite it.
