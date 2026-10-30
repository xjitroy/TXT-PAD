# TXT — offline text editor and document vault

Kotlin / Jetpack Compose Material 3. Android 8.0+ (API 26), target/compile API 35.
The Android app declares no INTERNET or ACCESS_NETWORK_STATE permission. No analytics,
ads, remote APIs, account requirement or cloud backup integration.

## Build

Open this directory in Android Studio with JDK 17 or 21 and install Android SDK 35.
Let Gradle sync, then run the `app` configuration. On Linux/macOS with curl, unzip
and sha256sum: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.
The source-only bootstrap downloads and checksum-checks Gradle 8.11.1. Build dependencies
need network on first sync; the installed app works offline. Alternatively use a local
Gradle 8.11.1 installation. Release signing must use your own keystore.

## Use

Choose a workspace folder. Tap **New File** to start typing. Back automatically names
and saves a new plain draft using its first nonempty line or a timestamp. Existing
documents ask before discarding unsaved edits. Save displays its state in the title bar.
The main toolbar scrolls horizontally on small screens; Tools opens the extended controls.
Tools → Save As creates a named copy in the current storage location in TXT, DOCX or STXT format.
Move a saved file to the workspace to export it from the private vault.

Settings → Fingerprint / screen lock is **off by default**. Enabling/disabling requires
hardware-backed AndroidKeyStore authentication. Android 11+ supports device credential
fallback; Android 8–10 requires a strong enrolled biometric. App lock gates TXT on return
from the background. File encryption still needs the file password; biometrics do not
recover a forgotten password or unlock backups on a different device. Password fields
support Android Autofill. An installed third-party Autofill provider may have its own
network behavior outside TXT's process.

Backup exports a password-encrypted .tpbak through the system file picker. Choose a
local provider for entirely local storage; a user-selected cloud document provider is
controlled by that provider. Save the backup outside this phone before a reset. Restore
preserves nested relative paths and private/workspace placement. It verifies authentication
and all hashes in private disk staging before writing destinations. Keep both is the default.
STXT files retain their own passwords inside backups. Device-bound biometric keys and
workspace grants are intentionally not exported. Choose a new workspace on a new phone.

## Formats and limits

STXT v1: `STXT | version=1 | salt(16) | IV(12) | ciphertext | GCM tag(16)`.
All 33 header bytes are authenticated as AAD. PBKDF2-HMAC-SHA256 uses 600,000
iterations and 256 bits. A new salt/IV is generated on every encrypted save.

TPBK v1: `TPBK | version=1 | salt(16) | baseIV(12)`, followed by frames:
`flag(u8) | plaintextLength(u32 BE) | ciphertext | tag(16)`.
Data frames contain 1..65,536 bytes; the required final frame has flag 1 and length 0.
The nonce XORs the final flag into baseIV byte 0 and the monotonically increasing
64-bit index into bytes 4..11. AAD binds header, expected index, flag and length.
Missing final frames, chunk reorder, changed flags, damaged tags and trailing bytes fail.
ZIP entries are indexed names, with validated original paths in manifest.json; traversal,
duplicate paths, duplicate entries, size/hash mismatches and unsupported versions fail.

Editor input is bounded to 12 MiB encoded and 2 million UTF-16 code units. Undo history
is bounded to 100 steps / approximately 16 MiB of retained text. Search is capped at 10,000
matches, has a time budget, and uses literal replacement text even in regex mode.
Backup payload memory uses bounded buffers, with a manifest limited to 10,000 files.
Restore needs free private disk space for staging plus rollback copies; it caps decoded
payload at 100 GiB. These are deliberate limits, not promises of arbitrary size support.
DOCX is text/paragraph import and export: images, layout, formatting, tables and macros
are not preserved. Non-UTF-8 and binary content is rejected instead of silently corrupted.

## Security and verification boundaries

Private vault isolation protects against ordinary other apps; root, compromised firmware,
debug access and physical extraction are outside that guarantee. `.nomedia` suppresses
media indexing and is not access control. Uninstalling TXT removes vault files. Device
transfer and automatic OS backups are disabled. Use the explicit encrypted backup feature.

Mutable passwords, key bytes and crypto buffers are erased best-effort. JVM strings,
Compose editor text, IME state, JCA provider key copies and OS Autofill buffers cannot be
guaranteed erased. FLAG_SECURE protects encrypted screens and password dialogs, including
busy read/write transitions. Optional app lock is an access gate, not encryption at rest
for plain files.

Vault saves use fsync + same-directory atomic rename. SAF providers vary: updates use a
private rollback copy and restore it after reported failures, but provider writes are not
universally atomic across process death, power failure or media removal. Restore rolls
back reported failures; it does not claim a distributed transaction across arbitrary SAF
providers. Recovery copies are retained if rollback fails. SAF modification timestamps
cannot generally be set; original times remain in the manifest, and vault times are restored.

GitHub Actions run [41014123255](https://github.com/xjitroy/TXT-PAD/actions/runs/41014123255)
successfully executed `gradle :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`
on commit `a020f64dd70363494fd159d8d7b8fbae2b3b498e`. The debug APK and SHA-256 file
are published in [preview-2](https://github.com/xjitroy/TXT-PAD/releases/tag/preview-2).
Source XML and manifest permission checks also passed. Crypto tests cover file round trips,
password erasure, new salt/IV, wrong passwords, changed tags, chunk reordering, truncation,
trailing data, and empty backups. Device/emulator flows, screen-reader accessibility,
cold-start, 120 Hz pacing and independent security review remain unverified.
This is a debug-signed preview; stable production signing and device QA are required
before a production release. No bug-free or production certification is claimed.
