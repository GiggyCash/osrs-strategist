# OSRS Strategist Foundation Map

This document is a maintainer-facing map of the plugin's architecture. The goal is to keep the reasoning engine understandable even as OSRS content coverage becomes very large.

## Core data flow

```text
RuneLite / player-confirmed observations
        |
        v
Readers + ObservedStateStore
        |
        v
StrategyDataAssembler
        |
        v
StrategyDataBundle
        |
        v
StrategyContext
        |
        v
StrategyEngine
   |         |          |
   v         v          v
Recommendation  Opportunity  Strategy modules
Engine          Engine        + signals
        \        |        /
         \       |       /
          v      v      v
           StrategyResult
                |
                v
        Compact RuneLite panel
        + optional Details view
```

## What is live now

The foundation can already read or persist these pieces of state:

- Player name, account type, total level, skill levels, and skill XP.
- Inventory and equipment while RuneLite exposes their containers.
- A bank snapshot only after RuneLite has actually exposed the bank container. An unopened bank is never treated as an empty bank.
- Per-character recommendation preferences and cooldowns.
- Per-character explicit strategy settings through `PlayerStrategyProfile`.
- Strategy style, session intent, quest tolerance, active big goal, Group Storage preference, and collectionist weighting.
- Skill recommendations, starter training-method selection, milestone momentum, and immediate feedback rotation.

## Scaffolded systems awaiting verified readers/game data

These systems now have typed homes in the architecture but must not be described as fully implemented until their readers and game data are verified:

- Quest and miniquest progression.
- Achievement Diaries.
- Clue and STASH state.
- Combat Achievements.
- Collection Log opportunity scoring.
- Main-account economy, GE decisions, protected-item rules, high alchs, and money making.
- GIM Group Storage item observations.
- UIM storage/capability state, including Tool Leprechaun, STASH, looting bag, POH storage, death storage, and deathpile safety.
- Farming patches, Tool Leprechaun contents, herb/tree runs, and farming contracts.
- Sailing ports and activities.
- Slayer task/master/points state.
- PvM readiness, bosses, raids, gear ladders, and practical upgrade paths.
- Minigame unlocks and currencies.
- Transport routes and POH furniture.
- Broader recurring opportunities such as Tears of Guthix and Kingdom.

## No-guessing rule

`UNKNOWN`, `CHECK_NEEDED`, and `BLOCKED` are intentional states.

Do not write logic that silently turns an unobserved source into an empty or unavailable source. Examples:

- Bank not opened != empty bank.
- Group Storage not inspected != empty Group Storage.
- Tool Leprechaun access != every tool is stored there.
- A possible POH furniture upgrade != the player already built it.
- A Sailing activity existing in OSRS != this character has unlocked it.

When RuneLite cannot verify a state, either leave it unknown or ask the player to confirm it once and persist that confirmation.

## Account-mode rules

Use `AccountModePolicy` instead of scattering restrictions throughout planners.

- Main may use the GE, but purchases still need economy validation.
- Iron-like accounts self-source by default.
- GIM may use Group Storage only when the option is enabled and the storage has actually been observed.
- UIM storage routes require verified capabilities.
- HCIM/HCGIM/UIM are treated as risk-sensitive for irreversible or dangerous recommendations.

No planner should automate clicks, movement, combat, banking, or gameplay interaction. Strategist is an adviser only.

## UI rule

The sidebar is intentionally concise. Default recommendations should show:

1. What to do.
2. The target/checkpoint.
3. Best method.
4. Attention/confidence.
5. A short prep preview.

Detailed instructions and deeper reasoning belong behind `Details`. The engine may become sophisticated without making the default panel a wall of text.

## Adding future OSRS content

Prefer this order:

1. Add or update structured game data.
2. Add a reader only if new live account state is required.
3. Add a strategy module only if the content introduces a genuinely new reasoning domain.
4. Add fake-account tests.
5. Let GitHub Actions compile/test before local RuneLite testing.

The long-term maintenance goal is for most Jagex content updates to change data definitions and tests rather than the central strategy algorithm.
