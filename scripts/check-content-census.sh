#!/usr/bin/env sh
set -eu

./gradlew test --tests '*ContentCoverageManifestTest'
