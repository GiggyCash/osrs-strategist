# Gielinor Compass Product Specification

## Product statement

Gielinor Compass is a local, adaptive progression planner that understands the current RuneScape account and helps the player decide what to do next.

It is an adviser, not an automation tool. It never clicks, moves, banks, fights, or performs gameplay actions.

**Compass decides. RuneLite helps you execute.** Compass selects the worthwhile
quest, method, purchase, or activity and proves whether the account is ready.
Quest Helper, encounter helpers, and the normal trading interfaces remain the
right tools for walkthroughs, mechanics, and game interaction.

## Primary UX

The normal sidebar should remain compact:

1. Account and membership state.
2. Big Goal / Current Mission.
3. Strategy style and session intent.
4. One `DO NEXT` recommendation.
5. Best method and confidence.
6. A short readiness/prep preview.
7. Later / Not Today / Dislike feedback.
8. Two good alternatives.
9. Ready opportunities.

Detailed reasoning belongs behind `Details`. Active methods may also expose a movable in-game checklist, similar in purpose to other RuneLite helper overlays.

There is no `Do This` button. Compass follows live account state and detects natural progress/completion.

## Recommendation behavior

`Pick for Me` ranks all currently eligible work and chooses one best move. Eligibility is decided before scoring. Membership, account mode, known blockers, Wilderness policy, and required capabilities can therefore remove impossible methods before preference/style scoring.

`Surprise Me` may eventually choose among a high-quality useful subset for variety without intentionally selecting poor progression.

### Feedback

- `Later`: snooze the activity for 1 hour. No long-term preference penalty.
- `Not Today`: suppress for 24 hours. No long-term preference penalty.
- `Dislike`: persistent negative preference plus a short cooldown. Repeated dislikes can lower the hidden preference weight, but strategically necessary content can still eventually override preference.

Cooldown and learned preference are separate concepts.

## Strategy personality

- Efficient: prioritizes time efficiency and strong opportunity cost.
- Balanced: mixes progress, convenience, and variety.
- Relaxed: accepts slower methods when comfort/attention profile is substantially better.

Session intent can be Quick 20 Minutes, One Hour, Long Session, AFK, or Pick for Me.

Quest tolerance can be Low, Normal, or High.

## Confidence and evidence

Every meaningful requirement should resolve to one of:

- Verified.
- Check Needed.
- Blocked.

`Check Needed` means Compass knows what fact is missing but has not proven it. It should attempt live state, quest state, remembered access, inventory, last observed bank, storage, and other verified evidence before asking the player.

Positive observations may be remembered per character. Lack of observation is never proof that something is unavailable.

## Membership

F2P/P2P is automatic account state. A F2P account receives only F2P skills and F2P-compatible methods. A P2P account may consider the full method pool, still subject to actual requirements and account state.

## Account modes

### Main

The GE is an available acquisition family, not infinite resources. Before recommending a purchase, Compass should compare:

- Required quantity.
- Inventory/bank supply.
- Verified GP.
- Verified price.
- Buy time versus self-source time.
- Money-making opportunity cost when cash is short.
- Protected/valuable items before any sale suggestion.

A cash shortage must not automatically produce a `sell gear` recommendation.

### Ironman / Hardcore Ironman

Self-source items through verified gathering, shops, crafting, minigames, drops, or other legal sources. Hardcore variants receive increased risk sensitivity.

### Group Ironman

Default to Ironman-like planning. Group Storage can be used only when the player enables it and the relevant state/items have actually been observed. `Ask teammates` is not a normal recommendation.

### Ultimate Ironman

UIM is a first-class account mode, not Ironman minus a bank.

- Never route through a normal bank.
- Evaluate inventory pressure.
- POH storage, STASH, Tool Leprechaun, looting bag, seed box, herb sack, death storage, and deathpile are capability-gated.
- An observed capability does not prove every item is compatible with it.
- Item compatibility and current capacity/preconditions must be verified before proposing a storage route.
- Death storage/deathpile require explicit risk treatment.
- Observed contents in verified storage can count toward resource readiness.

## Wilderness

Wilderness methods are an explicit per-character option and default OFF. When disabled they are hard-filtered before scoring. When enabled they become eligible but still require normal requirement/risk evaluation.

