# TXT 1.0.2 glass and writing ergonomics

The requested direction retains the simple file-list → editor workflow while making the
background glass visible. Text, native password fields and icons stay sharp. Colors and
system-bar icon appearance follow Android Light/Dark; dynamic palettes use Material You.

## Implementation

- A screen-coordinate mesh of moving radial light and a curved light ribbon is drawn on a
  dedicated background Canvas. Its RenderEffect never includes foreground children.
- Each glass pane records only that shared background into an Android RenderNode on API 31+.
  The sample is capped in size, blurred, slightly magnified, then covered with a 0.72 surface
  tint, directional rim, inset reflection and a soft shadow. API 26–30 uses a translucent
  fallback. Press feedback changes the background lens/highlight, leaving text unscaled.
- The mesh observes system animator scale. Cards animate placement; screen navigation uses
  a short fade and small spring translation. Bottom sheets keep their glass inside the
  animated sheet content to avoid clipping offscreen during expansion.
- Files / Vault / Trash is a labelled glass dock; New File keeps a stable label. The editor
  action bar floats below the canvas, above the keyboard. Undo and Redo are in the header.
- The editor uses a single outer vertical scroll owner for text and the line-number gutter.
  Caret reveal recalculates after text layout, selection, wrapping and IME viewport changes.
  A bottom inset leaves room for the caret and selection handle at the end of a document.
- Auto-save is on by default, debounced 1.5 seconds. Saving has its own state, so text input
  remains writable. Failed saves pause automatic retries and retain the document in memory.
  Back repeats a save if the IME committed text after the first snapshot, then navigates.
- Stable editor session identity prevents the first auto-save filename/URI change from
  recreating the input field. Undo history survives saves and reveals the changed selection.
- Unlocked encrypted documents retain only a RAM credential sealed under AndroidKeyStore.
  Temporary password arrays are opened on I/O for saves, then wiped. Closing clears the
  sealed token. No document password or sealed token is written to disk.
- Save As accepts user names/extensions, asks Android for a destination, and switches
  future auto-save to the successful new file while retaining the original.

## Verification evidence

Source commit `04ae9dbb62fc40b654fcee84460abf6c587ef3ff`, workflow
[41086574711](https://github.com/xjitroy/TXT-PAD/actions/runs/41086574711), passed app/test APK
compilation, crypto unit tests, lint and all 16 instrumented tests with no skips/failures.
The downloaded preview-14 APK hash matches its release digest. Its packaged binary manifest
contains no INTERNET or ACCESS_NETWORK_STATE permission. APK hash is in VERIFICATION.json.
Home light/dark/1.6× and IME/editor screenshots plus motion frames were visually inspected.
The emulator suite includes bilingual long-document IME/caret bounds, synchronized line
numbers, history selection/reveal, writes during saving, plain and encrypted auto-save,
late keyboard commits on Back, custom filename callbacks and actual UTF-8 file output.
Screenshots use synthetic documents. The 8-second motion recording is of the real APK's
first-launch screen with system animations enabled; it is not a recording of every flow.

Root theme contrast assertions cover deterministic surface/foreground pairs, not every
translucent pixel or OEM dynamic palette. Physical devices, screen-reader behavior,
120 Hz performance and independent cryptographic review remain unverified. The published
APK is a debug-signed preview; backup before uninstalling to resolve signature conflicts.
