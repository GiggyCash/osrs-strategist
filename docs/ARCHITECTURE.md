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

One method ID may have both a shared profile and a materially different
account-specific variant. The selector chooses the most specific applicable
record before ranking; adding an Iron/UIM variant therefore cannot accidentally
remove Giants' Foundry, Mahogany Homes, or another valid shared activity from
Main generation.

High-impact Iron and UIM method profiles cite direct per-skill guide source
IDs, not only the training-guide index. The conventional-bank behavior census
also rejects every explicit withdraw/bank/bankstanding route from UIM profile
generation. Account-aware resolved guidance follows the same rule: Forestry,
for example, banks renewable logs for ordinary accounts but drops only the
freshly produced logs—not carried setup items—for UIM.

Shared fallback footprints are derived from typed skill family, deterministic
execution inputs, and setup duration. Method IDs and player-facing names do not
decide inventory behavior; materially different account routes use exact
sourced profiles.

`MethodInventoryFootprint` describes minimum practical free slots, persistent
and temporary slots, inventory flow, and setup teardown. UIM viability is
relative to that proposed method, not a free-slot score in isolation.
`ActivityStrategyKnowledgeCatalog` applies the same plan-relative boundary to
quest, Slayer, clue, minigame, upgrade, PvM, and POH candidates before they
enter the common ranking pool. A full observed inventory can remove a quest or
ordinary PvM loadout while preserving an inside-instance or current-setup
activity that genuinely needs no additional slots.

Any UIM method/activity with a non-zero footprint requires a complete live
inventory-slot observation. A partial snapshot cannot prove free capacity and
therefore fails closed. Zero-footprint activities such as an internally
supplied instance may remain viable when their separate access evidence is
complete.

`MinigameSetupProfile` keeps required items, player-carried supplies, location,
and the gameplay action distinct. This preserves the player contract across
METHOD, BRING, WHERE, and DO instead of allowing an execution loop to leak into
the supply field. Variable-contract activities remain CHECK_NEEDED until the
live contract and exact materials are known.

Imported quest free-slot requirements retain their exact count and are checked
against complete live slot observation. An unmet slot requirement remains
ineligible until it actually fits; Compass does not turn it into generic
"make space", bank, or drop advice for UIM.

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

`AccountResourcePlanner` no longer turns every observed shortfall into an
unconditional GE instruction. Every required item must receive an exact-name
RuneLite price, the aggregate must be complete, and liquid coins must be
observed. When no defensible time estimate exists, `MainEconomyPlanner` uses
broad wealth-burden bands: small spends can become an executable purchase,
while a material spend uses a reviewed resource-family route when one exists.
Missing price/tradeability/economy evidence fails closed rather than presenting
a fake exact optimization.

## 6. UIM storage and inventory resolution
Storage is capability-specific and fails closed. POH furniture, STASH, Tool
Leprechaun, containers, looting bag, exact Item Retrieval Services, and an
on-ground deathpile retain different identities and retrieval rules.
`UimCapabilityService` requires observed access, item compatibility, and live
capacity/preconditions together. The legacy `DEATH_STORAGE` snapshot bucket is
too generic to authorize any recommendation; exact Hespori, Zulrah, and
Volcanic Mine retrieval capabilities are modeled separately.

`UimStorageMechanicProfile` also requires each restricted system to retain its
own reviewed location, access, eligible-item, insertion/deposit, retrieval,
cost, expiration, second-death, risk, and source fields. A live capability flag
cannot authorize a restricted route if that local mechanic profile is absent
or incomplete.

Death-based storage is not ordinary inventory optimization. It carries a high
or irreversible burden, cannot satisfy normal resource readiness, and must
have a typed `RecommendationRiskDisclosure` requiring explicit acknowledgement
before detailed steps. Final execution validation rejects generic, unverified,
or undisclosed dangerous-storage guidance. The sidebar labels the only reveal
control `View Risk Steps`; selecting that deliberately acknowledges the warning
before the details overlay is shown. Ordinary recommendations retain the normal
Details control.

`UimInventoryResolutionService` orders plan-relative resolutions explicitly:
fit the current inventory, use a reviewed low-footprint alternative, make
productive use of a currently useful resource, use proven item-compatible safe
storage, build only high-value recurring storage, then consider restricted
retrieval. Exact death-storage services are considered only for a major blocked
transition after every safer option fails. There is no generic bank, drop-item,
or generic death-bank resolution.

`UimRecurringPressureService` can increase safe storage/infrastructure value
only after two distinct, completely observed inventory layouts each block at
least two sourced activity families by their own footprints. Requirement-free,
level-legal sourced skilling routes participate without pretending an unknown
quest/resource requirement is ready. Repeated reranks
of the same layout do not count, state is bounded per account, and the signal
never fabricates existing furniture or make dangerous storage eligible.

Ownership absence is account-aware evidence, not an account-mode shortcut.
Main/Iron require observed inventory, equipment, and bank surfaces; opted-in
GIM additionally requires fresh observed Group Storage. UIM requires observed
inventory/equipment and ignores a conventional bank snapshot. Resource action
selection waits for every enabled resource container before using material
coverage. Duplicate UIM quantities are merged across verified storage systems,
while any restricted-retrieval contribution remains CHECK_NEEDED until the
exact retrieval plan is validated.

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
