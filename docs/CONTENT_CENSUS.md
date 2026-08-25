# Content coverage census

Gielinor Compass keeps explicit local coverage manifests so new game content is
not silently omitted. Runtime planning remains local; these manifests do not
scrape the Wiki or any other service.

The identity census was revalidated on 2026-08-25 against current RuneLite
master and the pinned RuneLite 1.12.35 build dependency:

| Family | Authoritative identity source | Discovered | Structured | Partial | Conservative |
| --- | --- | ---: | ---: | ---: | ---: |
| Quests and RuneLite quest-state identities | `net.runelite.api.Quest` | 211 | 211 represented | 0 | 0 |
| Progression miniquests | Maintained, Wiki-verified manifest | 19 | 19 | 0 | 0 |
| Minigames and major minigame-style activities | Maintained, Wiki-verified manifest | 43 | 42 | 0 | 0 (1 not progression-relevant) |
| PvM encounters | RuneLite `HiscoreSkill` entries of type `BOSS` | 71 | 4 | 67 | 0 |

“Represented” is not a claim of executable completeness. Quest records reconcile
the Wiki requirement module with a pinned Wiki quest-details/rewards snapshot.
The 2026-08-25 snapshot uses the evidence-aware schema for all 219 source rows:
explicit source NONE remains NONE, missing fields remain SOURCE_MISSING, and
unsupported page structure remains UNSUPPORTED_STRUCTURE.

The executable quest-item census currently reports 211 identities, 195 with an
item source field, 44 fully executable-or-verified-NONE, 141 partially
executable, 26 raw-only, 680 unsupported lines, 0 parser failures, and 0
source-missing fields. `./gradlew contentCensus` lists every unresolved line
with quest, field, raw evidence, and reason. These figures deliberately prevent
identity coverage from masquerading as executable planning coverage.
“Partial” may produce concrete
preparation but cannot claim encounter readiness. “Conservative” records the
identity and why it remains fail-closed. Conservative content cannot lead
**DO NEXT** merely because its identity is known.

The quest enumeration includes several miniquests, Recipe for Disaster
subquests, tutorials, and other quest-state entries. They remain in the census
because RuneLite can report them and a future RuneLite update must not create an
undetected planning gap.

Run the focused drift check with:

```sh
./scripts/check-content-census.sh
```

When RuneLite adds an identity, the pinned census assertions fail. Update the
manifest only after verifying current facts; otherwise add an explicit
conservative entry rather than guessing requirements.
