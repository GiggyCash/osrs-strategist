# Game Knowledge Completeness

OSRS Strategist is intentionally broad. That makes completeness discipline more important than raw line count.

The product must never claim that a broad category such as PvM, clues, training methods, or minigames is complete merely because a few representative records exist. The production code therefore has two layers of coverage tracking:

- `GameKnowledgeDomain` is the architectural map.
- `GameKnowledgeArea` is the granular product-completeness checklist.
- `GameKnowledgeCoverageRegistry` records whether an area is Scaffolded, Partial, or Verified.

## Meaning of coverage states

### SCAFFOLDED

A typed home or design path exists, but production data is not yet broad enough for Strategist to make comprehensive claims.

A scaffolded area may still have experimental or isolated records. Those records must not cause the UI to imply that the entire area is understood.

### PARTIAL

Useful production behavior exists and is validated, but meaningful OSRS content remains uncovered.

Examples include a skill with several valid methods but missing niche methods, a PvM catalog with several bosses but missing loadouts or mechanics, or clue support that recognizes tiers but does not yet model every step.

### VERIFIED

The feature has sufficient production data, account-state evidence, safety policy, and tests for the product to depend on it as a stable subsystem.

For game-data-heavy areas, VERIFIED should be used sparingly. A few passing tests are not enough if the underlying content set is not substantially complete.

## Definition of done for a training-method area

A skill-training area should not move to VERIFIED until all of the following are true:

1. Level-band coverage is continuous from the first trainable level through 99 where applicable.
2. Important efficient, balanced, relaxed, AFK, low-cost, and account-type-specific methods are represented.
3. F2P and members availability is explicit.
4. Main, Ironman, GIM, UIM, and Hardcore policy differences are accounted for where relevant.
5. Quest, diary, region, transport, item, currency, minigame, and other unlock requirements are structured rather than hidden in prose.
6. Supply-based methods use observed inventory/bank/storage evidence and never assume ownership.
7. Known missing supplies have an acquisition path or an explicit Check Needed state.
8. Wilderness or irreversible-risk methods are gated by explicit policy.
9. Session-intent scoring has tests for short, one-hour, long, and AFK sessions where relevant.
10. Multiple methods in the same level band are compared rather than whichever record happens to be listed first.
11. Method names and instructions are readable in the RuneLite sidebar without clipping.
12. A representative set of real account snapshots has regression tests.

## Definition of done for PvM

PvM coverage should include, as applicable:

- access and quest requirements
- minimum practical combat capability
- realistic gear progression, not only theoretical BIS
- food, potions, runes, ammunition, charges, and other consumables
- prayer/spellbook requirements
- transport and banking routes
- encounter mechanics and role expectations
- death/risk policy, especially Hardcore and UIM
- account-mode-specific acquisition paths
- combat achievement interaction
- collection-log and progression relevance
- expected session length and attention demand

A boss name in a catalog is not sufficient for VERIFIED PvM coverage.

## Definition of done for clues

Clue support should eventually model:

- every tier
- membership gating
- observed clue ownership
- current step when safely observable
- item/equipment requirements
- emotes and STASH interactions
- spade requirements
- transport requirements
- combat encounters
- Wilderness steps and Hardcore warnings
- UIM inventory/storage pressure
- age-based priority without spam
- collection-log influence

The live F2P hard-clue regression is a reminder that membership gating must apply at every layer: reader, candidate, signal, and opportunity presentation.

## Definition of done for minigames and skilling bosses

Each activity should define:

- entry requirements
- useful reward currencies
- reward unlocks and outfits
- skill XP/reward relevance
- account-mode restrictions
- supply requirements
- session length and intensity
- whether the activity supports a long-form objective that should not be interrupted by short checkpoints

## Definition of done for outfits and untradeables

Outfits are not merely collection-log entries. Strategist should know whether the outfit:

- improves XP or yield
- unlocks a diary/quest/other progression requirement
- is worth finishing once partially owned
- has storage implications for UIM
- changes the best method for a skill
- has an alternate acquisition route

## Definition of done for economy/resource routing

Main accounts should not be treated as having infinite GP. Iron accounts should never receive GE acquisition logic. UIM should never be assumed to have bank access.

Every resource path should distinguish:

- confirmed inventory
- confirmed equipment
- observed bank
- observed Group Storage
- verified UIM storage
- unverified/unknown state
- shop source
- monster drop
- world spawn
- processing chain
- GE purchase where permitted
- money-making prerequisite when liquidity is insufficient

## Product-quality rule

When Strategist does not know enough, the correct answer is not to guess. The correct answer is a concise `Check before starting` state that names the unresolved requirement.

That is preferable to a confident but incorrect recommendation.
