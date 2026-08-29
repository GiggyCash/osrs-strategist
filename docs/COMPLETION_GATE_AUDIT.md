# Product completion gate audit

This audit records the completion mandate ending on 2026-08-29. `FIXED` means
the limitation is implemented, connected to the production decision path, and
covered by tests. `EXTERNAL BLOCKER` is reserved for state that RuneLite or the
game client does not reliably expose; it never means that more catalog research
would be inconvenient.

## Fixed

- **Slayer — FIXED.** Live assignment/master/points/streak/reward/extension/
  block evidence drives master choice and DO, SKIP, BLOCK, PREP_FIRST, or
  ALTERNATIVE. All current RuneLite task identities have reviewed mechanics,
  strategic properties, named locations, and execution guidance. Mortimer's
  choice interface and distinct economics are decoded from pinned game data.
- **Skilling — FIXED.** Every current skill has curated level bands, named
  methods and locations, account/resource/access evidence, meaningful targets,
  and transitions. Generic legacy choice delegation cannot enter production.
- **Questing and public goals — FIXED.** Current RuneLite quest identities are
  reconciled with offline Wiki enrichment; unsupported reward structures for
  The Frozen Door and Barbarian Training are typed explicitly. Quest ordering,
  guaranteed XP, shared prerequisites, provenance, and all eight public goals
  use production dependency services. Max expands every unmaxed skill to 99.
- **Clues — FIXED.** Inventory/bank clue identity, age, interruption value, and
  current RuneLite Clue Scroll step evidence feed DO/HOLD/PREP behavior. All
  current STASH identities have offline requirements and risk classification.
- **Account modes — FIXED.** Main, Iron, GIM, UIM, HCIM, HCGIM, and Unranked
  GIM preserve distinct acquisition, storage, substitution, setup, and risk
  rules. Unknown membership fails closed to F2P.
- **Infrastructure, gear, resources, travel — FIXED.** Reviewed POH/storage/
  transport milestones, contextual gear acquisition, replacement horizon,
  sustainable resource pipelines, live Main prices, and named travel/location
  alternatives compete in the shared decision layer.
- **Plans, reuse, detours — FIXED.** NOW/NEXT/TARGET continuity, semantic
  deduplication, setup reuse, interruption value, and evidence-bound detours
  share the same account-scoped pipeline.
- **Progress — FIXED.** XP, levels, active time, rolling XP/hour, ETA, bounded
  charts, persistence, account isolation, and meaningful account milestones
  are regression-tested, including idle/logout/restart/corrupt-history cases.
- **UI and performance — FIXED.** The sidebar keeps DO NEXT primary, hides empty
  sections and redundant state, retains readable scaling, and moves detail to
  the existing overlays. Bursty container/varbit events are coalesced, repeated
  catalogs are reused, and XP tracking remains event driven.
- **Strategy quality — FIXED.** Real-provider tournaments cover F2P Main, P2P
  Main, Iron, GIM, UIM, HCIM, HCGIM, Unranked GIM, unknown membership,
  early/mid/late levels, all public goals, every strategy style, and every
  session intent. Winners must be safe, specific, actionable, and free of
  unresolved access evidence.

## External blockers

- **EXTERNAL BLOCKER — unopened bank and Group Storage.** RuneLite provides
  exact container contents only after the player opens the relevant interface.
  Compass preserves the last observed snapshot and never treats unobserved
  storage as empty.
- **EXTERNAL BLOCKER — unopened clue step.** RuneLite's Clue Scroll plugin can
  expose the decoded live step only after the scroll has been opened. Compass
  can identify the tier beforehand, but cannot truthfully invent the step.
- **EXTERNAL BLOCKER — global STASH built/filled state.** RuneLite has the full
  static unit catalogue but no reliable account-wide state for every unit.
  Construction level is not used as a substitute.
- **EXTERNAL BLOCKER — teammate capabilities.** Group Storage contents are
  observable when opened; teammate levels, specialisations, current supplies,
  and POH furniture are not reliable local client state.
- **EXTERNAL BLOCKER — complete live Sailing fitting/chart state.** Stable
  RuneLite evidence exists for owned slots, active port tasks, Barracuda Trials,
  and Pandemonium. Individual chart checkboxes and every currently fitted boat
  component are not exposed through a stable supported API.
- **EXTERNAL BLOCKER — player execution and random outcomes.** The client cannot
  prove the player's encounter mechanics, future damage, RNG drops, variable
  burn/failure rates, or future market movement. Compass therefore uses
  readiness and qualitative economics instead of claiming exact outcomes.

## Non-leading identity data

The repository intentionally retains broad offline identities for future
evidence enrichment. A bare Collection Log category total, Combat Achievement
point gap, boss identity, minigame name, or item name is not allowed into DO
NEXT. This is enforced by the final actionability policy and tournament tests;
it is not reported as an external blocker or as completed executable guidance.
