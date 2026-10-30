#!/usr/bin/env bash
# Keep screenshots and crash evidence even if an instrumentation assertion fails.
set -uo pipefail
adb shell settings put secure show_ime_with_hard_keyboard 1
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
if [ "$TXT_QA_EXIT" -eq 0 ]; then
    adb install -r app/build/outputs/apk/debug/app-debug.apk > /dev/null
    adb shell settings put global animator_duration_scale 1
    adb shell cmd uimode night yes
    adb shell am start -n com.secure.textpad/.MainActivity > /dev/null
    sleep 2
    adb shell screenrecord --time-limit 8 --bit-rate 2000000 --size 540x960 /sdcard/TXT-glass-motion.mp4 || true
    adb pull /sdcard/TXT-glass-motion.mp4 app/build/outputs/ui-screenshots/ || true
fi
exit "$TXT_QA_EXIT"
