# Gielinor Compass Content Coverage

This file describes real production coverage, not just type/class availability.

## Account policies

### Main
- GE is available as an acquisition option, but purchases require price, cash, and opportunity-cost validation.
- Gear-sale advice is separate from purchase advice and remains protected by item-safety rules.
- Money-making methods that depend on changing prices stay `Check Needed` until live economics are available.

### Ironman / GIM
- Self-source routes are preferred over GE assumptions.
- GIM may count Group Storage only when enabled and actually observed.
- Common resources now have concrete gathering/shop/crafting/minigame/drop-family routes instead of a generic `SELF_SOURCE` message.

### UIM
- Normal bank state is ignored for resource acquisition and generic PvM readiness.
- Inventory plus explicitly observed UIM storage capabilities form the usable resource model.
- Looting bag, death storage, and deathpile contents prove an item exists but do not prove it is immediately safe/retrievable.
- Common resource routes favor just-in-time acquisition/processing and explicitly avoid bank-dependent plans.
- Clue detection ignores normal bank state.

### HCIM / HCGIM
- Wilderness training is rejected by default even if the global Wilderness option is enabled.
- High/irreversible-risk training is rejected by default.
- Boss recommendations are deny-by-default unless the encounter is explicitly classified safe enough or a future player override is added.
- High-risk Wilderness resource routes and money methods are filtered.

## Skills and training methods

All current `Skill.values()` have curated Compass methods, including Sailing.

Methods carry:
- minimum/maximum levels
- F2P/P2P availability
- SWEATY / EFFICIENT / BALANCED / RELAXED / AFK profile
- broad cost tier
- attention level
- self-source friendliness
- UIM friendliness
- Hardcore safety
- Wilderness risk
- observable requirements

Major skills contain multiple real alternatives rather than one fixed route. Examples include tick-manipulation, conventional efficient, budget/self-source, relaxed, AFK, minigame, and progression-unlock methods where applicable.

For additional breadth, `RuneLiteSkillActionCatalog` dynamically adapts RuneLite's maintained skill-calculator actions for Agility, Cooking, Construction, Crafting, Firemaking, Fishing, Fletching, Herblore, Hunter, Magic, Mining, Prayer, Runecraft, Smithing, Thieving, and Woodcutting. Compass's curated method layer decides which action is sensible for the account instead of treating every calculator action as a recommendation.

## F2P

Curated F2P routes exist for every F2P skill. Method-level membership is checked so a members-only method cannot leak into an F2P recommendation merely because the skill itself exists in F2P.

Current PvM catalog marks Obor, Bryophyta, and Brutus as F2P boss content. Other boss definitions are filtered from F2P.

## Quests

`LiveQuestStateReader` consumes every quest in RuneLite's current `Quest.values()` and records live COMPLETE / IN_PROGRESS / NOT_STARTED state.

`QuestCandidateProvider` can place unfinished quests into the same `DO NEXT` queue as skills. In-progress quests receive momentum; major progression quests receive explicit unlock weighting; Quest Cape, Barrows Gloves, and Prifddinas goals add goal-specific weighting.

Not every quest's complete item/step/prerequisite graph is yet hand-authored in Compass. A not-started quest therefore remains `Check Needed` until its requirements are verified instead of being falsely presented as ready.

## Achievement Diaries

`LiveDiaryStateReader` reads all 12 diary regions and all four tiers, giving 48 live tier-completion states:

- Ardougne
- Desert
- Falador
- Fremennik
- Kandarin
- Karamja
- Kourend & Kebos
- Lumbridge & Draynor
- Morytania
- Varrock
- Western Provinces
- Wilderness

The main queue can recommend the next unfinished tier. Wilderness diaries respect Wilderness policy.

Individual task text and every per-task prerequisite are not yet a complete embedded 492-task dataset, so a tier stays `Check Needed` until task readiness is known.

## Combat Achievements

Live reward-tier state is read for Easy, Medium, Hard, Elite, Master, and Grandmaster. The candidate engine targets the next reward threshold and increases priority near a threshold.

The exact full Combat Achievement task database is not yet embedded. Compass is deliberately designed to prefer tasks on bosses the account is already ready to fight rather than blindly pushing mechanically extreme tasks.

## PvM, bosses, and raids

`PvmActivityCatalog` dynamically enumerates every RuneLite `HiscoreSkill` whose type is `BOSS`. This keeps identity coverage aligned with the RuneLite version in use, including raids and newly added hiscore bosses.

Metadata includes:
- raid status
- Wilderness status
- F2P status
- risk class
- conservative Hardcore policy

`PvmReadinessAnalyzer` builds a readiness result for every catalog boss. It has explicit floors/access rules for many major encounters (including Barrows, Scurrius, Zulrah, Vorkath, Gauntlet/CG, CoX, ToB, ToA, Slayer bosses, Jad/Zuk, Nex, GWD, DT2 bosses, and more) plus a conservative fallback for current/future identities.

