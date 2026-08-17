# Beta Content Coverage

This file is the current engineering checklist for content depth. It exists to keep "broad coverage" separate from "fully validated edge-case coverage."

## Membership and account modes

Implemented safety boundaries:

- F2P methods fail closed when membership is F2P or temporarily unknown.
- P2P accounts may use both F2P and members methods.
- Main accounts may use tradeable/GE acquisition only when price/cash logic is sufficiently verified.
- Ironman, Hardcore Ironman, GIM, HCGIM and UGIM use self-source semantics.
- GIM Group Storage counts only when enabled and observed.
- UIM normal bank state never counts as usable storage.
- UIM retrieval-only storage remains distinct from directly usable supplies.
- Restricted builds remain independent of account mode.

## Restricted-build safety

Current hard filtering includes standard, 1 Defence/pure families, Defence pure, skiller/F2P skiller, Prayer skiller, 10 Hitpoints and other configured pure constraints.

Quest and upgrade candidates must also pass build safety. A high score is never permission to break the account.

## Skill method coverage

The expanded training catalog contains broad F2P/P2P routes across every trainable skill and the test suite checks for level-band holes. Deterministic methods flow into the adaptive action/resource planner where data is reliable.

Special planners exist for variable or nonstandard activities including:

- Wintertodt
- Tempoross
- Guardians of the Rift
- Motherlode Mine
- Shooting Stars
- Volcanic Mine
- Blast Mine
- Giants' Foundry
- Mahogany Homes
- Mastering Mixology
- Tithe Farm
- Hunter Rumours
- Slayer
- Sailing
- direct combat

Variable methods report exact known milestone XP but do not invent fake game/kill counts.

## Exact resource planner

Supported principles:

- exact XP remaining
- exact action count when XP/action is deterministic
- exact multi-input recipes when modeled
- inventory/equipment/bank/group-storage accounting
- Main purchase shortfall
- Iron self-source shortfall
- UIM usable-vs-retrieval storage separation
- reusable/infinite elemental-rune sources only when actually usable
- full XP outfit modifiers where modeled
- burn/random-success methods handled by specialized logic rather than false precision

If recipe/input data is not proven, the exact planner fails closed instead of fabricating supplies.

## Slayer depth

Live task name, remaining count, master and observed location are supported.

Task-specific mechanics currently include conservative profiles for common mechanically constrained tasks such as:

- Dust devils
- Aberrant spectres
- Gargoyles
- Kurasks
- Turoths
- Banshees
- Cockatrices
- Basilisks
- Wall beasts
- Rockslugs
- Desert lizards
- Nechryaels
- Bloodvelds

The long-term Slayer content target is every task family, variant/location tradeoff, protection requirement, cannon/barrage suitability, drop-goal interaction, skip/block/extend state and account-specific gear setup.

## Gear and progression upgrades

Current actionable acquisition routes include or have foundations for:

- Fighter torso
- Abyssal whip
- Dragon defender
- Barrows gloves
- Fire cape
- Bow of faerdhinen acquisition/creation chain
- Angler outfit

The broader `GearProgressionCatalog` covers F2P through high-end combat tiers and raid-oriented sets. Encounter-specific true BIS remains intentionally separate because target defence, size, resistances, mechanics, specials, inventory and cost can change the answer.

## Recommendation intelligence

The global queue now compares the complete legal skill pool with quest, gear, detour, diary, minigame and PvM candidates before selecting the top three.

Primary DO NEXT must be actionable. Goal alignment, session fit, resource readiness, risk, account mode, UIM setup cost, player feedback and fatigue all influence close decisions.

## UI

Current information layers:

1. Sidebar: compact decision and feedback surface.
2. Movable Details overlay: full planner explanation.
3. Method guidance overlay: immediate execution/checklist view.

Do not solve future clipping by shrinking fonts. Keep long text out of the narrow sidebar.

## Still requiring deep beta passes

Broad coverage is not the same as finished content. The remaining long-tail work includes:

- every niche training route and method transition
- all Slayer task families and location variants
- encounter-specific gear/BIS and acquisition chains
- richer minigame currencies/reward state
- more quest prerequisite-chain reasoning
- diary prerequisite decomposition
- more transport/POH/live unlock state
- deeper UIM storage/setup transitions
- Iron resource-source chains and drop goals
- charge/ammunition/degradation-aware gear planning
- large simulated account matrix and real-play regression fixes

New content should be added with source validation and tests. Never fill coverage gaps with guessed OSRS facts.
