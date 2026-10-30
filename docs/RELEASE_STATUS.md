# TXT 1.1.0 release status

Source: `9867349e576165969fe71b7fa15176ee4bac593c`.

PASS: source checks on Android 8 and Android 16, 26 cases each, no skips/failures.
https://github.com/xjitroy/TXT-PAD/actions/runs/41106969032

PASS: independent local JDK 17 build, release lint, three crypto unit tests and test APK
compilation. The 29 compilation/configuration/test/resource files match the source tree.
APK and AAB signatures match the pinned production certificate; permissions and native
16 KiB alignment pass. Official Google bundletool 1.18.3 validates the AAB structure.

The exact signed/minified APK smoke test and final publication are the remaining gate.
This includes user-selected persisted SAF folder permissions and backup placement for
both public workspace and private vault files.
`Verify and publish signed artifacts` receives signed public blobs, not the private key,
checks the exact green source run, drives the real APK and publishes only after success.
Diagnostics remain attached to a draft when it fails. VERIFICATION.json has the live scope.

All passwords used by automated UI tests are synthetic fixtures, not user credentials.
Private release signing material is delivered only to the owner outside this repository.
Never regenerate the permanent signing key to continue development. Read RELEASING.md.

Physical-device/OEM behavior, TalkBack, 120 Hz hardware performance and independent
cryptographic review remain unverified. Play account forms and policy review are owner tasks.
