# OSRS Strategist Game-Knowledge Implementation

This branch moves Strategist beyond architecture-only coverage and begins the production knowledge layer.

## Definition of implementation coverage

A domain is not considered implemented merely because a snapshot or interface exists. Implemented content must have:

1. a concrete data definition or reader,
2. account-mode compatibility rules,
3. confidence/no-guessing behavior,
4. ranking behavior,
5. tests covering at least Main, Iron-like and UIM/HCIM safety where relevant.

## Account modes

First-class modes:

- Main
- Ironman
- Group Ironman
- Ultimate Ironman
- Hardcore Ironman
- Hardcore Group Ironman

Optional account archetypes are inferred independently from account mode:

- Skiller
- 1 Defence Pure
- Defence Pure
- Zerker-style restricted build

Archetype inference is deliberately conservative and may return NONE/UNKNOWN when the observed stats do not cleanly match a restricted build.

## Knowledge areas

The production knowledge layer is organized around:

- Training methods and method styles
- Quest progression and prerequisites
- Achievement Diary progression
- Clues and STASH preparation
- PvM and raids readiness
- Combat Achievements
- Gear ladders and practical/BIS targets
- Money making and acquisition routes
- Minigames and collection objectives
- Transport and POH unlocks
- Farming and recurring opportunities
- Slayer
- Sailing
- Collection Log and useful untradeables

## Source policy

RuneLite is the preferred source for observable client state. OSRS Wiki and other community sources may inform static game knowledge, but imported data remains staged until validated. Wise Old Man and TempleOSRS may inform optional historical/profile enrichment; they are not required for the local planner to identify obvious restricted-build archetypes from live skill levels.

The plugin remains advisory. It never performs gameplay actions.
