#!/usr/bin/env bash
set -e

APP_NAME="orca"
APP_VERSION="1.0"
ICON="../icons/orca.png"
RUNTIME="../runtime/runtime"
INPUT="../../build/libs"
MAIN_JAR="orca-all.jar"
MAIN_CLASS="orca.cli.OrcaCLI"

echo "🐧 Building Linux packages..."

# .deb
jpackage \
  --type deb \
  --name "$APP_NAME" \
  --input "$INPUT" \
  --main-jar "$MAIN_JAR" \
  --main-class "$MAIN_CLASS" \
  --app-version "$APP_VERSION" \
  --runtime-image "$RUNTIME" \
  --icon "$ICON" \
  --linux-shortcut \
  --linux-menu-group "Development"

# .rpm
jpackage \
  --type rpm \
  --name "$APP_NAME" \
  --input "$INPUT" \
  --main-jar "$MAIN_JAR" \
  --main-class "$MAIN_CLASS" \
  --app-version "$APP_VERSION" \
  --runtime-image "$RUNTIME" \
  --icon "$ICON" \
  --linux-shortcut

echo "✅ Linux packages built."
