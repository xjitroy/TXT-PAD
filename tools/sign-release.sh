#!/usr/bin/env bash
# Sign without passing passwords in process arguments. Input paths are public; key paths are private.
set -euo pipefail
: "${TXT_KEYSTORE:?Set TXT_KEYSTORE to the private .p12 file}"
: "${TXT_PASSWORD_FILE:?Set TXT_PASSWORD_FILE to its password file}"
: "${TXT_BUILD_TOOLS:?Set TXT_BUILD_TOOLS to the Android SDK build-tools directory}"
if [ "$#" -ne 3 ]; then
    echo 'Usage: tools/sign-release.sh unsigned.apk unsigned.aab output-directory' >&2
    exit 2
fi
mkdir -p "$3"
"$TXT_BUILD_TOOLS/zipalign" -P 16 -f 4 "$1" "$3/TXT-aligned.apk"
"$TXT_BUILD_TOOLS/apksigner" sign --ks "$TXT_KEYSTORE" --ks-key-alias txt-release --ks-pass "file:$TXT_PASSWORD_FILE" --key-pass "file:$TXT_PASSWORD_FILE" --out "$3/TXT.apk" "$3/TXT-aligned.apk"
rm "$3/TXT-aligned.apk"
cp "$2" "$3/TXT.aab"
jarsigner -keystore "$TXT_KEYSTORE" -storetype PKCS12 -storepass:file "$TXT_PASSWORD_FILE" -keypass:file "$TXT_PASSWORD_FILE" -sigalg SHA256withRSA -digestalg SHA-256 "$3/TXT.aab" txt-release
"$TXT_BUILD_TOOLS/apksigner" verify --verbose --print-certs "$3/TXT.apk"
"$TXT_BUILD_TOOLS/zipalign" -c -P 16 4 "$3/TXT.apk"
jarsigner -verify "$3/TXT.aab"
(cd "$3" && sha256sum TXT.apk TXT.aab > SHA256SUMS)
