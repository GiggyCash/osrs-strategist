# Maintenance Plan for a Non-Coder Owner

## Rule 1: the plugin must not rewrite itself
Self-modifying production code would make review, debugging, and RuneLite Plugin Hub distribution harder.

## Rule 2: make game knowledge easy to update
Long-term game content should live in structured data where possible. The Java engine remains stable while activities, requirements, XP estimates, clue items, and new content are updated separately.

## Rule 3: automate detection, not blind publication
Future GitHub Actions should:
- build on every push
- run strategy tests
- detect stale RuneLite compatibility
- flag changed game data
- open/update a report for review

Do not let an automated scraper silently publish strategy changes.

## Your normal update workflow
1. Jagex updates OSRS.
2. Automated checks or community reports flag likely affected content.
3. You send the repo/error/update note to ChatGPT.
4. We update game data or code.
5. Tests run.
6. You test in the development client.
7. Push the reviewed change.
8. Update Plugin Hub commit when needed.

## Regression tests we must build
At minimum:
- rich Main should prefer sensible GE purchases
- poor Main should compare money making, liquidation, and gathering
- Iron should never recommend GE
- GIM storage ON should use observed Group Storage
- GIM storage OFF should behave like Iron
- UIM without POH storage must not be told to use it
- UIM with unknown Tool Leprechaun contents must not assume tools are there
- UIM with confirmed Tool Leprechaun tools may count them for farm-run prep
- clue dislike should lower clue frequency gradually, not permanently
- critical quest requirement may still surface despite low quest tolerance
- "Not today" should suppress immediate repeat recommendations
# Content freshness workflow

Content validation is development-time only. Gielinor Compass must not fetch
game data at runtime.

The machine-readable source ledger is
`src/main/resources/content/content-freshness.json`. Its validation date is the
date on which live-vs-announced semantics were reviewed, not a promise that
every partial domain is exhaustive.

Run the safe refresh check with:

```sh
./scripts/refresh-content.sh
```

For a real upstream refresh, first update the pinned authoritative snapshot,
then run the quest importer in review mode, inspect new/removed/renamed/changed
identities, and update hand-authored strategy knowledge separately. Never let a
generator overwrite semantic routing without review. An announced future
change belongs in `announcedNotLive` with `planningEnabled: false` until an
official live update is verified.

Identity importers can emit review snapshots with stable records shaped as
`{"id":"stable-upstream-id","name":"display name","fingerprint":"semantic-hash"}`
inside each family. Compare the last reviewed and proposed snapshots with:

```sh
python3 scripts/detect-content-changes.py \
  --baseline path/to/reviewed.json \
  --current path/to/proposed.json
```

The report uses `NEW`, `REMOVED`, `RENAMED`, `CHANGED`, and
`POSSIBLY_STALE`. It never edits catalogs or enables announced content. A human
must review every event, update provenance and regression tests, and only then
replace a committed snapshot.
