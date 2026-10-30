#!/usr/bin/env python3
"""Regenerate the source bundle in the owner's requested nine-file order."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
files=['app/build.gradle.kts','app/src/main/AndroidManifest.xml','app/src/main/java/com/secure/textpad/ui/theme/LiquidGlassTheme.kt','app/src/main/java/com/secure/textpad/security/CryptoEngine.kt','app/src/main/java/com/secure/textpad/data/DocxHandler.kt','app/src/main/java/com/secure/textpad/data/StorageRepository.kt','app/src/main/java/com/secure/textpad/ui/home/HomeScreen.kt','app/src/main/java/com/secure/textpad/ui/editor/EditorScreen.kt','app/src/main/java/com/secure/textpad/MainActivity.kt']
parts=['# TXT — nine requested source files in order\n\nFull configuration, resources, tests and release instructions are in the repository.\n']
for i,file in enumerate(files,1):
 language='xml' if file.endswith('.xml') else 'kotlin'
 parts.append(f'\n## {i}. {file}\n\n```{language}\n{(root/file).read_text().rstrip()}\n```\n')
for file in ['app/src/main/java/com/secure/textpad/ui/theme/GlassControls.kt','app/src/main/java/com/secure/textpad/security/SessionPassword.kt']:
 parts.append(f'\n## Supporting source: {file}\n\n```kotlin\n{(root/file).read_text().rstrip()}\n```\n')
(root/'SOURCE_CODE.md').write_text(''.join(parts))
print('Refreshed SOURCE_CODE.md')
