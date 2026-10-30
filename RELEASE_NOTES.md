TXT 1.0.1 — system theme, readable text and file management

- Light/dark mode follows Android system settings, including status and navigation bars.
- Fixes inherited black text/icons on dark surfaces. Glass highlights render behind content, and the subtle blurred mesh never contains text. Removes scaled text during navigation.
- Refined home screen, wrapping filters, format/security badges and labelled Backup/Workspace actions.
- Per-file menu: Open, Rename, Move between workspace/private vault, and Move to Trash.
- Trash: Restore (keep both on conflict) and confirmed permanent Delete. Locked files retain their password protection. Trash is excluded from backups: restore needed files first.
- Android emulator checks cover light/dark themes, enlarged text, editor tools, rename and plain/encrypted Trash recovery. Screenshots are attached when these checks succeed.

DEBUG-SIGNED PREVIEW. Not a production security release. CI debug signing keys can differ between releases. If Android cannot update the existing installation, export and verify an encrypted backup before uninstalling: uninstall removes the private vault and Trash. Production signing, a security audit and physical-device 120 Hz measurements remain outstanding.
