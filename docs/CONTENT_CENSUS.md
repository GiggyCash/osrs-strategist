# Content coverage census

Gielinor Compass keeps explicit local coverage manifests so new game content is
not silently omitted. Runtime planning remains local; these manifests do not
scrape the Wiki or any other service.

The identity census was revalidated on 2026-08-25 against current RuneLite
master and the pinned RuneLite 1.12.35 build dependency:

| Family | Authoritative identity source | Discovered | Structured | Partial | Conservative |
| --- | --- | ---: | ---: | ---: | ---: |
| Quests and RuneLite quest-state identities | `net.runelite.api.Quest` | 211 | 211 represented | 0 | 0 |
| Skills and strategically useful training methods | `net.runelite.api.Skill`, maintained strategy catalog, RuneLite calculators | 24 skills / 165 curated methods / 1,528 actions | 24 skills represented | 0 raw-only skills | 0 invalid methods |
| Progression miniquests | Maintained, Wiki-verified manifest | 19 | 19 | 0 | 0 |
| Minigames and major minigame-style activities | Maintained, Wiki-verified manifest | 43 | 42 | 0 | 0 (1 not progression-relevant) |
| PvM encounters | RuneLite `HiscoreSkill` entries of type `BOSS` | 71 | 4 | 67 | 0 |
| STASH units | RuneLite `STASHUnit` and `EmoteClue`, current Wiki build rules | 119 | 119 | 0 | 0 |

“Represented” is not a claim of executable completeness. Quest records reconcile
the Wiki requirement module with a pinned Wiki quest-details/rewards snapshot.
The 2026-08-25 snapshot uses the evidence-aware schema for all 219 source rows:
explicit source NONE remains NONE, missing fields remain SOURCE_MISSING, and
unsupported page structure remains UNSUPPORTED_STRUCTURE.

The executable quest-item census currently reports 211 identities, 195 with an
item source field, 90 fully executable-or-verified-NONE, 121 partially
executable, 0 raw-only, 331 unsupported lines, 0 parser failures, and 0
source-missing fields. `./gradlew contentCensus` lists every unresolved line
with quest, field, raw evidence, and reason. These figures deliberately prevent
identity coverage from masquerading as executable planning coverage.
“Partial” may produce concrete
preparation but cannot claim encounter readiness. “Conservative” records the
identity and why it remains fail-closed. Conservative content cannot lead
**DO NEXT** merely because its identity is known.

The training-method census covers all 24 current skills with 165 curated
strategic methods. RuneLite contributes 1,528 deterministic calculator actions
across 17 skills as execution evidence; these actions do not become strategy
recommendations by identity alone. The census validates unique IDs, level
bounds, player-facing instructions, membership consistency, self-source routes,
and Hardcore-safe alternatives. Current-live overrides correct the pinned
RuneLite Hallowed Sepulchre floor 4/5 levels to 77/87; the announced 2026-09-02
follow-up remains excluded from runtime planning.

The STASH census covers all 119 current units: 3 Beginner, 31 Easy, 25
Medium, 16 Hard, 19 Elite, and 25 Master. Every record has RuneLite identity,
world coordinates, location, and authoritative emote/equipment evidence. Tier
rules supply the exact Construction level and build materials. Built and filled
state remains `UNKNOWN` until explicitly observed; seven location records are
classified as Wilderness-risk routes and cannot route without permission.

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
