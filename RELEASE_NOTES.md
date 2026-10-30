TXT 1.0.2 — visible liquid glass and a stable writing surface

- Animated Material You light ribbons, frosted background-only blur on Android 12+, inset reflections, lens-like background magnification, soft elevation shadows and spring-driven press highlights. Text is always outside the blur layer. Older Android versions use a translucent fallback. This is an original Android implementation inspired by glass interfaces, not Apple's proprietary renderer.
- Floating glass Files / Vault / Trash dock, glass New File action, bottom editor toolbar, glass sheets and dialogs, and animated list placement. System Light/Dark and animation-scale preferences remain supported.
- Keyboard-aware caret scrolling: the current line stays within the resized editor viewport. Line numbers share the same vertical scroll owner. Typing remains enabled during auto-save.
- Auto-save is on by default with a 1.5-second pause after typing. Unlocked encrypted documents can auto-save using a RAM-only sealed credential protected by AndroidKeyStore; mutable passwords are wiped after use. Sealed session data is erased when closing the document. No document password is stored on disk or exported in backups.
- Visible one-tap Undo / Redo moves to and selects the changed region; history survives auto-save.
- Save As prompts for any filename/extension (including .html, .php and custom names) and opens Android's destination picker. Password protection is selectable; DOCX saves preserve paragraph text only.

DEBUG-SIGNED PREVIEW. Back up your vault and needed Trash files before any uninstall. Debug signatures can change between CI builds. Production signing, an independent security review and physical-device performance measurements remain outstanding.
