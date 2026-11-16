#!/bin/bash
set -e

# ==============================================================================
# NovaCut Studio APK Downloader & Installer
# Copyright (c) 2026 Master H. All Rights Reserved.
# ==============================================================================
REPO="${NOVACUT_REPO:-newcountrymail-prog/NovaCut}"
TOKEN="${GITHUB_TOKEN:-${GH_TOKEN:-}}"
if [ -z "$TOKEN" ] && [ -f "${HOME}/.github_token" ]; then
  TOKEN=$(cat "${HOME}/.github_token" | tr -d '\r\n ')
fi
OUTPUT_DIR="${HOME}/novacut_apks"
DOWNLOADS_DIR="/sdcard/Download"

echo "=========================================="
echo "  NovaCut Studio — APK Downloader"
echo "  Copyright (c) 2026 Master H"
echo "=========================================="

mkdir -p "$OUTPUT_DIR"
cd "$OUTPUT_DIR"

echo "[1/4] Checking GitHub Actions artifacts for ${REPO}..."
ARTIFACT_URL=$(curl -s -H "Authorization: Bearer ${TOKEN}" \
  -H "Accept: application/vnd.github+json" \
  "https://api.github.com/repos/${REPO}/actions/artifacts" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    artifacts = data.get('artifacts', [])
    candidates = ['NovaCut-Android-Debug-APK', 'NovaCut-APK', 'NovaCut-Release-APK']
    target = None
    for name in candidates:
        target = next((a for a in artifacts if a.get('name') == name and not a.get('expired')), None)
        if target:
            break
    if target:
        print(target['archive_download_url'])
    else:
        print('')
except Exception:
    print('')
")

if [ -z "$ARTIFACT_URL" ]; then
  echo "[-] APK artifact is compiling or repository is pending."
  echo "    Track build status at: https://github.com/${REPO}/actions"
  exit 1
fi

echo "[2/4] Downloading APK package from GitHub Actions..."
curl -L -s -H "Authorization: Bearer ${TOKEN}" \
  -H "Accept: application/vnd.github+json" \
  -o "novacut_apks.zip" \
  "$ARTIFACT_URL"

echo "[3/4] Extracting APK package..."
unzip -o -q "novacut_apks.zip"

echo "[4/4] Copying APK to device /sdcard/Download/ folder..."
if [ -d "$DOWNLOADS_DIR" ]; then
  cp -f *.apk "$DOWNLOADS_DIR/" 2>/dev/null || true
  echo "✓ Copied to $DOWNLOADS_DIR/"
fi

echo "------------------------------------------"
echo "Downloaded APK files in: ${OUTPUT_DIR}"
ls -lh *.apk 2>/dev/null || true
echo "------------------------------------------"
echo "To install on your Android device:"
echo "1. Run: termux-open \$(ls ${OUTPUT_DIR}/*.apk 2>/dev/null | head -n 1)"
echo "2. Or open Files / Downloads app on your phone."
echo "=========================================="