Readiness evaluates combat stats, known quest/access gates, observed combat equipment, basic supplies, membership/account mode, and risk policy. UIM never becomes ready because of a normal bank cache.

This is broad real PvM coverage, but not yet a hand-authored phase-by-phase DPS/loadout/special-attack model for every encounter.

## Gear and BIS

Gear is modeled by combat style, use case, and budget tier rather than one universal BIS list.

Current ladders include:
- F2P melee/ranged/magic
- budget melee/ranged/magic
- midgame progression
- Bowfa/crystal progression
- high-end stab/ranged
- contextual slash/crush BIS
- contextual ranged BIS
- contextual magic BIS
- raid hybrid budget/BIS switches

BIS entries explicitly remain target/room/phase/setup dependent. Before an actual acquisition recommendation, Compass still checks owned equipment, account mode, resource route, Main GP/economics, and encounter context.

## Clues and STASH-aware planning

`LiveClueStateReader` detects actual clue scrolls in observed inventory and, for non-UIM accounts, an observed bank. It recognizes Beginner/Easy/Medium/Hard/Elite/Master and keeps clue age stable. It preserves prior clue state during challenge/puzzle intermediate items and never treats an unopened bank as proof that a clue vanished.

Clue recommendations are tier-aware, become gradually more important with age, receive Collectionist weighting, and carry explicit UIM and Hardcore safety guidance.

Exact every-step equipment/STASH/coordinate solution data is not yet duplicated from RuneLite's clue system. Current clue readiness remains `Check Needed` until the active step's requirements are verified.

## Collection Log and progression objectives

Observed category totals/completion counts can produce near-complete Collection Log recommendations. Missing one, two, or three slots receives increasing priority; Collectionist mode broadens the behavior.

Protected long-form objectives include a growing catalog of useful outfits/untradeables such as Graceful, Prospector, coal/gem bags, Raiments, GOTR uniques, Smiths' Uniform, Tempoross/Wintertodt rewards, Farmer outfit, seed/herb storage rewards, Carpenter outfit/plank sack, Rogue/Angler/Lumberjack/Void, Fighter torso, Dragon defender, rune pouch, Slayer helmet, diary utility items, Bowfa/crystal, Barrows, Moons equipment, and others.

## Minigames and skilling bosses

The catalog includes major progression activities such as Wintertodt, Tempoross, GOTR, Giants' Foundry, Mahogany Homes, Tithe Farm, Pest Control, Barbarian Assault, Fishing Trawler, Rogues' Den, Mage Training Arena, Volcanic/Blast Mine, Hallowed Sepulchre, Pyramid Plunder, Soul Wars, LMS, Castle Wars, Trouble Brewing, Shades of Mort'ton, Brimhaven Agility Arena, Temple Trekking, Warriors' Guild, Motherlode Mine, Barracuda Trials, Deep Sea Trawling, and more.

A minigame becomes a main-queue candidate only after its unlock is observed/verified.

## Money making

The stable catalog includes F2P, skilling, recurring, Iron-friendly, PvM, raids, and Wilderness money/resource methods. It intentionally does not hard-code GP/hour because market values change.

Main methods that depend on economics require live-price verification. Iron/UIM methods are judged for resource value and self-sourcing rather than pretending GE value is the account's objective.

## Resource acquisition

`ResourceSourceCatalog` supplies common concrete routes for logs/planks, ores/bars/cannonballs, seeds/herbs/secondaries, runes/essence, food, glass, bowstrings, feathers, gems, bones, and related progression inputs.

Matching uses resource phrases rather than loose substrings so equipment such as a rune weapon or Barrows item is not mistaken for a rune/bar material.

## Niche/restricted builds

`RestrictedBuildDetector` can conservatively suggest:
- Skiller
- F2P Skiller
- 1 Defence Pure
- Defence Pure
- Zerker
- 10 Hitpoints build

These are suggestions only. A normal developing account can temporarily resemble a pure, so Compass must get player confirmation before protecting a stat cap or excluding progression that would train it.

## Remaining deep-data work

The architecture is no longer the main blocker. The remaining work is primarily deeper structured content:

- exact prerequisite/item/step graph for every quest
- all 492 individual diary task definitions and readiness checks
- every individual Combat Achievement task and mechanic
- full per-boss phase/gear/DPS/special-attack/consumable modeling
- every clue step and STASH requirement integrated directly into Compass state
- every Collection Log item/source/drop relationship
- exhaustive item-source/shop/drop-rate database
- live Main price/GE economics
- more live readers for minigame currencies/unlocks, Slayer state, Sailing unlocks, storage, and Collection Log categories where RuneLite does not expose the state continuously

Until one of these is verified, `Check Needed` is the correct result. The plugin must never fill a knowledge gap by guessing.
