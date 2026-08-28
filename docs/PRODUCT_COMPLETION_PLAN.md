# Product completion implementation map

This document records the capability inventory taken at the start of the
product-completion phase. It is an implementation map, not a claim that a
typed class makes a gameplay domain complete.

## Starting baseline

- Branch: `feature/content-meat-and-potatoes`
- Local and remote HEAD: `b5b78d68706149b4857d6951a9cbf957e429939f`
- Untouched baseline: `./gradlew clean test` passed (595 tests)
- The local, untracked `AGENTS.md` is intentionally not part of this work.

## Existing seams to retain

| Capability | Existing production seam | Completion work |
|---|---|---|
| Final decision | `StrategyEngine`, `RecommendationEngine`, candidate registry, actionability and safety policies | Make typed account value and plan state affect ranking; keep hard safety filters final. |
| Method choice | `TrainingMethod`, selector, policy, execution profiles, requirement evidence | Remove duplicate property scoring and move reusable facts out of growing ID dispatch. |
| Account modes | `AccountModePolicy`, UIM capability/setup services, Group Storage freshness | Add a typed strategic-priority profile; mode must alter value as well as legality. |
| Infrastructure | POH, storage, transport and ability snapshots/catalogues | Add verified milestones, substitutes, downstream utility and final-queue candidates. |
| Travel | `TransportCatalog`, snapshot and universal dependency resolver | Feed reachable route/setup burden into concrete method and location decisions. |
| Resources | Readiness, acquisition, dependency and source catalogues | Add sustainable-pipeline and consumable-opportunity-cost value without false stock precision. |
| Gear | Contextual ladders, acquisition routes and upgrade candidates | Compare owned state, acquisition burden, replacement horizon and account practicality. |
| Slayer | Live task/count/points reader, identity/profile catalogues and guidance | Add assignment state, master selection, point economy and DO/SKIP/BLOCK/PREP/ALTERNATIVE decisions. |
| Quests/goals | Quest catalogue/resolver, goal graph, strict provenance and reward forecast | Order the active path, compress shared dependencies and retain required quests under low optional-quest preference. |
| Continuity | Recommendation stabilizer and universal dependency graph | Add an explicit plan and typed completion/invalidation rules across intermediate steps. |
| Opportunities | Observed timer engine, clues and two resource detours | Add typed DO NOW/PREP FIRST/WAIT/SKIP verdicts and benefit-threshold detours. |
| Progress | Absolute XP in account snapshots, stat events, milestone tracker and bounded feedback history | Add event-driven session XP, active time, measured rates, ETA, bounded buckets, milestones, recap and charts. |
| UI | Compact sidebar, wrapping/accessibility helpers, mutually exclusive overlays | Keep DO NEXT primary and add one calm secondary Progress surface. |

## Architectural corrections

1. `StrategySignal` currently explains state after selection; its score does not
   affect the shared queue. Strategic value will be represented by typed
   candidate/method assessments consumed at the final decision boundary.
2. Player-facing IDs and prose must not control ranking. New work uses typed
   risk, resource, travel, infrastructure, plan and readiness properties.
3. `UniversalDependencyPlanner` remains the bounded dependency resolver. An
   explicit plan layer will order its proven nodes, track completion and retain
   the next transition instead of creating a competing dependency graph.
4. Progress events update a small local model and coalesced UI repaint. They do
   not trigger full recommendation evaluation for every XP drop.
5. Live state that RuneLite cannot prove stays unknown. Optional capability
   input may be introduced only when it earns its UI cost and is namespaced to
   the character.

## Local checkpoint sequence

1. Account priorities plus infrastructure/unlock value.
2. Travel/resource/gear value integration.
3. Slayer decision loop and observable-state boundary.
4. Quest ordering, shared dependencies, explicit plans, setup reuse and
   worth-doing-now decisions.
5. Progress analytics, persistence, charts, recap and UI integration.
6. Cross-system red-team scenarios, performance audit, sources and maintainer
   documentation.

Every checkpoint is required to pass its focused tests. Substantial checkpoints
also run `./gradlew clean test`; the final state additionally runs the content
census/freshness validations and `git diff --check`.
