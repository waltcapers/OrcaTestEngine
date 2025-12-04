@echo off
echo Generating minimal JVM runtime...

jlink ^
  --add-modules java.base,java.logging,java.management,java.xml ^
  --strip-debug ^
  --no-header-files ^
  --no-man-pages ^
  --compress=2 ^
  --output runtime

echo Runtime image created at packaging\runtime\runtime
