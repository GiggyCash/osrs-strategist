# Compass Knowledge Foundation v1

This local foundation proves one deliberately small knowledge path for the six
ordinary F2P platebodies already consumed by Compass Intelligence Foundation v1.
It does not mirror the Wiki, add runtime networking, or turn Wiki prose into
strategy policy.

The three quest-data failures recorded in the historical validation below were
subsequently resolved by commit `845d4b7`.

## Boundary

The acquisition layer is development-only. `knowledge_pipeline.py fetch` asks the
OSRS Wiki Action API for thirteen explicitly reviewed pages: six platebodies, six
matching bars, and the hammer. It reads the pages' `Infobox Item` and `Recipe`
templates and writes a review candidate. No fetch runs in RuneLite or as part of
normal recommendation execution.

The checked-in raw snapshot records exact page titles and URLs, MediaWiki revision
IDs and timestamps, SHA-256 hashes of the reviewed wikitext, the Wiki license, and
the extracted item/recipe parameters. It keeps source facts such as item IDs,
membership, stackability, Smithing level, material quantity, tool, facility,
output quantity, ticks, and XP. Ticks and XP are retained for audit even though
the current reasoning path does not use them.

The normalizer projects only facts used by the existing catalogs. It updates the
matching rows in `reviewed-action-recipes.json` and the corresponding recipes in
`method-intelligence.json`. It does not generate banking behavior, purchase
policy, batch preference, location strategy, economic value, or ranking weights.
Those remain reviewed Compass data or policy. Sync refuses to create a target
that Compass has not explicitly modeled.

At runtime, the existing `ReviewedActionRecipeCatalog`,
`MethodIntelligenceCatalog`, `MethodReadinessService`, and
`TrainingMethodSelector` consume the bundled catalogs without network access.
The steel-to-mithril level boundary proves that source-derived facts change the
retained recipe candidate, guidance action, and alternative comparison through
the existing Intelligence Foundation.

## Validation

The development validator rejects malformed provenance, missing pages, duplicate
recipe identities, contradictory item/recipe facts, unsupported recipe shapes,
missing Compass targets, and catalog drift. Existing Java catalog validation then
checks the bundled representation at runtime boundaries. The content-freshness
manifest records this reviewed snapshot as a structured six-record family.

## Review and refresh procedure

1. Fetch a candidate without touching reviewed data:

   `python3 scripts/knowledge_pipeline.py fetch --output /tmp/smithing-platebodies.json`

2. Compare the candidate with
   `knowledge/raw/smithing-platebodies.json`. Review changed Wiki revisions and
   semantic facts; do not accept a revision change solely because it is newer.
3. Replace the checked-in raw snapshot only after that review, update its
   `reviewedDate`, and run:

   `python3 scripts/knowledge_pipeline.py validate`

4. Copy the reviewed factual projection into the existing catalogs:

   `python3 scripts/knowledge_pipeline.py sync`

5. Run `python3 scripts/knowledge_pipeline.py check`, the Python failure-mode
   tests, focused Java integration tests, and the normal repository validators.

Future recipe families can add a small manifest and an acquisition adapter for
the Wiki template shape they actually use. They should project into an existing
Compass catalog and use existing reasoning models. A future backend can replace
the fetch side while producing the same reviewed snapshot; the runtime catalog
and reasoning engine need no corresponding redesign.

## Limits

This proof supports one material, one tool, one facility, and one output per
recipe. It deliberately rejects more complex Wiki recipe shapes. It does not
automatically approve semantic changes, infer training value, import prices,
cover smelting or special Smithing facilities, or broaden method eligibility.
Those require separate reviewed facts or policy decisions.

## Local evidence

The acquisition command fetched the thirteen declared pages on 2026-10-01; its
candidate matches the checked-in snapshot byte-for-byte. Eight Python tests cover
the valid projection plus malformed provenance, missing pages, duplicate IDs,
manifest drift, contradictory facts, unsupported recipe shapes, and catalog
drift. Eleven focused Java tests cover the knowledge boundary, existing Smithing
readiness, and shared recipe contracts.

JDK 11 `./gradlew --offline --no-daemon clean test jar --continue` built the JAR
and ran 892 tests: 889 passed and only the same three quest-data baseline failures
remained. The standalone content census ran 5 tests with only its known quest
manifest failure. The refresh workflow passed freshness (15 families), strategy
sources (53 sources), the six-record knowledge projection, and the content-change
self-test before reaching the same three expected quest failures. The privacy
scan and `git diff --check` passed.

Production Java growth for this goal is exactly zero lines, zero characters, zero
estimated tokens, and zero files. Development-only reviewed knowledge adds 313
lines / 9,609 bytes; the acquisition/check tool adds 383 Python lines. Runtime
catalog facts are unchanged apart from six review-date substitutions. Adding the
freshness-family record increases bundled source data by 257 bytes; the built JAR
is 1,236,912 bytes, 96 bytes above the Intelligence Foundation baseline JAR.
