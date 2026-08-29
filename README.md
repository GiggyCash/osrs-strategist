# Gielinor Compass

**Your account. Your next move.**

Gielinor Compass is a local RuneLite progression planner. It examines the
account state RuneLite can safely observe and turns it into one clear **DO
NEXT** recommendation.

## What it does

Compass decides what is worth doing and which prerequisite comes first. It can
weigh skills, quests, gear, PvM preparation, Slayer, diaries, clues and STASH
units, transport unlocks, resources, and recurring opportunities in the same
decision.

The sidebar answers four practical questions:

- What should I do?
- How should I do it?
- Why this?
- What does this move me toward?

The secondary **Progress** view uses observed XP events to show session XP,
active time, a measured recent rate/ETA when enough samples exist, the current
NOW/NEXT/TARGET plan, bounded charts, milestones, and the previous session
recap. Recaps retain meaningful account unlocks even for low-XP sessions. It
remains local and character-namespaced.

Quest Helper remains the detailed quest walkthrough. RuneLite's Clue Scroll
plugin remains the detailed clue solver. Compass chooses the next useful move;
those tools help execute it.

## How DO NEXT works

Choose a goal, strategy, and session style—or leave the goal on Automatic.
Compass filters out unsafe or unavailable actions, resolves known
prerequisites, and ranks the remaining actionable choices. Details gives a
short reason and blocker; Method Guidance gives a compact bring/where/do
checklist.

Later, Not Today, Dislike, and Do This teach Compass your preferences for the
current account. You can reset that learned feedback without resetting account
evidence or unrelated RuneLite settings.

## Supported accounts

Compass keeps Main, Ironman, Ultimate Ironman, Group Ironman, Hardcore Group
Ironman, Unranked Group Ironman, and Hardcore Ironman rules distinct where the
game mechanics differ. It also protects restricted builds when a route could
cause irreversible experience or break the build.

Unknown membership and unobserved storage fail closed. Group Storage counts
only when enabled and observed. UIM storage is treated as retrieval/setup work,
not as an ordinary bank.

## Strategy and session modes

- **Efficient** favors time-saving, high-value progression.
- **Balanced** trades some speed for sustainable, practical routes.
- **Relaxed** favors lower-fatigue and lower-intensity choices when sensible.

Short, AFK, Focused, and Long session choices affect suitable actions across
the planner, not only the wording of training methods.

## Privacy and safety

Planning is local. Compass makes no runtime network requests and does not send
gameplay account evidence to a hosted Compass service. It does not automate
gameplay, inject input, or expose raw RuneLite account hashes.

## Known limitations

Compass cannot observe every game state. It may ask you to open your bank or
confirm an unlock, POH feature, STASH unit, or other state before it can safely
lead with that action. It does not observe player mechanical skill and does not
claim exact variable rates, RNG drops, or completion times.

The repository-backed coverage census is maintained in
[docs/CONTENT_CENSUS.md](docs/CONTENT_CENSUS.md).

## Reporting a bad recommendation

Please use the **Bad Recommendation** issue template and include the selected
goal, strategy, session, account mode, membership, and what Compass suggested.
Never include passwords, authentication data, or raw account identifiers.

## Support Gielinor Compass

Core planning is free and does not depend on donations. The optional in-plugin
**Support Compass** link remains hidden until a real destination is configured.
Bug reports and normal help belong on the
[GitHub Issues page](https://github.com/GiggyCash/osrs-strategist/issues).

## Development

The first public release is `0.2.0`. The project uses Java 11-compatible source
and RuneLite's standard Plugin Hub build. From the repository root:

```sh
./gradlew clean test --warning-mode all
./scripts/check-content-census.sh
git diff --check
```

Content maintenance is documented in [docs/MAINTENANCE.md](docs/MAINTENANCE.md),
the implemented planning/progress seams in
[docs/PRODUCT_COMPLETION_ARCHITECTURE.md](docs/PRODUCT_COMPLETION_ARCHITECTURE.md),
the current Plugin Hub review in
[docs/PLUGIN_HUB_AUDIT.md](docs/PLUGIN_HUB_AUDIT.md), and publication steps in
[docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md).

Stable compatibility identifiers intentionally retain the internal
`osrs-strategist` name.
