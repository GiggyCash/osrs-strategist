# Architecture

Keep the plugin boring internally. Boring code survives game updates.

## 1. AccountStateService
Reads only observable state:
- levels / XP / total
- account mode
- quests
- inventory
- equipment
- latest bank snapshot
- latest Group Storage snapshot if enabled
- observed storage/capability state

## 2. CapabilityService
Answers questions such as:
- Can this account use the GE?
- Is Group Storage enabled and observed?
- Is this POH storage method actually available?
- Is this STASH built / filled / unknown?
- Is this Tool Leprechaun tool known to be stored?
- Is this transport unlocked?

Three-state values are required: VERIFIED, UNKNOWN, BLOCKED.

## 3. GameDataRepository
Data, not strategy code:
- activities
- training methods
- requirements
- XP estimates
- GP costs
- resource outputs
- account compatibility
- quest dependencies
- diary dependencies
- clue/STASH requirements
- recurring opportunities

The long-term goal is to make most content updates data updates.

## 4. StrategyEngine
Scores valid activities. Suggested score families:
- unlock value
- progress toward pinned goal
- total-level efficiency
- XP/time
- GP/time
- resource synergy
- multi-skill value
- bank opportunity
- momentum / near breakpoint
- session fit
- attention fit
- player preference
- recent repetition penalty
- quest tolerance
- risk / confidence penalty

Invalid methods receive no score at all.

`StrategySourceRegistry` and `MethodStrategyKnowledgeCatalog` form the local
strategy-knowledge boundary. Sources are reviewed during development and
reduced to concise typed properties; runtime never queries the Wiki. The
selector asks for profiles applicable to the observed account mode before
ranking. A Main bank loop is therefore not generated for UIM and then repaired
after selection. Shared methods remain one shared record when their practical
execution is genuinely common.

`MethodInventoryFootprint` describes minimum practical free slots, persistent
and temporary slots, inventory flow, and setup teardown. UIM viability is
relative to that proposed method, not a free-slot score in isolation.

The implemented account-value boundary uses `RecommendationStrategicValue`.
Typed goal provenance, infrastructure, unlock, travel, resource, setup reuse,
shared-dependency, risk, and opportunity-cost properties are attached before
the final queue. IDs and player-facing prose do not manufacture those values.

The final decision boundary is ordered as:

1. merge every candidate family, including opportunities;
2. reject membership-incompatible content;
3. reject account/build-unsafe content;
4. reject BLOCKED and bare unresolved candidates;
5. apply intelligence, feedback, session, risk and fatigue scoring;
6. choose DO NEXT only from candidates that satisfy the actionability contract.

Provider filtering is an early optimization, not the security boundary.

`FinalExecutionPlanValidator` runs after guidance and travel resolution. It
propagates the resolved typed banking behavior into final safety evidence, so a
method cannot pass early UIM checks and later acquire a conventional withdraw /
bank / repeat loop.

## 5. EconomyPlanner
Main accounts only:
- required spend
- current GP
- safe liquidation candidates
- protected items
- money-making alternatives
- buy-vs-gather comparison

## 6. UimStoragePlanner
Models storage and inventory pressure without guessing.
The planner asks CapabilityService before proposing POH, STASH, Tool Leprechaun, looting bag, death storage, or deathpile actions.

## 7. OpportunityEngine
One generic engine for recurring and short detours:
- herb runs
- birdhouses
- tree runs
- farming contracts
- clues
- Tears of Guthix
- Kingdom maintenance
- future recurring content

Each opportunity defines unlocks, cooldown, required items, likely duration, value, and preparation steps.

A timer observation proves only timing. Setup becomes VERIFIED only from positive
access/resource evidence; otherwise the opportunity remains a concrete preparation
alternative or sidebar item.

## 8. PreparationService
Creates the green-check checklist before the player leaves the bank/location.

## 9. PreferenceService
Stores slow-moving activity weights and temporary cooldowns after "Not today".

## 10. UI
Default screen should remain small:
- current main goal
- one recommendation
- two alternatives
- ready opportunities
- Why this?
- feedback controls

Advanced screens may expose dependencies and debug reasoning, but the normal experience should stay calm.

## 11. Strategic plans and progress

`StrategicPlanService` derives NOW/NEXT/TARGET only from proven selected-goal
paths. `PlanContinuityService` retains valid unfinished work across minor state
changes and rebuilds on material invalidation.

`ProgressAnalyticsService` consumes XP events, excludes idle time, and stores
bounded character-local summaries. The secondary Progress surface contains the
chart, ETA, plan, milestones, and recap; it does not compete with DO NEXT.

The complete implemented seams and their evidence limitations are documented
in [PRODUCT_COMPLETION_ARCHITECTURE.md](PRODUCT_COMPLETION_ARCHITECTURE.md).
