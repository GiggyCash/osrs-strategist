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

The final decision boundary is ordered as:

1. merge every candidate family, including opportunities;
2. reject membership-incompatible content;
3. reject account/build-unsafe content;
4. reject BLOCKED and bare unresolved candidates;
5. apply intelligence, feedback, session, risk and fatigue scoring;
6. choose DO NEXT only from candidates that satisfy the actionability contract.

Provider filtering is an early optimization, not the security boundary.

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
