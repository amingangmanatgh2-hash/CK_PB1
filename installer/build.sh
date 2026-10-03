#!/usr/bin/env bash
# =====================================================================
# CK_PB1 installer packaging (run in CI on linux with JDK 17 + NSIS)
# Produces:
#   dist/CK_PB1_Setup.exe          (the real Windows installer)
#   dist/CK_PB1-v1.0.0-Setup.exe   (release asset naming)
# =====================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/installer"
APP_VERSION="${CKPB1_VERSION:-1.1.0}"

echo "== CK_PB1 installer build (v${APP_VERSION}) =="

command -v makensis >/dev/null || { echo "ERROR: makensis not found"; exit 1; }
[ -n "${JAVA_HOME:-}" ] || { echo "ERROR: JAVA_HOME not set"; exit 1; }

rm -rf build dist
mkdir -p build dist

echo "-- generating icons"
python3 gen_icon.py

echo "-- creating bundled Java runtime (jlink)"
"$JAVA_HOME/bin/jlink" \
  --add-modules java.base,java.desktop,java.net.http,java.logging,java.management,jdk.crypto.ec,jdk.unsupported,jdk.zipfs \
  --strip-debug --no-header-files --no-man-pages --compress=2 \
  --output build/jre

echo "-- building CK_PB1.exe launcher stub"
makensis -V2 ckpb1-app.nsi

echo "-- building CK_PB1_Setup.exe"
makensis -V2 ckpb1.nsi

echo "-- preparing release asset name"
cp dist/CK_PB1_Setup.exe "dist/CK_PB1-v${APP_VERSION}-Setup.exe"

echo "== installer artifacts =="
ls -la dist/
