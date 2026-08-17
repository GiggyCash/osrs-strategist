# OSRS Strategist Contributor Guide

This project is designed to be understandable by a new contributor without requiring them to reverse-engineer the author's intent from implementation details.

## Core product rule

Strategist is an adviser, not a gameplay automation tool.

It may read account state, rank options, explain requirements, remember preferences, and provide preparation checklists. It must not click, move the player, automate combat, automate skilling, or perform prohibited gameplay interactions.

## Architectural rule: evidence before confidence

Never infer ownership or access from convenience.

Examples:

- Do not say `use banked planks` unless the bank was observed and those planks were present.
- Do not assume a UIM can use normal bank storage.
- Do not assume a GIM teammate will provide an item.
- Do not assume a quest, diary, transport route, POH feature, STASH unit, or storage system exists unless state or policy proves it.
- Do not mark a recommendation Ready when a required state is unknown.

Unknown state should normally become `CHECK_NEEDED`. Known unmet state should become `BLOCKED`. Proven state becomes `VERIFIED`.

## Keep scoring separate from presentation

The engine may use many signals that should not become permanent UI text.

Examples include attention demand, strategy-style weights, hidden preference weights, cooldown penalties, momentum, opportunity cost, and healthy-variety adjustments.

The compact sidebar should answer only what the player needs to act:

1. What should I do?
2. What is the best method?
3. What do I still need to check or prepare?

Detailed reasoning belongs behind `Details` or in debug/test tooling.

## Account-mode policy

### Main

- Grand Exchange acquisition is allowed.
- Main accounts do not have infinite GP.
- Purchase recommendations should consider verified GP, price, time saved, and alternative self-sourcing.
- Protected items must never be casually sold.
- Selling or alching useful equipment should be conservative and explainable.

### Ironman / Hardcore Ironman

- No GE sourcing.
- Supplies need real acquisition paths.
- Hardcore recommendations require stricter risk review where death is meaningful.

### Group Ironman

- Behaves like Ironman unless verified Group Storage state supplies the item and the user allows Group Storage.
- Do not turn unknown teammate inventory into a routine recommendation to `ask a teammate`.

### Ultimate Ironman

- Never count a normal bank.
- Storage capabilities default to unknown.
- A storage route is usable only when the capability, compatibility, contents, and relevant access/risk conditions are verified.
- Looting bag, death storage, and deathpile state may prove an item exists without proving it is immediately safe/ready to retrieve.

## Membership policy

F2P and members content must be filtered at every layer that can surface or score content.

A stale observed object from a previous members session must not become an F2P recommendation merely because the reader still remembers it.

When adding new content, review:

- readers
- requirement evaluators
- candidate providers
- strategy signals
- opportunity engine
- training-method policy
- presentation
- tests

## Training methods

Training methods should be data-driven records whenever possible.

A useful method definition includes:

- stable ID
- skill
- level range
- player-facing name
- concise instructions
- Efficient/Balanced/Relaxed weights
- attention level
- minimum useful session length
- setup time
- structured requirements
- confidence
- F2P/members policy
- account-mode policy where needed
- Wilderness/risk flags
- cost/intensity metadata

Do not encode method selection as a giant chain of ad-hoc `if` statements when the method belongs in structured data.

## Recommendation scoring

Scoring should be additive and explainable.

High-priority inputs include:

- active goal dependency
- account-state readiness
- unlock progression
- meaningful opportunity cost
- session intent
- strategy style
- account type
- explicit preferences
- cooldowns
- risk policy
- long-form objective protection

Comfort/engagement adjustments should remain bounded so they cannot override critical safety or progression requirements.

## Healthy engagement

Strategist should help players enjoy OSRS, not manipulate them into endless sessions.

Good engagement behavior includes:

- respecting Later / Not Today / Dislike
- offering useful variety after repeated completions
- matching AFK requests with genuinely low-attention content
- celebrating milestones briefly without obstructing gameplay
- avoiding constant notification spam
- protecting long objectives from pointless task switching
- allowing strategic importance to override a small variety penalty

Avoid dark-pattern mechanics such as artificial urgency, fake scarcity, guilt language, or reward schedules designed solely to increase compulsive use.

## UI rules

RuneLite sidebar width is constrained.

Prefer:

- slightly larger readable type
- vertical wrapping
- short headings
- one primary recommendation
- two alternatives at most
- concise opportunity list
- detailed reasoning behind Details

Do not solve clipping by forcing large horizontal widths that may not be honored by RuneLite.

Unknown requirements should not use symbols that look like broken assets. Pair a neutral bullet with clear words such as `Check` or `Check before starting`.

## Comments and documentation

Comments should explain why the code exists, not narrate obvious syntax.

Good comment:

`// UIM storage may prove the item exists without proving safe retrieval, so restricted storage remains Check Needed.`

Poor comment:

`// Loop through items.`

When a rule exists because of a live regression, document the underlying invariant rather than only describing the one account that exposed it.

## Adding new game content

Before coding, identify the relevant `GameKnowledgeDomain` and `GameKnowledgeArea`.

Then answer:

1. What account state is required?
2. Can RuneLite observe it directly?
3. If not, can Strategist remember a verified observation?
4. What states are Verified / Check Needed / Blocked?
5. Does membership matter?
6. Does account mode matter?
7. Does Wilderness/death/irreversible risk matter?
8. Does the content require supplies or transport?
9. Is it a short action, recurring opportunity, long objective, or permanent unlock?
10. What tests prove we are not guessing?

After implementation, update the granular coverage registry only as far as the evidence warrants.

## Test expectations

Every meaningful policy change should include at least one regression test.

High-value test categories:

- Main / Iron / GIM / UIM fake snapshots
- F2P vs members
- unknown bank vs observed bank
- inventory/equipment recognition
- missing supply acquisition
- quest/access unknown vs complete
- Wilderness disabled vs enabled
- Hardcore risk
- feedback cooldown rotation
- persistent per-account profile behavior
- UI compact-copy length
- readiness markers
- long-form objective protection
- recommendation variety after completion

## Before merging

Run the Java 21 CI build and tests.

Review the changed code for:

- accidental account-state assumptions
- F2P/P2P leaks
- UIM bank assumptions
- new hardcoded strings that should be structured data
- misleading confidence labels
- clipping-prone UI copy
- unbounded score adjustments
- missing null/unknown handling
- comments that are stale or misleading
- tests that only confirm the happy path

A branch should remain draft until the exact tested head is launched in the development RuneLite client when the change affects live client behavior.
