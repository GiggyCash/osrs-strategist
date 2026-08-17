# Recommendation Intelligence

## Purpose

OSRS Strategist must answer a harder question than "what training method is good?"

It must decide which legal, useful, executable account action should happen next after comparing skills, quests, upgrades, resource detours, diaries, minigames, PvM and account goals.

## Queue invariants

1. Membership and restricted-build legality are hard gates before ranking.
2. BLOCKED content never enters the player queue.
3. A true unknown requirement cannot lead DO NEXT.
4. A preparation requirement can lead only when the preparation itself is explicit and actionable.
5. All legal skill candidates enter the global pool before it is trimmed to three cards.
6. Non-skill candidates can lead only when VERIFIED and backed by concrete action guidance.
7. The final ranking is account-value ranking, not raw provider score alone.

## Account-value dimensions

`RecommendationIntelligenceService` evaluates:

- provider/raw opportunity score
- readiness and confidence
- active goal alignment
- session length and attention fit
- account mode
- resource readiness
- Wilderness and Hardcore risk
- detour opportunity cost
- UIM setup and retrieval cost
- explicit player feedback and temporary score adjustments

The scoring layer is intentionally inspectable and additive. A future online/premium reasoning service may explain or tune these dimensions, but it must not replace local membership/build/actionability safety gates.

## Goal behavior

Goal boosts are contextual rather than absolute. Examples:

- `MAX` continues to value direct skill XP while allowing justified unlock/resource detours.
- `QUEST_CAPE` strongly values executable quest progress.
- `BARROWS_GLOVES` prioritizes the Recipe for Disaster chain and the final glove purchase.
- `PRIFDDINAS` values Song of the Elves and its requirements.
- `BOWFA` values the Prifddinas/Enhanced seed acquisition chain.
- `SLAYER_85` strongly favors Slayer and whip progression.
- `GEAR_TARGET` and `RAID_READY` allow verified equipment upgrades to beat generic skill milestones.

A goal boost never makes an illegal or unresolved action executable.

## Player fatigue and variety

`TrainingFatigueTracker` observes real XP activity during the current client session.

- Efficient mode does not apply a psychological variety penalty.
- Balanced mode may softly rotate a skill after sustained continuous training.
- Relaxed mode rotates earlier and more strongly.
- Hitpoints XP is ignored for continuity because it is commonly passive while training a chosen combat skill.
- A long XP gap or a skill switch resets continuity.

This is temporary preference state, not a permanent dislike.

## UIM opportunity cost

`UimSetupCostService` treats setup as account progress cost.

Observed conditions that can reduce a candidate's value include:

- high setup time
- a nearly full inventory
- active death storage
- an active deathpile
- a populated looting bag
- a dangerous-death activity while retrieval storage is populated
- Main-style bank/Grand Exchange assumptions

UIM-aware just-in-time and setup-preserving instructions receive a small practicality credit.

Unknown storage is never assumed empty.

## UI contract

The narrow RuneLite sidebar is a decision surface, not the full guide.

It should show:

- DO NEXT title
- compact best-method/status information
- one useful action sentence
- compact supply state
- feedback controls
- Details button

The movable Details overlay owns the complete action, supplies, location, notes, readiness evidence and longer explanation. The live method overlay owns the immediate checklist while playing.

This split prevents the old clipped wall-of-text failure from returning.
