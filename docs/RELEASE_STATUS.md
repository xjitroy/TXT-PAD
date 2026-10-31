# TXT 1.1.0 release status

The viewport-sized gutter and bounded long-line rendering are undergoing new regression checks. The previous source suite passed 26 cases per API; the new suite has 28 cases per API.

Previous verified source: `9867349e576165969fe71b7fa15176ee4bac593c`.

Signed APK checks are also exercising real persisted SAF selection, encrypted saves, Trash and both backup placement flags. Publication remains gated on passing source and signed-build checks.

The private signing identity is unchanged and stays outside the public repository.
