#!/bin/sh
set -eu

GRADLE_VERSION="9.3.1"
BASE_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
CACHE_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/gradle-${GRADLE_VERSION}-bin/bootstrap"
DIST_DIR="$CACHE_DIR/gradle-${GRADLE_VERSION}"
ZIP="$CACHE_DIR/gradle-${GRADLE_VERSION}-bin.zip"

if [ ! -x "$DIST_DIR/bin/gradle" ]; then
  mkdir -p "$CACHE_DIR"
  if [ ! -f "$ZIP" ]; then
    echo "Downloading Gradle ${GRADLE_VERSION}..."
    curl -fL --retry 3 --connect-timeout 20 \
      "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
      -o "$ZIP"
  fi
  rm -rf "$DIST_DIR.tmp"
  mkdir -p "$DIST_DIR.tmp"
  unzip -q "$ZIP" -d "$DIST_DIR.tmp"
  mv "$DIST_DIR.tmp/gradle-${GRADLE_VERSION}" "$DIST_DIR"
  rmdir "$DIST_DIR.tmp"
fi

exec "$DIST_DIR/bin/gradle" "$@"
