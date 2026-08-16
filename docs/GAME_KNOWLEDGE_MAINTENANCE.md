# Game Knowledge Maintenance and Completion Standard

Strategist is intended to reason across the whole of Old School RuneScape, but broad architecture and exhaustive verified game data are not the same thing. This document defines the standard for moving a domain from useful coverage toward production-complete coverage without introducing guesses.

## Source priority

Use the strongest available source for each kind of fact:

1. **RuneLite API/source** for client-visible identifiers, skills, item IDs, varbits, containers, hiscore boss identities, and APIs the plugin compiles against.
2. **Jagex game updates/news** for newly released content and official mechanics.
3. **OSRS Wiki** for mature training-method comparisons, minigame/activity lists, equipment/outfit effects, routes, requirements, and account-mode strategy notes.
4. Community discussion may identify a missing route, but it is not enough by itself to mark a production knowledge record Verified.

Every imported or manually curated rule should be traceable to a source family and verification date where practical. Volatile economics such as GE prices and profit/hour should never be frozen into long-lived strategy truth.

## What a complete training-method record eventually needs

A method is more than a name and level range. Production-quality method data should be capable of expressing:

- skill and level range;
- F2P/P2P availability;
- Main/Iron/GIM/UIM compatibility;
- Hardcore and Wilderness risk;
- quest, diary, area, transport, POH, minigame, and capability requirements;
- required items and quantities;
- whether inventory, bank, equipment, Group Storage, Tool Leprechaun, STASH, POH storage, looting bag, or other storage may satisfy those items;
- acquisition alternatives when supplies are missing;
- setup time and useful session length;
- attention/intensity profile;
- cost/profit character without depending on stale prices;
- useful by-products, Collection Log, outfit, untradeable, currency, diary, CA, clue, or gear progression;
- whether the method is a long-form objective that should be protected from short-term recommendation variety;
- confidence/evidence state for the specific account.

Until those requirements are modeled, a method may exist as a useful candidate but must remain `Check Needed` when Strategist cannot prove readiness.

## Breadth regression protection

`GameKnowledgeAuditService` counts unique training-method IDs per RuneLite skill and the sizes of major catalogs such as minigames, PvM identities, progression objectives, money-making methods, and resource sources.

The audit is deliberately a **floor, not a completeness certificate**. Its purpose is to catch accidental deletion or refactors that quietly collapse an entire knowledge surface. Domain-specific tests still validate membership rules, account-mode safety, item requirements, F2P filtering, Wilderness behavior, and readiness evidence.

## Minigames and repeatable content

The minigame catalog includes combat, skilling, hybrid, PvP, distractions/diversions, utility activities, and Sailing-era repeatables. Catalog presence does not equal account access. `MinigameCandidateProvider` requires an observed unlock in `MinigameSnapshot` before an activity may compete for `DO NEXT`.

This deny-by-default behavior is intentional. It lets the identity catalog be broad while readers/evaluators become more precise over time.

## PvM

`PvmActivityCatalog` uses RuneLite's current boss hiscore enum as an automatically updating identity backbone. Hiscore identity is not the same as encounter readiness, so `PvmReadinessAnalyzer` separately evaluates levels, gear, supplies, unlocks, risk, and observed account state. Non-hiscore encounters still need explicit records where strategically relevant.

## Outfits, untradeables, and Collection Log

A training recommendation should understand when an activity is doing more than generating XP. `ProgressionObjectiveCatalog` protects useful outfit/untradeable/CLOG grinds so a short checkpoint does not tell the player to leave immediately before finishing meaningful account progression.

Future additions should link reward progress to observed currency/item/CLOG state rather than assuming an account wants every cosmetic.

## Completion workflow for a game-data batch

1. Identify the authoritative/current source and date.
2. Add or update structured records. Avoid burying facts in UI strings.
3. Add account-mode, membership, and risk flags.
4. Add typed requirements where readers exist; unresolved requirements remain `Check Needed`.
5. Add focused tests for known failure modes.
6. Run the full Java 21 CI suite.
7. Run `GameKnowledgeAuditService` breadth tests.
8. Live-test only after the batch is coherent enough that normal gameplay can expose useful behavioral bugs.
9. Never mark a domain `VERIFIED` in `GameKnowledgeManifest` merely because its architecture exists or its record count is large.

## Definition of "close to finished"

For Strategist, product completion should mean more than lots of lines of code. A near-finished domain has broad verified content, safe account-mode behavior, concrete preparation guidance, reliable observation of account state, tests for common regressions, readable UI, and a maintenance path for future OSRS/RuneLite updates.
