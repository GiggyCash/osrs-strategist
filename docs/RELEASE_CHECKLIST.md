# Release and Live-Test Checklist

Use this checklist for substantial Strategist milestones and before any future Plugin Hub release. It exists to prevent a large, exciting content batch from shipping with a basic account-safety or UI regression.

## Automated checks

- [ ] `./gradlew clean test --stacktrace` passes on Java 21.
- [ ] GitHub Actions passes on the exact commit intended for testing/release.
- [ ] Game-knowledge breadth tests pass.
- [ ] No test was weakened merely to make a new behavior green without documenting the product-rule change.
- [ ] No new deprecation/error warning indicates an immediately required RuneLite API migration.

## Account truth

- [ ] Logged-out state is safe and quiet.
- [ ] Account type is correct after profile switch.
- [ ] Membership state is correct after switching F2P/P2P worlds/accounts.
- [ ] Inventory changes refresh readiness.
- [ ] Equipped-item changes refresh readiness.
- [ ] Bank-dependent claims only appear after a bank snapshot was actually observed.
- [ ] Stale state from another character/profile does not leak into the current one.

## Account-mode matrix

### Main
- [ ] GE-capable logic does not assume infinite GP.
- [ ] Protected items cannot be casually liquidated.
- [ ] Unknown prices/cash remain unknown.

### Iron / GIM
- [ ] GE acquisition is not recommended.
- [ ] Group Storage is optional and only observed contents count.

### UIM
- [ ] Normal bank is ignored.
- [ ] Inventory/equipment count correctly.
- [ ] Verified safe storage counts only when capability/content evidence permits it.
- [ ] Looting bag/death storage/deathpile do not become automatically Ready without retrieval/risk checks.

### Hardcore
- [ ] Wilderness/high-risk suggestions obey risk policy.
- [ ] Clues/PvM with potentially dangerous routing are not mislabeled Ready.

## Recommendation behavior

- [ ] `DO NEXT` has a concrete activity.
- [ ] Skill recommendations have a concrete `BEST METHOD`.
- [ ] Method choice changes when account evidence makes a close alternative clearly more actionable.
- [ ] A strategically much stronger route is not buried merely because one prep fact remains unresolved.
- [ ] `Later`, `Not Today`, and `Dislike` rerank immediately and persist per character.
- [ ] One skip does not suppress an entire activity family.
- [ ] Repeated avoidance/variety behavior remains bounded and reversible.
- [ ] Progression-protected outfit/untradeable grinds are not interrupted by freshness logic.

## Opportunities

- [ ] F2P cannot see members-only recurring content or clue tiers.
- [ ] No recurring reminder appears merely because an activity exists in the catalog.
- [ ] Timers/readiness are based on observed state.
- [ ] Preparation checklists use actual reachable/unlocked content where readers exist.

## UI and accessibility

- [ ] Sidebar width is comfortable at common RuneLite window sizes.
- [ ] Long recommendation titles wrap instead of clipping.
- [ ] Body text does not run under card borders/buttons.
- [ ] In-game guidance overlay remains readable and movable.
- [ ] `○` communicates `Check First`, `✓` communicates Ready, and `✕` communicates Blocked.
- [ ] Compact view does not repeat attention/readiness metadata unnecessarily.
- [ ] Detailed view still contains instructions, session fit, evidence, and rationale.
- [ ] Feedback buttons remain usable at the configured sidebar width.
- [ ] Scrollbar appears normally when vertical content exceeds the panel.

## Full-game content sanity

Spot-check at least one representative from each implemented family:

- [ ] combat training
- [ ] gathering skill
- [ ] production skill
- [ ] Runecraft
- [ ] Farming
- [ ] Hunter
- [ ] Slayer
- [ ] Sailing
- [ ] quest
- [ ] clue
- [ ] diary
- [ ] minigame/skilling boss
- [ ] PvM/boss
- [ ] raid-readiness route
- [ ] Combat Achievement
- [ ] Collection Log
- [ ] gear upgrade
- [ ] money/resource route
- [ ] outfit/untradeable objective
- [ ] transport/POH/storage-dependent route

## Documentation and maintainability

- [ ] New score behavior is documented in `SCORING_MODEL.md` when appropriate.
- [ ] New content follows `GAME_KNOWLEDGE_MAINTENANCE.md`.
- [ ] New contributor-sensitive behavior has comments explaining why constraints exist.
- [ ] Stable persisted IDs were not changed accidentally.
- [ ] `GameKnowledgeManifest` does not overstate completeness.
- [ ] Any known partial coverage is described honestly in the PR/release notes.

## Manual RuneLite soak test

After CI is green, play normally instead of clicking only synthetic test cases:

1. Log in and open Strategist.
2. Open the bank once so bank-aware evidence has a real snapshot.
3. Equip/unequip a useful item.
4. Move a relevant supply between bank and inventory.
5. Gain at least one skill level or complete a tracked checkpoint if practical.
6. Use one feedback action.
7. Let the plugin sit open during normal travel/combat/skilling.
8. Watch for impossible recommendations, stale readiness, clipping, recommendation thrash, or spam.

A normal-play soak test often catches integration problems that perfectly isolated unit tests cannot.

## Release-blocking defects

Do not release with any known issue that can:

- recommend members-only content on F2P;
- assume unavailable UIM storage/bank access;
- recommend a blocked/high-risk action as Ready;
- leak another character's persisted account state;
- claim an unobserved item/unlock exists;
- clip or hide the primary recommendation at normal RuneLite sizing;
- automate gameplay rather than advise;
- corrupt or repeatedly rewrite profile configuration;
- crash RuneLite during ordinary account switching or item/stat updates.
