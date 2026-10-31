# Continue TXT development from here

This repository is the complete Android Studio project. It is not a web mockup or a generated
APK-only delivery. The latest versioned release and VERIFICATION.json identify the exact
source/build that was tested. Current publication state: [RELEASE_STATUS.md](RELEASE_STATUS.md). Read AGENTS.md and ARCHITECTURE.md first.

## Where to make changes

| Concern | Source |
| --- | --- |
| Theme colors, background light, blur, writing/chrome tint | LiquidGlassTheme.kt |
| Independent button feedback and focus | GlassControls.kt |
| File list, filters, settings, file menus, Trash | HomeScreen.kt |
| Text input, caret, search, history, Tools and passwords | EditorScreen.kt |
| State, URI routing, app lock, auto-save orchestration | MainActivity.kt |
| SAF/vault/Trash/backup storage and rollback | StorageRepository.kt |
| STXT and chunked TPBK formats | CryptoEngine.kt |
| RAM credential sealing | SessionPassword.kt |
| Text-only DOCX support | DocxHandler.kt |

All Kotlin sources are under app/src/main/java/com/secure/textpad. Tests are under
app/src/test and app/src/androidTest. The theme uses native Android rendering only for
backgrounds, with Compose content on top; do not blur a content subtree to imitate glass.

## Release identity

Package: `com.secure.textpad`. Never change it casually after Play publication. Stable signing
certificate: release/signing-certificate.pem. Its private key and password were delivered to
the owner outside this public repo. Do not generate another signing key for the next version.
GitHub signing secrets require owner setup; the original connector could not manage them.
RELEASING.md has both an automated path and manual signing path.

## Validation and limitations

Check VERIFICATION.json for executed checks, source commit, APK/AAB digests and proof links.
Debug tests and a successful minified-release smoke test have different scopes; preserve both.
A screenshot/semantics assertion is not a physical-device or TalkBack test. No independent
cryptographic audit or 120 Hz hardware certification is implied. Test on actual target phones
before claiming those properties or moving an app through Play Console production review.

DOCX is paragraph-text import/export. Plain private files are sandboxed but unencrypted unless
Lock is used. Workspace providers vary in write/permission behavior. Managed-memory secure
erasure cannot be guaranteed. Private vault/Trash are deleted by uninstall; take verified
portable backups before migrating from old debug previews.

## Useful commands

```sh
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintRelease
python3 tools/export-sources.py
# With a test emulator running:
bash tools/android-qa.sh
# With private signing inputs configured outside the repo:
python3 tools/check-release.py dist/TXT.apk dist/TXT.aab --out dist/release-metadata.json
python3 tools/build-qa-input.py --out /tmp/TXT-qa-input
python3 tools/release-smoke.py --apk dist/TXT.apk --out release-proof --input-helper /tmp/TXT-qa-input/TXT-qa-input.apk
```

Start the next task by inspecting the current branch and release evidence, preserving user
files and existing signing identity, then reproduce the requested issue. Record new tests and
remaining limits honestly. Updating docs alone does not require another APK build. Changing
code, resources, SDKs or dependencies requires new checks and screenshots where relevant.