## Goals

Compass uses Big Goal and Current Mission layers rather than one giant fixed guide.

Typed dependency paths exist for Max, Quest Cape, Barrows Gloves, Prifddinas, Bowfa, Infernal Cape, Diary Cape, Elite Combat Achievements, Raid Ready, 2000 total, 85 Slayer, Base 70s, gear targets, and custom goals.

The graph describes dependency families. Structured game data supplies exact quests, items, activities, skill levels, and resource requirements over time.

## Milestones and longer progression objectives

Short skill checkpoints provide momentum and a small reward popup when completed naturally.

A short checkpoint must not knock the player off a meaningful longer objective. Examples include Graceful, Prospector, Raiments of the Eye, Smiths' Uniform, Tempoross rewards, Wintertodt rewards, useful untradeables, gear targets, and Collection Log progress.

If the longer objective is known incomplete, the usual short-term variety penalty is suppressed. If completion is unknown, behave conservatively. If completion is explicitly verified, normal reranking may resume.

## Recurring opportunities

One Opportunity Engine handles recurring/cooldown content. An opportunity appears only after its availability/timer has been observed or otherwise verified.

Planned/typed opportunity families include birdhouses, herb runs, tree runs, farming contracts, Tears of Guthix, Kingdom, Kingdom approval, battlestaves, dynamite, daily diary rewards, clues, and future cooldown content.

### Farming runs

Run guidance can track supported observed patches as planted/growing, ready, empty, diseased, dead, or Check Needed. Optimal means the best confirmed/reachable set using current evidence, not an invented teleport route.

Preparation checks include suitable seeds/saplings, tools, compost where appropriate, reachability, transport, and Tool Leprechaun state when observed.

### Clues

Clues age upward in priority without becoming spammy. Checklist reasoning can include equipment, spade, transports, food, combat, STASH state, account mode, and inventory pressure. Completion/step datasets must be verified before exact guidance is shown.

## Skills and methods

Training methods are data-driven. The same evidence system applies across skills:

- Agility: level, membership, region, quest/access, Wilderness policy.
- Farming: level, patches, seeds, tools, transport, observed patch state.
- Slayer: task/master/access/gear/resources.
- Runecraft: altar/activity access and resources.
- Hunter: creature/trap/access/resources.
- Construction: POH/access/materials/budget.
- Combat/PvM: stats, prayers, spellbooks, weapons, armor, jewelry, ammo, food, potions, access, and realistic readiness.

The architecture is shared; game-data coverage expands domain by domain.

## PvM, gear, Combat Achievements, and Collection Log

PvM should never be recommended from combat level alone. A readiness assessment should consider the real setup and access requirements.

Gear progression should prefer practical upgrade ladders over blind theoretical BIS. Combat Achievement planning should prioritize reachable reward tiers and learning progression instead of prematurely suggesting mechanically difficult tasks.

Collection Log and useful reward grinds contribute to the broader mission system and can protect a useful method from trivial variety changes.

## Structured game knowledge and OSRS Wiki

The production planner consumes local structured game data. OSRS Wiki may be used as a major upstream knowledge source, but runtime recommendations should not blindly scrape pages.

Imported knowledge carries provenance and coverage state. A Wiki/API record may be staged automatically, but it must be marked verified for planning before it can affect production recommendations.

This supports update tooling that detects OSRS changes, runs tests, and prepares reviewed data updates rather than self-modifying production logic.

## Compass Plus readiness

The local core remains independent of hosted services. Current code contains a dormant entitlement/sync boundary only.

Possible future hosted capabilities include cloud profile sync, cross-device history, GIM team planning, remote reminders, a web dashboard, and optional online reasoning.

The current build has no Plus endpoint, HTTP client, auth, billing, telemetry, or data transmission. Offline local planning must continue even if Plus is later introduced or unavailable.

## Safety

- No gameplay automation.
- No guessing unknown state.
- Warn for risky/irreversible actions.
- Protect explicitly protected items from sell/drop/alch/destroy suggestions.
- Keep UIM and hardcore risk rules stronger than ordinary convenience logic.
- Keep Wilderness opt-in.
- Keep hosted/network behavior opt-in and separately reviewable if introduced later.
