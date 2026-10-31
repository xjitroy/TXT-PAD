# Signing and releasing TXT

The production identity is the public certificate in `release/signing-certificate.pem`.
Its SHA-256 is `8e857dbb69fe4df9759d10f23ebc5da3111cce95a7b8a1efb5ef554057bf1036`.
The private recovery kit is delivered separately to the owner. It must never be in this repo,
release assets, issue comments, workflow artifacts or logs.

## Owner: back up the key

Keep `TXT-release.p12` and `keystore-password.txt` in two safe offline locations. Alias:
`txt-release`. The same password protects the store and key. The kit includes instructions.
Losing the signing identity prevents compatible direct APK updates. Do not regenerate it
just because a build fails. Export a public certificate to compare before signing updates.

Debug previews used temporary keys. Android cannot update those with this production key.
Before replacing a preview, export/verify an encrypted .tpbak backup and restore needed Trash
items; uninstalling removes all private app data. Future directly installed production APKs
use the stable key and increasing versionCode.

## Configure GitHub Actions once (owner account)

Repository → Settings → Secrets and variables → Actions → New repository secret:

- `TXT_KEYSTORE_BASE64`: base64 of the private .p12 file, without line breaks.
- `TXT_STORE_PASSWORD`: content of the private password file (without its newline).
- `TXT_KEY_PASSWORD`: same password.

The connector used for the first release could not manage repository secrets (HTTP 403),
so these are not automatically installed. An owner can set them in GitHub's UI, or with
an authenticated CLI that has Secrets write permission. These commands do not print values:

```sh
base64 < /private/path/TXT-release.p12 | tr -d '\n' | gh secret set TXT_KEYSTORE_BASE64 --repo xjitroy/TXT-PAD
tr -d '\r\n' < /private/path/keystore-password.txt | gh secret set TXT_STORE_PASSWORD --repo xjitroy/TXT-PAD
tr -d '\r\n' < /private/path/keystore-password.txt | gh secret set TXT_KEY_PASSWORD --repo xjitroy/TXT-PAD
```

Do not paste these values into code or public chat. Actions uses them only in a trusted,
manual main-branch workflow, removes the temporary key and verifies the pinned certificate.

## Routine update

1. Implement and test changes. Increase versionCode and versionName in app/build.gradle.kts.
2. Update RELEASE_NOTES.md. Regenerate SOURCE_CODE.md with tools/export-sources.py.
3. Push main. Wait for both API 26 and API 36 `TXT checks` jobs to pass. They also build/shrink
   the release APK/AAB and run unit tests and release lint.
4. Actions → Sign and publish TXT → Run workflow with `v` + versionName. It requires a successful
   TXT checks run on that exact commit, builds with the existing signing secrets, validates
   the artifacts, drives the real release APK on an emulator and publishes only on success.
5. Keep TXT.apk, TXT.aab, mapping.txt, release-metadata.json, public certificate and SHA256SUMS.
6. Upload TXT.aab to Play Console. Keep mapping.txt for deobfuscation. Follow docs/PLAY_STORE.md.

## Manual signing when secrets are unavailable

`TXT checks` retains unsigned signing inputs and QA proof as Actions artifacts after both
test jobs pass. It also attempts a clearly marked public unsigned candidate release. If
GitHub integration permissions reject that optional publication, download the
`TXT-signing-inputs-<run number>` artifact instead; the test results remain authoritative.
Download its TXT-unsigned.apk, TXT-unsigned.aab and mapping.txt. Use JDK 17 and SDK build-tools 36:

```sh
export TXT_KEYSTORE=/private/path/TXT-release.p12
export TXT_PASSWORD_FILE=/private/path/keystore-password.txt
export TXT_BUILD_TOOLS=/path/to/android-sdk/build-tools/36.0.0
tools/sign-release.sh TXT-unsigned.apk TXT-unsigned.aab dist
python3 tools/check-release.py dist/TXT.apk dist/TXT.aab --out dist/release-metadata.json
python3 tools/build-qa-input.py --out /tmp/TXT-qa-input
python3 tools/release-smoke.py --apk dist/TXT.apk --out release-proof --input-helper /tmp/TXT-qa-input/TXT-qa-input.apk
```

The script aligns/signs the APK and JAR-signs the AAB without password command-line arguments.
The separate QA helper uses no network permission or TXT signing key. Its clipboard receiver
requires Android DUMP permission, so only the test shell/system can call it. It sends synthetic
editor input atomically. Shell-launched UiAutomation sets native password fields by hint and
activates their Continue action; encrypted-save/unlock round trips verify acceptance. Never run it on real vault data.

Add `--stress` when changing text layout to include the long-document cases in the signed APK.
Run on an empty test emulator, not a phone holding real vault data. Publish only after checks
pass. A separate `Verify public signed APK` workflow can validate an already uploaded release.

## Google Play App Signing

For a new app, Play App Signing can generate/manage the distribution signing key and accept
this stable key as the upload key. The AAB is uploaded using this key; Play-distributed APKs
then use Google's managed app signing key. Direct GitHub APKs and Play installs may consequently
have different signing identities and cannot necessarily update each other. Choose the long-term
arrangement in Play Console before onboarding; store its app-signing and upload fingerprints.
An existing Play app must use its registered upload key, not a new key from another project.
Never upload the private recovery kit as a Play store listing asset.

## Publish pre-signed files without sharing the key with Actions

If a managed connector cannot call uploads.github.com, keep the key local and use the
public-blob staging route. It sends only signed public artifacts, never the private kit.

1. Complete the exact source commit's TXT checks run. Sign/validate its candidate files.
2. Prepare a public-only directory with TXT.apk, TXT.aab, mapping.txt, SHA256SUMS,
   release-metadata.json, VERIFICATION.json and the two ZIPs from tools/package-public.py.
   The initial verification report should say signed smoke is pending; the publication
   workflow fills its actual result only after it passes.
3. Run tools/stage-release.py with --dir, --source (40-character source SHA), --run (successful
   TXT checks run ID) and --tag. It creates release/publish-request.json. Never point it at
   a signing-kit directory. It refuses private-key suffixes and stages an explicit allowlist.
4. Commit that manifest on main without [skip ci]. Verify and publish signed artifacts
   downloads the public blobs, checks hashes and the pinned certificate, runs the actual minified APK and requires
   green source checks before publication. Source and signed-build verification can run
   concurrently; publication requires both to pass. No signing
   password/key is sent to the runner. Diagnostics remain in a draft when smoke fails.
5. Review the real release proof, update the repository's VERIFICATION.json from the release
   result, and record the build in HANDOFF.md. Public blobs are staging objects; release
   assets are the durable downloads. Future updates reuse the same signing identity.
