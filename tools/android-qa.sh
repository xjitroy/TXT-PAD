#!/usr/bin/env bash
# Keep screenshots and crash evidence even if an instrumentation assertion fails.
set -uo pipefail
gradle :app:connectedDebugAndroidTest --stacktrace
TXT_QA_EXIT=$?
mkdir -p app/build/outputs/ui-screenshots
adb pull /sdcard/Android/data/com.secure.textpad/files/qa/. app/build/outputs/ui-screenshots/ || true
adb logcat -d -b crash > app/build/outputs/ui-screenshots/crash-log.txt || true
exit "$TXT_QA_EXIT"
