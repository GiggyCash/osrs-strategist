# Gielinor Compass

**Your account. Your next move.**

Gielinor Compass is an adaptive RuneLite progression planner designed for mains, Ironmen, Group Ironmen, Hardcore variants, and Ultimate Ironmen.

Its job is simple: decide the best safe, useful thing to do next without making the player manage a giant spreadsheet. Compass decides what is worthwhile and whether the account is ready; established RuneLite tools can help execute that decision.

## Design goals
- simple UI, deep reasoning
- account-type-aware strategy
- no guessing about unavailable unlocks or storage
- bank/resource-aware progression
- optional Group Storage use
- UIM capability-aware storage planning
- Tool Leprechaun-aware farm preparation
- birdhouse and herb-run readiness checklists
- clue reminders, clue supplies, and STASH planning
- player preference learning
- Efficient / Balanced / Relaxed modes
- quest tolerance
- confidence labels and "Why this?" explanations
- local-first architecture
- no gameplay automation

## Current state
`0.2.0-beta.1` is a local-first testing build. Conservative content remains
explicitly fail-closed until its requirements are verified; see
`docs/CONTENT_CENSUS.md` for the current identity/structure/readiness census.

Start with `docs/TONIGHT_SETUP.md`.

## Development basis
The project is intended to be copied over RuneLite's official `example-plugin` template so the current Gradle wrapper is retained.

## Important rule
Unknown state stays unknown. Gielinor Compass should never invent an unlock, bank item, Group Storage item, Tool Leprechaun tool, POH storage option, STASH state, or UIM storage capability.
