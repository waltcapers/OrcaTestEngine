#!/usr/bin/env bash
set -e

APP_NAME="Orca"
APP_VERSION="1.0"
ICON="../icons/orca.icns"
RUNTIME="../runtime/runtime"
INPUT="../../build/libs"
MAIN_JAR="orca-all.jar"
MAIN_CLASS="orca.cli.OrcaCLI"

echo "📦 Building macOS DMG installer..."

jpackage \
  --type dmg \
  --name "$APP_NAME" \
  --app-version "$APP_VERSION" \
  --input "$INPUT" \
  --main-jar "$MAIN_JAR" \
  --main-class "$MAIN_CLASS" \
  --runtime-image "$RUNTIME" \
  --icon "$ICON" \
  --mac-package-identifier "com.orca.testengine" \
  --mac-package-name "$APP_NAME" \
  --mac-package-signing-prefix "Developer ID Application"

echo "✅ macOS DMG built."
