# Contributing to OSRS Strategist

OSRS Strategist is an account-aware adviser. The most important contributor rule is simple: **do not make the plugin sound more certain than the evidence allows.**

This guide is intentionally strict because a plausible-looking but incorrect recommendation is worse than a visible `Check First` state.

## Development environment

The project is tested with:

- Java 21
- Gradle wrapper committed in the repository
- RuneLite APIs declared by `build.gradle`

Run the full suite before opening or updating a pull request:

```bash
./gradlew clean test --stacktrace
```

For local RuneLite development:

```bash
./gradlew run
```

Do not commit local RuneLite credentials, Jagex credentials, private keys, IDE secrets, or account-specific private data.

## Architectural boundaries

### Readers observe

Reader/services may collect state the RuneLite client can actually expose or state the plugin previously observed and stored with clear provenance.

Examples:

- `LiveItemStateReader`
- `LiveQuestStateReader`
- `LiveClueStateReader`
- farming/access observation services

A reader should not decide that an activity is strategically good.

### Snapshots store immutable knowledge about one account state

Snapshots should be boring data containers. Avoid hidden scoring or side effects inside snapshots.

### Catalogs describe game content

Catalog records should describe activities, methods, rewards, risks, level ranges, and compatibility. They do not prove that the current character has access.

### Evaluators prove requirements

Requirement evaluators answer questions such as:

- Is the item actually observed?
- Is this quest complete?
- Is the patch reachable?
- Is the storage capability verified?

If evidence is incomplete, return `CHECK_NEEDED` rather than guessing.

### Policies protect account safety and player intent

Account-mode, risk, actionability, and healthy-variety policies should remain small and testable. Never bury a hard safety rule inside a magic score constant.

### Candidate providers propose work

New non-skill systems should usually plug into `StrategyCandidateProvider`. Do not add a giant chain of special cases to `OsrsStrategistPlugin` or the Swing panel.

### UI presents decisions

The panel and overlay should not invent strategy. They display decisions and evidence produced by the engine.

## Account-mode requirements

Any new feature that depends on items, storage, death mechanics, trading, or acquisition should consider at least:

- Main
- Ironman
- Group Ironman
- Ultimate Ironman
- Hardcore variants

### Main

The GE is available, but that does not mean the player has infinite GP. Purchasing should require known price/cash assumptions, and protected-item rules must prevent destructive liquidation suggestions.

### Iron / GIM

Do not route through the GE. GIM Group Storage only counts when enabled and actually observed.

### UIM

Never use a normal bank as a resource source. Storage is capability-specific. POH/STASH/Tool Leprechaun/looting bag/death storage/deathpile state must not be assumed. Death-based or restricted storage can prove an item exists while still remaining `Check First` because retrieval/risk conditions matter.

### Hardcore

Do not silently recommend Wilderness/high-risk content. Risk policy must be explicit and tested.

## F2P/P2P requirements

Every new method/activity should state membership compatibility. A members-only clue, minigame, skill route, diary, boss, or recurring activity must not leak into an F2P session merely because stale state was observed on a members world earlier.

When membership is unknown, prefer conservative behavior.

## Game-data sourcing

Prefer current primary/authoritative sources:

1. RuneLite source/API for client identities and APIs.
2. Jagex updates for newly released mechanics.
3. OSRS Wiki for mature gameplay requirements, methods, reward structures, and strategy references.

Community posts can reveal a gap, but should not by themselves promote a planning record to verified production truth.

See `docs/GAME_KNOWLEDGE_MAINTENANCE.md`.

## Training-method checklist

A good method addition considers:

- level band;
- membership;
- account-mode compatibility;
- Wilderness/risk;
- attention and setup time;
- useful session length;
- required items;
- quests/access/transport;
- acquisition path when resources are missing;
- useful rewards, currencies, outfits, untradeables, CLOG, diary, CA, or gear synergy;
- whether the method is part of a progression-protected objective.

Do not encode all of this into a long instruction string if a typed field/evaluator can represent it.

## Tests expected for behavior changes

Depending on the feature, add focused tests for several of these:

- lower/upper level boundaries;
- F2P filtering;
- Main vs Iron acquisition;
- UIM bank rejection/storage safety;
- Hardcore/Wilderness behavior;
- unknown evidence staying unknown;
- known items turning checks Ready;
- score adjustment caps;
- progression-objective protection;
- persistence/backward compatibility;
- UI wording regressions.

A catalog-size test is useful for accidental deletion but is not evidence that the data is correct.

## UI rules

The RuneLite sidebar is narrow even after Strategist's modest width increase.

- Default card: what, best method, unresolved checks.
- `Details`: instructions, session fit, evidence, why the method, why the goal matters.
- Avoid walls of text.
- Do not use `?` as an unknown-state marker. Strategist uses a neutral hollow circle for `Check First` and a check mark for verified readiness.
- Make labels wrap within the actual allocated Swing width.
- Live-test font/width changes on RuneLite, not only in unit tests.

## Healthy engagement rules

Do not add dark patterns to increase plugin usage. No artificial streaks, guilt, fake urgency, hidden punishment for leaving, or random retention rewards.

Player feedback should increase autonomy. Repeated behavior can gently break close recommendation ties, but it must remain bounded and reversible.

See `docs/HEALTHY_ENGAGEMENT_DESIGN.md`.

## Comments and naming

Comments should explain **why a constraint exists**, not narrate obvious Java syntax.

Good:

> UIM bank contents are ignored here because a normal bank is not an accessible resource source for that account mode.

Less useful:

> Loop through the list.

Use stable IDs for persisted recommendations/preferences. Changing an ID can reset learned preference and cooldown/history behavior for existing users.

## Pull-request expectation

A strong PR explains:

- the player problem;
- the model/data change;
- why the account-mode behavior is safe;
- what remains unknown or partial;
- tests added;
- live RuneLite behavior if UI/API integration changed.

Do not call a domain complete because the architecture exists or because many records were added. `GameKnowledgeManifest` deliberately distinguishes `SCAFFOLDED`, `PARTIAL`, and `VERIFIED`.