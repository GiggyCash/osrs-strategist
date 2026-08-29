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

## Feedback and account-state transitions

Later, Not Today, Dislike, and Do This are applied to the deduplicated semantic
action, so the same recommendation cannot rebound immediately under another
provider ID. Later remains a short cooldown, Not Today remains day-scale,
Dislike adds a durable negative preference, and Do This adds a positive signal
without hiding the action.

Preference, strategy, milestone, and recommendation-history documents remain
in RuneLite's per-character `osrs-strategist-profile` group. Live observation
caches have an independent raw-account-hash boundary; the hash is used only for
equality and is never rendered or logged. The state-sequence matrix covers bank
observation, item acquisition, quest completion, changed gear, feedback
rotation, logout/relogin restoration, and A/B/A account isolation.

## Skills and training methods

All current `Skill.values()` have curated Compass methods, including Sailing.
The development census currently records 178 distinct strategic methods across
24 skills plus 1,528 RuneLite calculator actions used only as execution
evidence. Current-live level overrides are applied explicitly when the pinned
RuneLite calculator lags a verified game update.

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

Sailing uses a separate live boundary because RuneLite has no calculator enum
for it. Pandemonium completion proves the exact Port Sarim-Pandemonium starter
courier loop; pinned RuneLite varplayers prove owned boat slots, active port
tasks, and completed Barracuda Trials. Individual chart checkboxes and fitted
boat components remain hard evidence/preparation gates where they cannot yet be
mapped safely.

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

The pinned RuneLite task catalog contains all 378 current task rows across the
12 regions and 48 tiers. Direct skill, quest, combat-level, and quest-point
requirements are structured; nested RuneLite alternatives stay explicit
`Check Needed` evidence. Because RuneLite exposes tier completion rather than
every individual task's live completion state, Compass tells the player to
check the selected task before following its first known prerequisite.

## Transportation

The structured catalog covers 41 high-value reusable systems across fairy
rings, spirit trees, gnome transport, minecarts, boats/charters, item and
spellbook teleports, jewellery, diary/minigame/quest/Slayer routes, POH routes,
and current Sailing transport. Each system records membership, quests, skill,
item/access setup, Wilderness risk, and reusable fan-out.

Only observed `TransportSnapshot` routes count as verified. Unknown membership
fails closed for members routes; Hardcore accounts do not auto-route Wilderness
teleports. POH portal, mounted, spirit-tree, and fairy-ring capabilities require
observed furniture state and are never inferred from Construction level. The
plugin can now prove tracked furniture present or absent from a complete scene
scan only in the current character's own POH build mode; public houses and
unknown ownership do not update the personal snapshot.

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

`PvmReadinessAnalyzer` builds a readiness result for every catalog boss. All 71
current identities have encounter-specific preparation covering known access,
style, mandatory setup, supplies, and risk, including Barrows, Scurrius,
Zulrah, Vorkath, Gauntlet/CG, CoX, ToB, ToA, Slayer bosses, Jad/Zuk, Nex, GWD,
DT2 bosses, and current 2026 encounters.

Readiness evaluates combat stats, known quest/access gates, observed combat equipment, basic supplies, membership/account mode, and risk policy. UIM never becomes ready because of a normal bank cache.

This is broad real PvM coverage, but not yet a hand-authored phase-by-phase DPS/loadout/special-attack model for every encounter.

Only four deliberately simple evidence profiles can become locally
`VERIFIED`; the remaining 67 are preparation-only. Complete stats and equipment
never prove player mechanical execution.

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

The decision layer explicitly separates best owned, best usable, best available
now, best value upgrade, best practical upgrade, long-term target, and
target-specific best. Compound slot descriptions and alternative sets do not
masquerade as one exact missing item. Forty-one concrete acquisition targets
recurse through the shared dependency graph instead of duplicating quest,
boss, minigame, skill, shop, or resource logic.

## Slayer

The development-time catalog is generated from all 151 canonical RuneLite
Slayer assignment identities. Every identity maps to one of 146 task-specific
profiles through 227 audited aliases. Guidance separates mandatory protection,
location/access, style, cannon and multitarget-Magic evidence, Wilderness and
boss variants, Iron objectives, and keep/extend/skip/block value. An unknown
future task still receives conservative guidance, but no current canonical task
depends on that fallback.

