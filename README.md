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

## Controlled beta

`0.2.0-beta.1` is a local-first testing build. **DO NEXT** compares legal,
actionable candidates across the account evidence Compass has actually
observed. Goals, Efficient/Balanced/Relaxed strategy, session intent, and the
Later/Not Today/Dislike/Do This feedback controls influence that decision and
are stored per stable RuneLite account identity.

Quest Helper remains the quest walkthrough: Compass chooses the useful quest or
prerequisite and hands execution off. RuneLite's Clue Scroll plugin remains the
detailed clue solver: Compass only decides whether clue preparation is worth
doing and what safely observed blocker comes first.

The planning core is offline and local. It does not make runtime network calls
or send account evidence to a hosted service. Unknown membership, storage,
unlock, clue-step, and POH state fails closed. This beta does not observe player
mechanical skill and does not claim exact variable rates, drops, or completion
times. See `docs/CONTENT_CENSUS.md` for the current honest coverage census.

Developers need Java 11 and the repository Gradle wrapper. Run
`./gradlew clean test --warning-mode all`, then
`./scripts/check-content-census.sh`. The reviewed content-refresh workflow is
documented in `docs/MAINTENANCE.md`.

## Development basis
The project is intended to be copied over RuneLite's official `example-plugin` template so the current Gradle wrapper is retained.

## Important rule
Unknown state stays unknown. Gielinor Compass should never invent an unlock, bank item, Group Storage item, Tool Leprechaun tool, POH storage option, STASH state, or UIM storage capability.
