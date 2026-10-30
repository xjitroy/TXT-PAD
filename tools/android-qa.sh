#!/usr/bin/env bash
# Keep screenshots and crash evidence even if an instrumentation assertion fails.
set -uo pipefail
gradle :app:connectedDebugAndroidTest --stacktrace
TXT_QA_EXIT=$?
mkdir -p app/build/outputs/ui-screenshots
python3 - <<'PY_QA'
from pathlib import Path
import shutil
out = Path('app/build/outputs/ui-screenshots')
for file in Path('app/build/outputs').rglob('*'):
    if file.is_file() and file.suffix in {'.png', '.txt'} and 'additional' in str(file) and file.parent != out:
        shutil.copy2(file, out / file.name)
print('Captured screenshots:', len(list(out.glob('*.png'))))
PY_QA
adb logcat -d -b crash > app/build/outputs/ui-screenshots/crash-log.txt || true
exit "$TXT_QA_EXIT"
