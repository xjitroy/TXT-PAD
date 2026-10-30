# TXT theme and file-action fixes

The supplied phone screenshot showed black default Text/Icon content on a dark mesh.
MaterialTheme did not establish LocalContentColor for bare content. A root Material 3
Surface now provides onSurface content color. Native password inputs also receive theme
text/hint colors. System status/navigation icon appearance follows the system theme.

Glass highlights now draw behind foreground content, the mesh tint is restrained, and
only the dedicated sibling canvas receives Gaussian blur. Navigation uses a fade instead
of scaling text layers. Home has wrapping filters, readable badges, labelled Backup /
Workspace actions and an explicit Trash entry. At enlarged font scales action buttons
stack at full width. The New File label uses a persistent layout without visibility animation.

File actions: Open, Rename (same extension), Move (workspace/vault), confirmed Trash,
Restore (keep both on conflict), confirmed permanent Delete. Trash uses private disk
storage and verified raw-file copies, so locked files remain encrypted. Trash is excluded
from backup, stated in the UI and README. Destructive operations run off the main thread.

Validation: GitHub Actions run 41019504444 on source commit
231ad67b0963dd8da970dc43184c02bf7a7f6b1c passed app/test compilation, crypto unit tests,
Android lint and seven Android 15 emulator tests. Screenshots were downloaded and viewed.
Both root foreground/surface pairs exceed 7:1 in the deterministic fallback palette test.
The visible large-text Workspace word break found during review was fixed and rechecked.
No crash entries were captured in the emulator crash buffer. Test data is synthetic.

Limits: screenshots test the fallback palette, not every OEM dynamic palette. Screen-reader
flows, physical devices, secure erase, independent security review and performance
benchmarks were not certified. Release is debug-signed preview-7, not production-signed.
