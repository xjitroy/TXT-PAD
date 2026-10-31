# Architecture

## Entry and state

`MainActivity` is a FragmentActivity for Android BiometricPrompt and owns the system intent
router, SAF activity results and app-lock gate. `PadViewModel` owns operation state, the file
list and one active `EditorDocument`. A document has a stable session UUID; saving its first
URI/title must not recreate the text input. `busy` blocks I/O-sensitive operations; `saving`
is separate so auto-save cannot make typing read-only. A pending navigation waits for all
text revisions committed by the IME to be saved before closing.
Activity recreation restores only an unhandled external URI. A consumed ACTION_VIEW/EDIT
intent is not replayed when the system changes Light/Dark mode or rotates the screen.

The app has no server, database or network permissions. Workspace URI permissions and small
settings live in SharedPreferences. Device backup/transfer is disabled; explicit .tpbak is
used for portable document backup.

## UI

- `LiquidGlassTheme.kt`: system/dynamic palette, lifecycle/animation-scale tracking, shared
  procedural mesh, screen-coordinate background-only RenderNode sampling and Gaussian blur.
  API 26–30 uses translucent surfaces. The Writing style is 96% tinted for stable reading.
- `GlassControls.kt`: independent button press/release spring, highlight and keyboard focus
  feedback around native Material 3 controls. Controls retain native semantics and hit targets.
- `HomeScreen.kt`: wrapping search/filter controls, workspace/vault files, labelled bottom dock,
  file actions, Trash, settings and backup prompts.
- `EditorScreen.kt`: BasicTextField, shared text/gutter scroll, IME caret reveal, bounded history,
  literal/regex search, debounced save and Tools/Save As/password UI. The action bar wraps
  at large UI fonts, history controls grow with labels and gutter width follows digit width.

`WindowManager.FLAG_SECURE` protects encrypted documents and password UI. Do not put a
foreground-containing subtree in a blurred graphicsLayer. Avoid global motion that reruns
text composition each animation frame. Phase changes invalidate background drawing only.

## Storage

`StorageRepository` queries DocumentsContract cursors on I/O. It never calls
DocumentFile.listFiles(). It supports nested relative paths, a private secure_vault directory,
verified raw-file moves and a private Trash. Private writes use same-directory fsync/rename;
SAF writes keep rollback copies but cannot promise atomicity across arbitrary providers or
power failure. Restore verifies all data before destination changes and rolls back reported
write failures. Custom text extensions saved by the app are remembered for scanning.

DOCX parsing/writing is paragraph-text only. Do not advertise formatting or image preservation.
Editor input is bounded to 12 MiB encoded and 2 million UTF-16 units; history is bounded;
backup streams use 64 KiB buffers with metadata/restore-disk limits documented in README.

## Cryptography and session state

STXT: `STXT | 01 | salt16 | IV12 | ciphertext | tag16`. The header is authenticated as AAD.
PBKDF2-HMAC-SHA256: 600,000 iterations, 256-bit AES-GCM key, fresh random salt/IV per save.

TPBK: `TPBK | 01 | salt16 | baseIV12`, then `flag1 | length4BE | ciphertext | tag16` frames.
Expected chunk index, header, flag and length are authenticated. Nonces XOR the final flag
into byte 0 and the 64-bit index into bytes 4..11 of baseIV. A required empty final frame
prevents truncation; trailing bytes and reordering fail. ZIP names are indexed; manifest
paths, sizes, hashes and placement are checked before restore writes.

`SessionPassword` keeps a sealed document password only in RAM under a non-exportable
AndroidKeyStore key so encrypted auto-save can continue during an explicitly unlocked session.
Mutable temporary arrays are wiped. Managed Strings, text input and provider internals cannot
be guaranteed erased. App biometrics gates entry; it does not recover file/backup passwords.

## Tests

The JVM suite exercises file/chunk authentication, freshness, wrong passwords, erasure,
reordering, truncation and trailing data. Instrumented tests cover theme/motion, 2× text,
IME/gutter/history, plain/encrypted auto-save, late commits, custom names, native DOCX,
archive conflicts/damage/path checks and private Trash. The signed-APK smoke script uses
UI XML-derived tap positions and verifies actual saved bytes through a system document URI.
