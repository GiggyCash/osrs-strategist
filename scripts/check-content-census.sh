#!/usr/bin/env sh
set -eu

./gradlew test --tests '*ContentCoverageManifestTest'

# Keep the human-readable dispositions visible in local/CI maintenance output.
sed -n '/| Family |/,/^$/p' docs/CONTENT_CENSUS.md
