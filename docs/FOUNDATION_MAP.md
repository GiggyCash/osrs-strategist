# Gielinor Compass Foundation Map

This is the maintainer-facing map for the planner. The architecture is intentionally broader than the current verified OSRS dataset.

## Core data flow

```text
RuneLite live state + verified player observations
        |
        v
Readers / per-character stores / ObservedStateStore
        |
        v
StrategyDataAssembler
        |
        v
StrategyDataBundle
        |
        v
StrategyContext
        |
        v
StrategyEngine
   |             |                |
   v             v                v
Recommendation  Opportunity       Strategy modules
Engine          Engine            + signals
   |                              |
   v                              |
Method selector/evidence           |
        \                          /
         \                        /
          v                      v
             StrategyResult
                  |
                  +--> compact RuneLite sidebar
                  +--> movable method checklist
                  +--> milestone reward overlay
```

## Recommendation pipeline

1. Read current account state.
2. Hard-filter impossible content: membership, account mode, Wilderness policy, level, known blockers.
3. Evaluate method requirements from evidence.
4. Skip dynamically blocked methods and try the next candidate.
5. Score usable work with strategy style, session intent, milestone momentum, preference learning, and temporary cooldown/variety state.
6. Blend specialized domain signals through `StrategyModuleRegistry`.
7. Show one best move plus concise alternatives/opportunities.

## Account modes

Use `AccountModePolicy` instead of scattering restrictions through feature code.

### Main

- GE is permitted but not automatically wise.
- `MainEconomyPlanner` requires verified cash plus verified price/time inputs before a buy can be considered verified.
- Insufficient GP routes to money-making/resource review, not automatic gear sales.
- `ProtectedItemProfile` and future built-in protection both veto disposal suggestions.

### Iron / hardcore

- Self-source by default.
- Hardcore variants are risk-sensitive.

### GIM

- Iron-like by default.
- Group Storage is usable only when enabled and actually observed.
- Teammate requests are not normal local recommendations.

### UIM

UIM must never inherit a bank-centric fallback.

```text
Resource need
   |
   +--> inventory
   |
   +--> observed contents in VERIFIED UIM storage
   |       |
   |       +--> ordinary safe storage: can satisfy readiness
   |       +--> death storage/deathpile: explicit risk check remains
   |
   +--> verified self-source route
   |
   v
Check Needed when the route/capability is not proven
```

`UimCapabilityService` requires:

1. The storage capability itself is verified.
2. The item is verified compatible.
3. Capacity/current preconditions are verified.

Death storage is high risk; deathpile is irreversible-risk class. Merely knowing that these mechanics exist is never permission to recommend them.

## Resource readiness/acquisition

Two related layers exist:

- `ResourceReadinessService`: do we already have what the method needs?
- `ResourceAcquisitionPlanner`: where should a missing resource come from next?

Unknown sources stay unknown. Bank unopened is not empty. Group Storage unseen is not empty. UIM storage possible is not the same as item stored there.

## Evidence and memory

Evidence is `Verified`, `Check Needed`, or `Blocked`.

Positive area observations can persist per RuneScape profile. Quest state can prove access. Farming patch state can be directly observed. Storage and resource evidence must be based on actual observation or explicit confirmation.

Future one-time confirmations should flow into per-character capability stores rather than adding global booleans.

## Farming / active guidance

The farming observer records supported patch state from RuneLite game state. The guidance layer can show:

- planted/growing,
- ready,
- empty,
- diseased,
- dead,
- check needed.

Prep uses the shared resource-readiness layer. The same `GuidanceChecklist` model is intentionally reusable by non-Farming methods.

## Goal graph

`GoalGraph` is a typed dependency-family graph, not a fixed guide. It currently has typed paths for Max, Quest Cape, Barrows Gloves, Prifddinas, Bowfa, Infernal Cape, Diary Cape, Elite Combat Achievements, Raid Ready, 2000 total, 85 Slayer, Base 70s, and gear/custom targets.

Exact prerequisite nodes should come from structured verified game data over time.

## Longer progression objectives

`ProgressionObjectiveCatalog` separates useful long grinds from tiny skill checkpoints. Graceful, Prospector, Raiments, Smiths' Uniform, Tempoross, and Wintertodt are starter examples.

A completed skill checkpoint can still trigger the reward popup. If the longer objective remains known or conservatively assumed incomplete, the normal short-term variety penalty is suppressed. Explicit verified completion releases that protection.

## Opportunities

`OpportunityEngine` is generic. Timed content only appears when a ready-time key has actually been observed.

Typed families currently include birdhouses, herb/tree runs, farming contracts, Tears, Kingdom, Kingdom approval, battlestaves, dynamite, daily diary rewards, clues, and a future cooldown seam.

## Domain modules

`StrategyModuleRegistry` provides one bus for specialized reasoning:

- Goal strategy.
- Account-mode strategy.
- UIM strategy.
- Clues.
- Progression/quests/CAs/CLOG.
- Account systems: Slayer, Sailing, minigames, transport, POH, economy.
- PvM.

New domains should add a module only when they need genuinely new reasoning. Ordinary content records should usually be data, not Java switches.

## Game knowledge / OSRS Wiki

`GameKnowledgeManifest` enumerates every major planned knowledge domain and marks coverage as Scaffolded, Partial, or Verified.

`KnowledgeRecordMetadata` records source and revision. `GameKnowledgeImportPolicy` requires a record to be explicitly validated before it can affect planning. This supports a future workflow like:

```text
OSRS Wiki / RuneLite source changes
        |
        v
staged structured records + provenance
        |
        v
validation/tests/change report
        |
        v
reviewed repository update
        |
        v
local production dataset
```

Do not make the production plugin self-modify or blindly trust a live Wiki edit.

## Compass Plus seam

Core strategy has no knowledge of billing.

```text
Local planner --------------------------> always available

StrategistEntitlementService
        |
        +--> optional future hosted feature entitlement
        |
        v
StrategistRemoteGateway
        |
        v
CURRENT BUILD: disabled, no network endpoint/transmission
```

Potential hosted capabilities are cloud sync, cross-device history, GIM team planning, remote reminders, web dashboard, and online reasoning. See `STRATEGIST_PLUS_ARCHITECTURE.md`; the filename remains stable for existing links.

## Knowledge coverage versus architecture coverage

Do not say a domain is complete merely because it has a class/interface. The repository includes `FOUNDATION_COMPLETION_CHECKLIST.md` specifically to prevent this confusion.

Most future expansion should be:

1. Structured data.
2. A live reader only when necessary.
3. Evidence evaluator.
4. Fake-account tests.
5. GitHub Actions validation.

## Safety invariants

- No click/movement/combat/banking automation.
- No normal bank routing for UIM.
- No Group Storage assumption.
- No unverified UIM storage route.
- No uncontrolled Wilderness suggestion when disabled.
- No automatic sale of protected items.
- No remote data transfer in the current build.
- Unknown state stays unknown.
