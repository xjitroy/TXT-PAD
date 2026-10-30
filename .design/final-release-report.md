# TXT 1.1.0 release refinement

## Confirmed findings and changes

The previous implementation reused toolbar/dock interaction sources across buttons, making
independent button pressure feedback impossible. Each Material control now has its own
source and a short interruptible pressure/release spring, a background highlight and focus
ring. Controls retain native semantics; foreground content is not recorded by blur nodes.

The previous backdrop washed much of the dark screen to grey. Light is now localized,
with two slow moving ribbons and a more restrained dynamic palette. The writing surface
uses a 96% tint and a faint active-line guide; surrounding chrome remains translucent.
Critical reading and editing content has a quiet, stable surface rather than changing light
behind every line. The header no longer duplicates the Trash dock action.

The launch window had a static light background. Day/night resources now set the launch
background and system-bar icon flags. Navigation-bar attributes are qualified for API 27+
after release lint caught their use on the API 26 minimum. Motion observes system animation
scale and lifecycle, and a persistent Animated glass setting provides an in-app off switch.
Navigation and custom control movement respect that setting. Save status crossfades briefly
and is a single truncated line, while the IME status strip is compact.

Expanded tests uncovered a DOCX data-loss bug: the token parser ignored entity references
created by the native XML serializer for <, &, > and emoji. It now handles text, CDATA,
entity-reference text and whitespace tokens while still rejecting DTD declarations and
unresolved entities. The Unicode round-trip test remains in the suite.

## Evidence and scope

Exact source commits, API 26/36 test results, signed/minified APK smoke results, APK/AAB
metadata and public proof links are recorded in VERIFICATION.json after validation.
The suite covers 2× text, real IME, gutter/caret position, long code lines, Undo/Redo,
default and encrypted auto-save, late commits, independent button animation, archives,
conflicts, tampering, path rejection, DOCX and Trash. Coverage is not itself a claim of a pass.

The signed-APK smoke script also checks actual auto-saved bytes and authenticated file
re-opening, private movement, Trash/Restore and archive export/restore through system pickers.
Images contain synthetic data. The store artwork is derived from TXT's original vector
mark, and source/asset generation is documented for future maintainers.

No independent security audit, physical-device/OEM matrix, TalkBack flow test, 120 Hz
hardware or Play Console review is implied by emulator/build checks. The long-term private
signing material remains outside GitHub; only its certificate/fingerprint is public.
