@echo off
setlocal

set APP_NAME=Orca
set APP_VERSION=1.0
set ICON=..\icons\orca.ico
set RUNTIME=..\runtime\runtime
set INPUT=..\..\build\libs
set MAIN_JAR=orca-all.jar
set MAIN_CLASS=orca.cli.OrcaCLI

echo Building Windows EXE installer...

jpackage ^
  --type exe ^
  --name %APP_NAME% ^
  --app-version %APP_VERSION% ^
  --input %INPUT% ^
  --main-jar %MAIN_JAR% ^
  --main-class %MAIN_CLASS% ^
  --runtime-image %RUNTIME% ^
  --win-menu ^
  --win-shortcut ^
  --icon %ICON%

echo.
echo Installer built successfully.
