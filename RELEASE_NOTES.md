TXT 1.1.0 — clearer writing, responsive glass and stable releases

- Independent button press/release springs, pressure highlights and focus feedback. Buttons no longer share one press state.
- A refined Material You liquid backdrop with two flowing light ribbons, softer localized color and directional glass reflections. The writing page has a quieter, more opaque surface and an active-line guide. Foreground text never enters blur nodes.
- Moving glass can be disabled in Settings, reacts to Android's animation scale and stops in the background. Reduced-motion navigation preserves the same controls and workflow.
- Launch-window colors and system icons follow Light/Dark, including legacy Android resource qualifiers. Save-state labels use a short transition without wrapping away the editor viewport. The keyboard status line is compact.
- Large-font Undo/Redo captions grow without clipping, and primary editor actions wrap into two rows when needed. Bengali auto filenames retain vowel signs and combining accents. DOCX entity references and emoji round-trip correctly.
- Auto-save defaults on; Undo/Redo reveals changed text and survives saving. Custom filenames/extensions remain available in Tools → Save As.
- SDK target 36, min Android 8. Minified release APK and AAB use a stable production certificate. Source, build workflows, privacy/listing assets, mapping, checksums and maintainer handoff are public; private signing material is delivered separately to the owner.

Back up private files before uninstalling an old debug preview: its signing key differs. Uninstalling erases the private vault and Trash. DOCX support keeps paragraph text, not formatting or images. Physical-device and Play Console policy/review checks remain necessary; no independent security audit or 120 Hz hardware certification is claimed.
