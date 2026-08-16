# Strategist Scoring Model

This document explains how OSRS Strategist turns account state into one `DO NEXT` recommendation. It is intended for future contributors, reviewers, and anyone evaluating the project technically.

## Core principle

A high score is never permission to ignore a hard rule. Content must first survive membership, account-mode, risk, unlock, and requirement policy. Scoring only compares candidates that are allowed to exist.

The planner should answer two separate questions:

1. **WHAT should this account do next?**
2. **HOW should this account do it?**

Those questions share evidence, but they are intentionally separate so a new training method does not have to rewrite the whole recommendation engine.

## 1. Observe account truth

`StrategyDataAssembler` creates a `StrategyDataBundle` from live and remembered evidence such as:

- levels, XP, total level, account type, and membership;
- inventory and equipment;
- the most recent bank snapshot that was actually observed;
- quests, diaries, clues, Combat Achievements, Collection Log state;
- farming/access memory;
- group/UIM storage only when the relevant source was actually observed;
- transport, POH, Slayer, Sailing, minigame, PvM, and recurring-opportunity state as readers become available.

Unknown information is not converted into false defaults. If the bank has not been observed, a method may say `Check First`; it may not say that the account owns an item merely because a Main could buy it.

## 2. Filter impossible or unsafe methods

Before a training method receives a useful score, `TrainingMethodSelector` and `TrainingMethodPolicy` apply hard filters:

- current skill level must fit the method's range;
- F2P/P2P compatibility must match observed membership;
- Wilderness methods require the player's explicit Wilderness setting;
- Hardcore accounts reject unsuitable high-risk/Wilderness methods;
- UIM rejects methods marked incompatible with UIM routing;
- irreversible/risk-sensitive routes are blocked where account policy requires it;
- any requirement that is known `BLOCKED` removes the method.

A blocked method should disappear rather than linger as an attractive high-score card.

## 3. Choose HOW within a skill

Every valid method receives a base fit score from `TrainingMethod.scoreFor(...)`, then several bounded adjustments:

### Strategy and session fit

The method's Efficient/Balanced/Relaxed scores, attention level, setup time, and minimum useful session length are compared with the selected strategy style and session intent.

### Account-mode and risk fit

`TrainingMethodPolicy.scoreAdjustment(...)` can favor self-source-friendly/low-cost routes for Iron-like accounts, UIM-friendly routes for UIM, and safer routes for risk-sensitive accounts.

### Live actionability

`RequirementEvidenceEngine` turns method requirements into `RequirementCheck`s. Dedicated evaluators are preferred over generic text:

- Runecraft has exact rune-essence/talisman/tiara evidence;
- Farming and Agility have domain access evaluators;
- `MethodReadinessCatalog` handles stable item-driven methods such as wines, prayer potions, cannonballs, common Construction furniture, Fletching logs, and several Fishing/Firemaking routes;
- remaining unknown access facts stay `Check First`.

`ActionabilityScoringPolicy` then gives a small bonus to a fully verified method and a capped penalty to unresolved methods. This lets a ready method win a close contest without burying a substantially better progression method.

The winning method becomes the recommendation's `TrainingPlan`.

## 4. Build WHAT candidates across the game

Skill recommendations are only one candidate family. `StrategyCandidateRegistry` also allows verified/observed candidates from:

- quests;
- clues;
- PvM;
- diaries;
- Combat Achievements;
- gear progression;
- money making;
- minigames/repeatable activities;
- Collection Log progress.

Future candidate families should implement `StrategyCandidateProvider` rather than adding special-case logic directly to the UI.

## 5. Cross-domain actionability

After all domains have produced candidates, `ActionabilityScoringPolicy` runs again on the final recommendations. The intent is practical:

- a similarly valuable activity that is proven ready can move ahead of one with several unresolved prep checks;
- a strategically much stronger activity remains stronger even when it still needs one or two checks.

The adjustment is intentionally small and capped.

## 6. Healthy variety and player preference

`PreferenceProfile` remains the strongest representation of explicit player feedback for a specific activity. `Later`, `Not Today`, and `Dislike` create cooldown/weight behavior attached to stable activity IDs.

`HealthyEngagementPolicy` is a weaker family-level tie-breaker. It may slightly reduce a family only after repeated avoidance or a longer run of recently completed activities. One skip is never enough to infer that the player dislikes an entire category.

Progression-protected objectives such as an outfit or useful untradeable grind are protected from completion-based variety penalties.

See `HEALTHY_ENGAGEMENT_DESIGN.md` for the non-dark-pattern requirements.

## 7. Sort and present

Only after the previous layers does `StrategyEngine` sort the candidates and keep the strongest three for the normal sidebar:

1. `DO NEXT`
2. two alternatives
3. independent ready opportunities

The compact UI should expose only decision-critical information. Detailed scoring/evidence belongs under `Details` or future diagnostics, not in the default card.

## Stable ordering of concerns

When adding a new feature, preserve this conceptual order:

1. **Observed truth**
2. **Hard validity and safety**
3. **Goal/progression value**
4. **Strategy/session/account-mode fit**
5. **Method-level actionability**
6. **Cross-domain actionability**
7. **Explicit per-activity preference/cooldowns**
8. **Weak healthy-variety adjustment**
9. **Presentation**

Do not solve a missing-reader problem by increasing a score. Do not solve a UI problem by weakening a safety rule.

## Adding a new training method

A contributor should normally:

1. Add a structured `TrainingMethod`/`CuratedTrainingMethod` record.
2. Add accurate membership, risk, cost, attention, and account-mode metadata.
3. Add a typed readiness profile or dedicated evaluator for requirements that can be observed safely.
4. Leave facts `Check First` when they cannot be proven yet.
5. Add tests for level bands, F2P/P2P, at least one relevant Iron/UIM case, and any unusual risk rule.
6. Run the full Java 21 test suite.
7. Live-test the method only after CI is green.

## Adding a new non-skill activity

Prefer a catalog + snapshot + candidate-provider pattern:

- the catalog describes the activity;
- a reader/snapshot says whether this character has actually unlocked or observed it;
- a provider turns that evidence into a candidate;
- the global engine handles ranking.

Catalog presence must never be treated as proof of character access.

## Score tuning rule

Before increasing any score constant, ask whether the problem is actually one of these:

- missing requirement evidence;
- missing goal dependency;
- missing account-mode rule;
- missing progression objective;
- wrong session/attention metadata;
- a stale or incomplete content record.

Fixing the underlying model is preferable to adding another arbitrary bonus.