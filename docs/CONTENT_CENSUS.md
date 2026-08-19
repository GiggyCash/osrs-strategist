# Content coverage census

Gielinor Compass keeps explicit local coverage manifests so new game content is
not silently omitted. Runtime planning remains local; these manifests do not
scrape the Wiki or any other service.

The census was audited on 2026-08-19 against RuneLite 1.12.35:

| Family | Authoritative identity source | Discovered | Structured | Partial | Conservative |
| --- | --- | ---: | ---: | ---: | ---: |
| Quests and RuneLite quest-state identities | `net.runelite.api.Quest` | 211 | 211 (0 with field-level uncertainty) | 0 | 0 |
| Progression miniquests | Maintained, Wiki-verified manifest | 19 | 19 | 0 | 0 |
| Minigames and major minigame-style activities | Maintained, Wiki-verified manifest | 43 | 42 | 0 | 0 (1 not progression-relevant) |
| PvM encounters | RuneLite `HiscoreSkill` entries of type `BOSS` | 71 | 4 | 67 | 0 |

“Structured” has a local planning definition. Quest records reconcile the Wiki
requirement module with a pinned Wiki quest-details/rewards snapshot; explicitly
blank requirement fields are recorded as authoritative NONE rather than UNKNOWN.
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
