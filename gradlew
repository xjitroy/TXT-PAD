#!/bin/sh
# Small Gradle bootstrap for source-only delivery. No downloads occur in the Android app.
set -eu
TXT_GRADLE_VERSION=8.11.1
TXT_GRADLE_CACHE="${GRADLE_USER_HOME:-${TMPDIR:-/tmp}/txt-gradle}/bootstrap"
TXT_GRADLE_DIR="$TXT_GRADLE_CACHE/gradle-$TXT_GRADLE_VERSION"
if [ ! -x "$TXT_GRADLE_DIR/bin/gradle" ]; then
    mkdir -p "$TXT_GRADLE_CACHE"
    TXT_GRADLE_ZIP="$TXT_GRADLE_CACHE/gradle-$TXT_GRADLE_VERSION-bin.zip"
    TXT_GRADLE_URL="https://services.gradle.org/distributions/gradle-$TXT_GRADLE_VERSION-bin.zip"
    curl --fail --location --retry 2 "$TXT_GRADLE_URL" --output "$TXT_GRADLE_ZIP"
    curl --fail --location --retry 2 "$TXT_GRADLE_URL.sha256" --output "$TXT_GRADLE_ZIP.sha256"
    TXT_GRADLE_SHA=$(tr -d '\r\n ' < "$TXT_GRADLE_ZIP.sha256")
    printf '%s  %s\n' "$TXT_GRADLE_SHA" "$TXT_GRADLE_ZIP" | sha256sum --check --status
    unzip -q -o "$TXT_GRADLE_ZIP" -d "$TXT_GRADLE_CACHE"
fi
exec "$TXT_GRADLE_DIR/bin/gradle" "$@"
