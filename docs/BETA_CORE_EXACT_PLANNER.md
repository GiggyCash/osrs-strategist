# Exact planner invariants

This note documents the invariants behind exact method calculations. The goal
is not to make every recommendation look precise. The goal is to make every
precise recommendation defensible from live account evidence and verified game
data.

## 1. Exact where the game is exact

Compass may convert a milestone into an exact action/material count when all of the following are known:

- current skill XP
- target milestone XP
- XP per modeled action
- any XP multiplier included in the model
- the consumed-input recipe for that action
- membership legality for the account
- build legality for the account

Examples include a known number of potion completions, logs burned, bars smithed, bows fletched, bones offered under a specifically modeled route, or other deterministic calculator actions.

If a route has variable XP, randomized outputs, variable kill XP, one-time rewards, variable encounter scoring, burn/failure randomness, or an ambiguous recipe, Compass must not invent a fixed completion count. The planner can still state exact XP remaining and concrete setup/readiness requirements.

## 2. RuneLite action data versus Compass route data

RuneLite's skill-calculator action definitions are the maintained source for deterministic base action XP where available.

Compass remains responsible for:

- deciding whether the action belongs to the selected training route
- checking F2P/P2P access
- checking account-build restrictions
- converting the action into a milestone count
- resolving the consumed recipe
- checking the account's usable resources
- deciding whether Main buying or Iron-style self-sourcing is legal
- explaining location, setup, and route caveats

A RuneLite action is never enough by itself to prove that the route is appropriate.

## 3. Resource evidence semantics

`AccountResourcePlanner` is the canonical deterministic supply resolver.

### Unknown is not empty

An unopened bank is unknown. Compass must ask the player to open the bank before publishing an exact stored-item shortfall. It must never convert `bank == null` into `0 banked`.

### Main

After the bank has been observed:

- inventory, equipment, bank, and verified usable storage contribute to owned quantity
- the exact missing quantity can be sent to the Grand Exchange acquisition path
- live price and verified cash are used when available
- lack of live price data must not block an exact item quantity

### Ironman and Hardcore Ironman

- no Grand Exchange advice
- observed usable supply is counted
- missing supply is a self-source requirement
- Hardcore risk policy remains an independent hard gate

### Group Ironman

- personal usable supply is always considered
- Group Storage only counts when the feature is enabled and the storage has actually been observed
- enabled but unobserved Group Storage remains unknown, not empty

### Ultimate Ironman

Normal bank contents are never usable account supply.

Immediately usable quantities may include inventory, equipment, and verified storage that does not require a separate risky/retrieval setup.

Looting bag, death storage, and deathpile contents may be remembered as observed resources, but they are reported separately as retrieval-only supply. They do not silently satisfy a normal milestone plan. A future retrieval recommendation can deliberately turn those resources into an actionable step.

This separation prevents Compass from suggesting a UIM route that only works by destroying the player's current inventory setup without acknowledging the cost.

## 4. Reusable resources

Reusable equipment must not be represented as an arbitrarily large item quantity.

For elemental runes, an equipped verified staff/source can satisfy the matching elemental-rune requirement. Merely owning the staff in a bank is not enough because the selected build or equipment setup may not allow it.

Empty or otherwise nonfunctional variants must not waive the resource requirement.

## 5. Recipe resolver policy

`UniversalActionRecipeResolver` is intentionally conservative.

A recipe is exact only when the consumed materials are stable for the modeled RuneLite action unit. The resolver fails closed for:

- composite Cooking recipes without a complete ingredient table
- special Firemaking activities that are not conventional one-log burns
- specialty Fletching ammunition whose components are not proven by the generic resolver
- unmapped Construction furniture
- ambiguous Magic spells
- Farming actions whose XP combines variable harvests/check-health/planting behavior
- any action whose name is not enough to prove the consumed recipe

Dedicated route planners should replace generic unknown recipes over time.

## 6. Cooking burn/failure rule

Successful-cook XP may be deterministic while raw-food consumption is not.

Low-level F2P Cooking therefore retains its dedicated burn-aware stage planner. A universal successful-cook count must not be presented as an exact raw-food shopping list when burns can occur unless the selected route has a validated burn model.

The same principle applies to Smithing iron failure or any other probabilistic resource consumption.

## 7. Candidate-pool ordering

The global StrategyEngine must receive the complete skill candidate pool before the final three recommendations are chosen.

The compact `RecommendationEngine.recommend(...)` API may retain a three-result view for compatibility, but `StrategyEngine` uses `recommendAll(...)`.

Reason: three high-scoring unresolved skills must not hide a lower-scoring executable action before `RecommendationActionabilityPolicy` runs. DO NEXT is selected only after skill, quest, gear, minigame, resource-detour, and other candidate families have competed under the same final actionability gate.

## 8. DO NEXT actionability

A candidate may lead the queue when:

- it is verified and has an executable action, or
- its only unresolved work is ordinary preparation that the guidance explicitly tells the player how to perform

A candidate may not lead when access, quest requirements, account-build safety, or another hard prerequisite remains genuinely unknown.

`Needs Info` can remain visible as a secondary option when useful, but it cannot displace a safe executable primary action.

## 9. Simulation matrix

`BetaAccountSimulationTest` exercises the real selector/recommendation path for:

- F2P Main
- unknown membership fail-closed state
- F2P to P2P transition
- Defence pure
- 1 Defence pure
- level-3 skiller
- Hardcore with Wilderness globally enabled
- Ironman
- UIM
- Hardcore Ironman
- GIM
- Hardcore GIM
- Unranked GIM
- multiple strategy modes
- multiple session intents

The simulation asserts safety boundaries, not subjective global optimality. Optimization quality remains a separately tunable scoring problem.

## 10. Beta blockers

Before calling the planner beta-ready, the following must remain green:

- F2P and P2P level-band coverage matrices
- account-build safety tests
- actionability/primary-queue tests
- bank unknown-versus-empty tests
- Main/Iron/GIM/UIM resource semantics
- recipe safety tests
- variable-method no-fake-precision tests
- account simulation matrix
- complete Gradle test suite in CI

Additional depth can then be added without weakening these invariants.

## 11. Known depth still to add

The generic planner is a foundation, not a claim that every OSRS route is finished. Remaining content depth includes:

- more route-specific burn/failure models
- more exact Construction furniture and alternate methods
- more spell-specific rune/loadout recipes
- task-specific Slayer gear, supply, and location profiles
- encounter-specific combat/BIS and acquisition chains
- deeper UIM retrieval/setup-cost decisions
- more live activity state readers
- dedicated resource-source chains for Iron and UIM shortages
- fatigue/variety scoring over recent session history

These additions should extend the shared models rather than bypassing them with one-off supply logic.
