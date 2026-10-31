# TXT — offline text editor and document vault

**[Versioned releases: signed APK, AAB and verification](https://github.com/xjitroy/TXT-PAD/releases)**

Kotlin / Jetpack Compose Material 3. Android 8.0+ (API 26), target/compile API 36.
The Android app declares no INTERNET or ACCESS_NETWORK_STATE permission. No analytics,
ads, remote APIs, account requirement or cloud backup integration.

## Build

Open this directory in Android Studio with JDK 17 or 21 and install Android SDK 36.
Let Gradle sync, then run the `app` configuration. On Linux/macOS with curl, unzip
and sha256sum: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintRelease`.
The standard Gradle wrapper (including gradlew.bat on Windows) pins Gradle 8.11.1 with a
distribution SHA-256; its JAR was checked against Gradle's official published checksum. Build dependencies
need network on first sync; the installed app works offline. Alternatively use a local
Gradle 8.11.1 installation. Production updates must reuse the stable TXT signing identity. See [RELEASING.md](docs/RELEASING.md); its private key is not in this repository.

The [ordered source bundle](SOURCE_CODE.md) contains the nine requested files and the
supporting RAM credential implementation. The full repository also includes resources,
configuration, unit tests and emulator tests.

## Use

Choose a workspace folder. Tap **New File** to start typing with the keyboard ready.
**Auto-save is on by default**, after a 1.5-second pause in typing. A new draft is named
from its first nonempty line or a timestamp. Save status appears in the title bar. Typing
remains enabled during saves, and Back waits for late keyboard commits to be saved before
leaving. Unsaved existing documents still have a Save / Discard / Keep editing guard.

**Undo / Redo** is visible in the editor header and also available in Tools. It selects
the changed text (or moves the caret to a deletion), scrolls that location into view, and
keeps history after auto-save. With the keyboard open, the editing viewport resizes and
reveals the caret. Optional line numbers use the same vertical scroll owner as the text.

Light/dark appearance follows Android, including system-bar icons. Material You accents
remain automatic on Android 12+. Animated light ribbons are sampled into frosted glass
panels, with background-only Gaussian blur, directional reflections, inner highlights,
soft shadows and spring press feedback. **Foreground text and icons never enter the blur
node.** Android 8–11 uses a translucent fallback. Disabling system animation scale or Settings → Animated glass stops
the moving mesh. Motion also stops while the app is in the background. Individual buttons
have independent pressure/release and focus feedback; the writing surface is more opaque
than the surrounding glass to protect reading contrast. Files / Vault / Trash has a bottom glass dock; the main editor toolbar
floats above the keyboard. Tools contains font, wrap, line-number and storage controls.
Home filters wrap, and large-font Backup/Workspace controls stack to keep labels readable.

Each file's **⋮** menu provides Open, Rename, Move and Move to Trash. Rename keeps the
current extension. Move transfers between the active workspace and private vault. Trash
stores verified raw bytes in private app storage, preserving STXT encryption. Open Trash
from the bottom dock to Restore or permanently Delete with confirmation.
Restore keeps both files on filename conflicts; public files return to the active workspace.
Trash is excluded from backups: restore needed files before exporting. Trash is removed
on uninstall, together with the private vault.

**Tools → Save As…** accepts your own filename and extension: `index.html`, `script.php`,
`notes.txt`, `document.docx` or another text extension. Choose a destination in Android's file picker.
Password protection is selectable; `.stxt` / `.enc` requires it. DOCX exports paragraph
text only. After a successful Save As, auto-save uses the new file; the original remains.
Custom extensions saved into the workspace are remembered for future file-list scans.
Read-only external files can be exported to another location using Save As.

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

Editor input is bounded to 12 MiB encoded and 2 million UTF-16 code units. The editor
uses a gutter sized to the visible viewport. Exceptionally long unwrapped lines continue
visually at 32,760 physical pixels to stay within Android layout limits; text is unchanged.
Undo history
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
guaranteed erased. For encrypted auto-save, the password is sealed in RAM under an AndroidKeyStore AES key.
Only its ciphertext remains between saves; transient mutable password buffers are wiped
on use. The sealed token is cleared on document close and is never saved to disk. This
supports auto-save after an explicit password unlock; it is not password recovery.
FLAG_SECURE protects encrypted screens and password dialogs, including busy protected
read/write transitions and the complete editor exit animation. The window remains protected
until a safe frame has committed. Optional app lock is an access gate, not encryption at rest
for plain files.

Vault saves use fsync + same-directory atomic rename. SAF providers vary: updates use a
private rollback copy and restore it after reported failures, but provider writes are not
universally atomic across process death, power failure or media removal. Restore rolls
back reported failures; it does not claim a distributed transaction across arbitrary SAF
providers. Recovery copies are retained if rollback fails. SAF modification timestamps
cannot generally be set; original times remain in the manifest, and vault times are restored.

Verification results and exact build/release links are recorded in [VERIFICATION.json](VERIFICATION.json).
The instrumented suite runs on Android 8 (API 26) and Android 16 (API 36). It covers
fallback light/dark/large text, menus, root foreground contrast, editor tools, line-number/
keyboard caret visibility, Undo/Redo location, typing during saving, default plain auto-save,
encrypted auto-save, late IME commits on Back, RAM credential clearing, arbitrary extension
output, button feedback, native DOCX, archive conflicts/tampering/path rejection and Trash.
Exact pass/fail results, build identifiers and release verification are recorded in
[VERIFICATION.json](VERIFICATION.json); do not infer a result only from this coverage list.

The versioned release provides a minified, stably signed APK for direct installation and an
AAB for Play Console, R8 mapping, signatures/checksums and store assets. See
[Play submission files](docs/PLAY_STORE.md), [privacy policy](PRIVACY.md),
[architecture](docs/ARCHITECTURE.md) and [future-agent handoff](docs/HANDOFF.md).
The public repository contains no private signing key or password.

Physical-device/OEM behavior, TalkBack workflows, cold-start and 120 Hz hardware performance
and independent cryptographic review are not certified by emulator checks. Play Console
account verification, app forms, testing requirements and review are completed by the owner.
Older debug previews used different signing keys: before uninstalling one, export/verify
an offline backup and restore needed Trash items. Future direct APK updates use the stable
production certificate and increasing versionCode.
