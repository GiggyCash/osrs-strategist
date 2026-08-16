# OSRS Strategist

**OSRS Strategist** is an adaptive RuneLite progression planner for mains, Ironmen, Group Ironmen, Hardcore variants, and Ultimate Ironmen.

Its job is simple: **recommend the best useful thing to do next, explain the best practical way to do it for this account, and keep the player out of spreadsheet-management hell.**

## Product principles

- simple default UI, deep reasoning underneath;
- account-type-aware strategy;
- observable evidence over guesses;
- concrete `DO NEXT` + `BEST METHOD` guidance;
- bank/inventory/equipment/resource-aware progression;
- optional Group Storage use;
- UIM capability-aware storage planning;
- Tool Leprechaun-aware Farming preparation;
- recurring opportunities such as Farming/birdhouses/clues only when evidence supports them;
- player preference learning with reversible cooldowns;
- Efficient / Balanced / Relaxed strategy modes;
- session intent and quest tolerance;
- bounded actionability and healthy-variety scoring;
- progression protection for useful outfits, untradeables, and longer objectives;
- local-first architecture;
- no gameplay automation;
- no dark-pattern retention mechanics.

## How recommendations are built

Strategist separates **WHAT to do** from **HOW to do it**.

A training recommendation is not complete just because it says `Train Herblore`. The method selector evaluates current levels, membership, account mode, risk, session style, and live readiness evidence to choose a concrete route. The global strategy engine then lets skills compete with quests, clues, PvM, diaries, Combat Achievements, gear, money making, minigames, Collection Log work, and other typed candidate families.

Unknown state stays unknown. If Strategist cannot prove a required item, unlock, transport, storage capability, or access condition, it stays `Check First` rather than being invented.

See [`docs/SCORING_MODEL.md`](docs/SCORING_MODEL.md) for the full scoring order.

## Current state

`0.2.0-dev` is a substantial development build, not yet a finished Plugin Hub release.

The project now has broad game-system architecture and useful partial content across training methods, quests, diaries, Farming, Slayer, Sailing, minigames, PvM, raids, clues, gear progression, money/resource routes, recurring opportunities, outfits/untradeables, and account-specific safety policies. **Broad coverage is not the same as exhaustive verified coverage.** `GameKnowledgeManifest` deliberately tracks that difference.

The remaining path toward a production-complete planner is increasingly about converting partial catalog knowledge into source-backed typed requirements/readers, expanding encounter/content detail, and doing sustained live RuneLite testing across account types.

## Development

Use Java 21 and the committed Gradle wrapper.

```bash
./gradlew clean test --stacktrace
./gradlew run
```

Useful project documents:

- [`CONTRIBUTING.md`](CONTRIBUTING.md) - architecture, safety rules, and contributor expectations
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) - major subsystem boundaries
- [`docs/SCORING_MODEL.md`](docs/SCORING_MODEL.md) - recommendation and method-selection order
- [`docs/GAME_KNOWLEDGE_MAINTENANCE.md`](docs/GAME_KNOWLEDGE_MAINTENANCE.md) - source/coverage standard for full-game data
- [`docs/HEALTHY_ENGAGEMENT_DESIGN.md`](docs/HEALTHY_ENGAGEMENT_DESIGN.md) - autonomy/variety design and prohibited dark patterns
- [`docs/RELEASE_CHECKLIST.md`](docs/RELEASE_CHECKLIST.md) - CI, account-mode, UI, and live-soak checklist
- [`docs/PRODUCT_SPEC.md`](docs/PRODUCT_SPEC.md) - product direction

## Non-negotiable account-safety rule

OSRS Strategist must never invent an unlock, bank item, Group Storage item, Tool Leprechaun tool, POH storage option, STASH state, transport, quest completion, UIM storage capability, or other account fact.

For UIM specifically, a normal bank is never treated as an accessible resource source. Restricted/death-based storage may prove that an item exists while still remaining `Check First` until retrieval and risk conditions are verified.

## Contributing

Prefer structured data, snapshots, evaluators, and candidate providers over adding one-off logic to the Swing UI or plugin event handlers. Add focused tests for account-mode and membership edge cases, and keep persisted recommendation IDs stable whenever possible.

Before a major RuneLite test or future release, use [`docs/RELEASE_CHECKLIST.md`](docs/RELEASE_CHECKLIST.md).