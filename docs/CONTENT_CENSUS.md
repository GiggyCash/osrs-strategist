# Content coverage census

Gielinor Compass keeps explicit local coverage manifests so new game content is
not silently omitted. Runtime planning remains local; these manifests do not
scrape the Wiki or any other service.

The identity census was revalidated on 2026-08-29 against current RuneLite
master and the pinned RuneLite 1.12.35 build dependency:

| Family | Authoritative identity source | Discovered | Structured | Partial | Conservative |
| --- | --- | ---: | ---: | ---: | ---: |
| Quests and RuneLite quest-state identities | `net.runelite.api.Quest` | 211 | 211 represented | 0 | 0 |
| Skills and strategically useful training methods | `net.runelite.api.Skill`, maintained strategy catalog, RuneLite calculators | 24 skills / 178 curated methods / 1,528 actions | 24 skills represented | 0 raw-only skills | 0 invalid methods |
| Progression miniquests | Maintained, Wiki-verified manifest | 19 | 19 | 0 | 0 |
| Minigames and major minigame-style activities | Maintained, Wiki-verified manifest | 43 | 42 | 0 | 0 (1 not progression-relevant) |
| PvM encounters | RuneLite `HiscoreSkill` entries of type `BOSS` | 71 | 4 locally verifiable | 67 preparation-only; all 71 have specific preparation | 0 |
| Slayer assignments | RuneLite Slayer `Task` enum | 151 | 151 mapped to 146 specific profiles / 227 aliases | readiness stays evidence-bound | 0 |
| Contextual gear and acquisition | Maintained target/context catalogs | 41 acquisition targets / 17 context ladders | 7 decision kinds operational | live value/readiness checked | 0 |
| Resource dependencies and sources | Deterministic recipes and account-aware source families | 41 recipes / 60 source families | quantity/yield/account routes | unknown live supply checked | 0 |
| Achievement Diary tasks | RuneLite Achievement Diary definitions | 378 tasks / 12 regions / 48 tiers | 378 represented | alternatives remain explicit checks | 0 |
| Reusable transport systems | RuneLite identities plus maintained current-live unlock evidence | 41 systems / 19 categories | 41 | live unlock state checked | 0 |
| STASH units | RuneLite `STASHUnit` and `EmoteClue`, current Wiki build rules | 119 | 119 | 0 | 0 |

“Represented” is not a claim of executable completeness. Quest records reconcile
the Wiki requirement module with a pinned Wiki quest-details/rewards snapshot.
The 2026-08-29 snapshot uses the evidence-aware schema for all 219 source rows:
explicit source NONE remains NONE, missing fields remain SOURCE_MISSING, and
unsupported page structure remains UNSUPPORTED_STRUCTURE. The Frozen Door and
Barbarian Training intentionally distribute their unlocks through walkthrough
sections rather than conventional reward sections; the generator proves those
known structures as NOT_APPLICABLE while typed local quest definitions retain
their requirements, checks, and durable unlocks.

The executable quest-item census currently reports 211 identities, 195 with an
item source field, 81 deterministically executable-or-verified-NONE, 130
partially executable, 0 raw-only, 0 unsupported item lines, 352 explicit
semantic checks, 0 parser failures, and 0 source-missing fields. Conditional,
skill-dependent, variable-quantity, generic-loadout, and quest-phase evidence
becomes a specific `CHECK_NEEDED` action rather than a fake exact item. The
census counts those checks separately, so normalized evidence cannot masquerade
as deterministic execution coverage. `./gradlew contentCensus` lists any future
unresolved lines with quest, field, raw evidence, and reason.
“Partial” may produce concrete
preparation but cannot claim encounter readiness. “Conservative” records the
identity and why it remains fail-closed. Conservative content cannot lead
**DO NEXT** merely because its identity is known.

The training-method census covers all 24 current skills with 178 curated
strategic methods. RuneLite contributes 1,528 deterministic calculator actions
across 17 skills as execution evidence; these actions do not become strategy
recommendations by identity alone. The census validates unique IDs, level
bounds, player-facing instructions, membership consistency, self-source routes,
and Hardcore-safe alternatives. Current-live overrides correct the pinned
RuneLite Hallowed Sepulchre floor 4/5 levels to 77/87 and replaces the live
birdhouse XP values. Calculator XP made stale by non-exact Hunter/Colossal Wyrm
changes is suppressed rather than used for false exact action counts; the
announced 2026-09-02 follow-up remains excluded from runtime planning.

The STASH census covers all 119 current units: 3 Beginner, 31 Easy, 25
Medium, 16 Hard, 19 Elite, and 25 Master. Every record has RuneLite identity,
world coordinates, location, and authoritative emote/equipment evidence. Tier
rules supply the exact Construction level and build materials. Built and filled
state remains `UNKNOWN` until explicitly observed; seven location records are
classified as Wilderness-risk routes and cannot route without permission.

The diary census is generated from the pinned RuneLite definitions and contains
all 378 current task rows across 12 regions and 48 tiers. Direct skill, quest,
combat-level, and quest-point prerequisites are structured. RuneLite `OR`
requirements remain one explicit alternative check instead of being flattened
into an unsafe all-of requirement. Diary goals traverse the first known unmet
task prerequisite, while the unobserved per-task completion state is always
called out before **DO NEXT**.

The transport census contains 41 high-value reusable systems across all 19
modeled families, including fairy rings, spirit trees, gliders, minecarts,
boats, spellbooks, jewellery, diary/minigame/Slayer routes, POH routes, and
current Sailing transport. It includes all 15 Agility shortcuts added or
converted to barehanded travel on 2026-08-12. Verified live routes short-circuit their setup;
unknown membership, POH furniture, destination access, or Wilderness risk fails
closed. POH furniture is never inferred from Construction level.

All 71 current boss identities now have named preparation profiles covering the
known first access, style, mandatory setup, supply, and risk questions. Four
simple encounters retain complete locally verifiable evidence profiles; the
other 67 remain preparation-only and never claim player mechanical readiness.
The Slayer census is generated from RuneLite's canonical 151-task enum and
asserts that every assignment maps to a specific maintained profile. Profiles
separate mandatory protection, location/access, combat style, cannon and
multitarget-Magic evidence, Wilderness/boss variants, Iron objectives, and the
keep/extend/skip/block decision.

Gear planning separates best owned, best usable, best available now, best value
upgrade, best practical upgrade, long-term target, and target-specific best.
Compound slot prose is not treated as one exact missing item. The 41 acquisition
targets traverse shared quests, skills, bosses, minigames, resources, shops,
and checks through the bounded universal graph. Resource planning retains 41
deterministic yield-aware definitions and 60 Main/Iron/UIM-aware source
families; unknown storage never becomes an empty bank assumption.

The ranking suite contains 51 defensible sensible-winner scenarios and 20 real
multi-domain dependency simulations; at least 10 contain five or more edges and
at least five contain seven or more. State-transition tests cover observation,
progression, gear, feedback rotation, relog restoration, and account isolation.
Semantic feedback prevents provider-alias rebound without suppressing unrelated
recommendations.

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
