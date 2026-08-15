#!/usr/bin/env bash
set -e
printf 'Git: '; git --version || true
printf 'Java: '; java -version 2>&1 | head -1 || true
printf 'Javac: '; javac -version || true
printf '\nProject files:\n'
for f in build.gradle settings.gradle runelite-plugin.properties; do
  if [[ -f "$f" ]]; then echo "  [OK] $f"; else echo "  [MISSING] $f"; fi
done
printf '\nNext: ./gradlew clean test\n'
