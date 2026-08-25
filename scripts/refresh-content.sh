#!/usr/bin/env sh
set -eu

# This workflow validates generated evidence before tests. It deliberately does
# not overwrite hand-authored strategy catalogs or perform runtime networking.
python3 scripts/validate-content-freshness.py
python3 scripts/update-quest-enrichment.py --help >/dev/null
./gradlew test --tests '*QuestItemEvidenceParserTest' \
  --tests '*AuthoritativeQuestRequirementCatalogTest' \
  --tests '*AuthoritativeQuestEnrichmentCatalogTest' \
  --tests '*ContentCoverageManifestTest'
./scripts/check-content-census.sh

printf '%s\n' 'Review upstream diffs and hand-authored semantic changes before committing.'
