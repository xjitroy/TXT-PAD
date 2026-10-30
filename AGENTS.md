# TXT maintainer and agent handoff

This is the real Kotlin / Jetpack Compose Android app, package `com.secure.textpad`.
Read `README.md`, `docs/ARCHITECTURE.md`, `docs/HANDOFF.md` and `docs/RELEASING.md` before changing it.
The current public privacy statement is `PRIVACY.md`.

## Preserve these requirements

- No INTERNET or ACCESS_NETWORK_STATE permission, telemetry, ads, online account or remote API.
- Follow system Light/Dark. Keep foreground text/icons outside all RenderEffect blur nodes.
- Animated glass is on by default, can be disabled in settings, respects Android's animation
  scale, and stops while backgrounded. Each control owns its own interaction source.
- Auto-save defaults on. It must keep typing enabled, keep Undo/Redo history, preserve late
  IME commits on Back and reveal the caret after keyboard, wrap, selection or history changes.
- Preserve STXT v1 and TPBK v1 byte formats. Never weaken PBKDF2 (600,000), AES-GCM tags,
  random salt/IV generation, required final backup frame, staging/hash validation or path checks.
- Vault/Trash are private app data. `.nomedia` is indexing suppression, not access control.
- Passwords/sealed session credentials must not be written to disk. FLAG_SECURE must cover
  passwords, encrypted documents and protected read/write transitions.
- Keep the permanent signing identity. Never generate a replacement key for an update.
  Never commit/upload private keys, keystore passwords, Actions secrets or signing recovery kits.
  Only the public certificate in `release/signing-certificate.pem` belongs in the repository.

## Build and validate

JDK 17, Android SDK/build-tools 36, Gradle 8.11.1. Android 8+ (min 26), target 36.

```sh
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintRelease :app:assembleRelease :app:bundleRelease
```

Run instrumented tests on API 26 and 36 (the `TXT checks` workflow does both). For actual
signed/minified builds run `tools/release-smoke.py` and `tools/check-release.py` as documented.
Check long Bengali/English text, real IME, custom Save As extensions, password round trips,
archive damage rejection, Trash and reduced motion. Inspect compositor screenshots/video.
Do not claim physical-device, screen-reader, 120 Hz or independent-security certification
from an emulator run. Describe the checks that actually ran.

Use `python3 tools/export-sources.py` after source changes to refresh the ordered source bundle.
Update versionCode/versionName, RELEASE_NOTES.md and release evidence for real releases.
Docs/store-only changes can use `[skip ci]`; functional changes need a fresh green workflow.
Do not publish a stable release while tests are failing. Keep APK/AAB/mapping/checksums and
signing metadata together. Owner-only Play Console forms and account details remain outside
source control. Keep verification failures and limitations explicit in the handoff.