## Clues and STASH-aware planning

`LiveClueStateReader` detects actual clue scrolls in observed inventory and, for non-UIM accounts, an observed bank. It recognizes Beginner/Easy/Medium/Hard/Elite/Master and keeps clue age stable. It preserves prior clue state during challenge/puzzle intermediate items and never treats an unopened bank as proof that a clue vanished. Through an explicit dependency on RuneLite's Clue Scroll plugin, an opened clue also contributes its actual subtype, current solution/action, marked location, equipment requirements, spade/light requirements, enemy, Wilderness classification, and STASH identity.

Clue recommendations are tier-aware, become gradually more important with age, receive Collectionist weighting, and account for quick/AFK intent, UIM setup disruption, and Hardcore risk before interrupting a plan. Tier-only evidence asks the player to open the scroll once; exact step evidence produces one coherent PREP/HOLD/DO instruction instead of a generic clue checklist. Wilderness steps are held when risk is disabled and never become a Hardcore route merely because the step is known.

All 119 current RuneLite STASH identities are represented in the offline catalogue with tier, world coordinates, current clue/equipment evidence, Construction level, exact build materials, Wilderness classification, and fail-closed built/filled state. The development-time generator reads the pinned RuneLite sources without adding runtime networking, reflection, or process execution. RuneLite supplies the active clue step at runtime; exact per-unit STASH built/filled state remains unknown until observed and is never inferred from Construction level.

## Collection Log and progression objectives

Observed category totals/completion counts can produce near-complete Collection Log recommendations. Missing one, two, or three slots receives increasing priority; Collectionist mode broadens the behavior.

Protected long-form objectives include a growing catalog of useful outfits/untradeables such as Graceful, Prospector, coal/gem bags, Raiments, GOTR uniques, Smiths' Uniform, Tempoross/Wintertodt rewards, Farmer outfit, seed/herb storage rewards, Carpenter outfit/plank sack, Rogue/Angler/Lumberjack/Void, Fighter torso, Dragon defender, rune pouch, Slayer helmet, diary utility items, Bowfa/crystal, Barrows, Moons equipment, and others.

## Minigames and skilling bosses

The catalog includes major progression activities such as Wintertodt, Tempoross, GOTR, Giants' Foundry, Mahogany Homes, Tithe Farm, Pest Control, Barbarian Assault, Fishing Trawler, Rogues' Den, Mage Training Arena, Volcanic/Blast Mine, Hallowed Sepulchre, Pyramid Plunder, Soul Wars, LMS, Castle Wars, Trouble Brewing, Shades of Mort'ton, Brimhaven Agility Arena, Temple Trekking, Warriors' Guild, Motherlode Mine, Barracuda Trials, Deep Sea Trawling, and more.

A minigame becomes a main-queue candidate only after its unlock is observed/verified.

## Money making

The stable catalog includes F2P, skilling, recurring, Iron-friendly, PvM, raids, and Wilderness money/resource methods. It intentionally does not hard-code GP/hour because market values change.

Main methods that depend on economics require live-price verification. Iron/UIM methods are judged for resource value and self-sourcing rather than pretending GE value is the account's objective.

The production candidate boundary currently promotes only the deterministic
Agility Pyramid cash loop: non-Hardcore Iron accounts at 60 Agility or higher
receive the exact 10,000-coin top hand-in and Pollnivneach resupply route. Other
catalog identities remain hidden until their live price, access, resource, or
encounter evidence can support equally concrete guidance; a catalog row alone
is never presented as money-making advice.

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
- direct live observation of each individual diary task's completion state
- every individual Combat Achievement task and mechanic
- full per-boss phase/gear/DPS/special-attack/consumable modeling
- direct observation of per-unit STASH built/filled state (active clue-step observation and the full static 119-unit catalogue/dependency planner are present)
- every Collection Log item/source/drop relationship
- exhaustive item-source/shop/drop-rate database
- live Main price/GE economics
- more live readers for minigame currencies/unlocks, per-chart Sailing progress, specialized storage, and Collection Log categories where RuneLite does not expose the state continuously

Until one of these is verified, `Check Needed` is the correct result. The plugin must never fill a knowledge gap by guessing.
