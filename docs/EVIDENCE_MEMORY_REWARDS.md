# Evidence, Access Memory, and Milestone Rewards

This batch turns `Check Needed` from a generic warning into a concrete evidence model and adds a small reward loop for completed short-term goals.

## Evidence rule

Strategist should prefer evidence in this order:

1. Direct live account state from RuneLite.
2. Positive facts Strategist previously observed on this RuneScape profile.
3. Safe inference from verified prerequisites such as completed quests.
4. `Check Needed` when the fact cannot yet be proven.
5. `Blocked` when the account is known not to satisfy a hard requirement.

Unknown must never be silently converted to false or true.

## Quest evidence

`LiveQuestStateReader` reads RuneLite's current `Quest` states. This allows access evaluators to prove content gates without asking the player to remember quest completion manually.

## Persistent access memory

`AccountAccessMemoryStore` stores positive observations per RuneScape profile. `AccessObservationService` currently remembers every visited region and promotes known Farming regions into named Farming access facts.

This is deliberately positive-only memory. Seeing a place proves access. Not seeing a place does not prove the player cannot access it.

The same pattern can later support transports, Slayer areas, minigames, raid entrances, POH capabilities, clue locations, and other content.

## Farming example

The first deep evaluator is Farming because it combines levels, area access, quests, supplies, timers, and persistent observations.

The starter catalog includes common open-world allotment/herb patches plus quest-gated Morytania, Troll Stronghold, and Weiss access. Region IDs are based on RuneLite's FarmingWorld definitions.

A recommendation can therefore show readiness such as:

- Verified: Reachable Farming patch
- Check Needed: Seeds and farming tools

Details explain why a check is unresolved instead of only displaying the phrase `Check Needed`.

## Milestone reward popup

`MilestoneRewardOverlay` is a short-lived top-center overlay shown when the tracked recommendation checkpoint is naturally completed. It is intentionally inspired by the satisfying feel of Collection Log notifications without copying the Collection Log interface.

The popup is non-modal, lasts only a few seconds, and uses the existing milestone tracker. The same reward surface can later support quests, diaries, gear targets, combat-achievement tiers, and other short-term Strategist goals.
