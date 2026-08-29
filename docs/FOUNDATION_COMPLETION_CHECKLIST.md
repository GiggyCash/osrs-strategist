# Foundation Completion Checklist

This file distinguishes architectural coverage from exhaustive OSRS game-data coverage.

`Scaffolded` means the domain has a typed home and can enter the shared strategy pipeline safely. `Partial` means useful verified records/readers exist, but the domain is not exhaustive. A domain should only be called fully verified when its structured dataset and readers are actually complete enough to support that claim.

## Core planner

- [x] Live account snapshot and 24-skill support, including Sailing.
- [x] F2P/P2P content gating.
- [x] Main/Iron/GIM/UIM/hardcore account-mode policy.
- [x] Per-character preferences and cooldowns.
- [x] Per-character strategy profile.
- [x] Strategy styles, session intent, quest tolerance, big goals.
- [x] Wilderness methods opt-in, hard-filtered when disabled.
- [x] No-guessing evidence states: Verified / Check Needed / Blocked.
- [x] Local sidebar plus movable in-game method guidance.
- [x] Natural milestone completion detection without a Do This button.
- [x] CLOG-style milestone reward overlay.
- [x] Long-objective protection so tiny checkpoints do not interrupt Graceful/Prospector-style progression.

## State/evidence

- [x] Live skill/XP/account/membership state.
- [x] Live inventory/equipment and last observed bank state.
- [x] Live quest-state reader.
- [x] Persistent positive area-access memory.
- [x] Farming patch observation and remembered patch state.
- [x] Resource-readiness layer.
- [x] Storage capability model.
- [x] UIM-specific storage/risk gate.
- [x] UIM resource readiness ignores normal bank state.
- [x] Observed UIM storage contents can satisfy a resource only when the capability and contents are verified.
- [x] Every UIM resource decision distinguishes carried, directly usable,
  retrieval-only, and unobserved storage; systems RuneLite cannot continuously
  expose remain an explicit external observation boundary.

## Farming/run guidance

- [x] Herb/tree run checklist model.
- [x] Live planted/ready/empty/diseased/dead/check-needed patch states for observed supported patches.
- [x] Quest/access evidence and prior-access memory.
- [x] Seed/tool resource readiness.
- [x] Tool Leprechaun evidence can satisfy tool requirements when actually observed.
- [x] Supported live patches have exact access/tool/seed state and named route
  guidance; unobserved patches cannot masquerade as a complete run.

## Skills/methods

- [x] Data-driven training-method database.
- [x] Strategy/session scoring.
- [x] Membership filtering before method selection.
- [x] Wilderness filtering before method selection.
- [x] Dynamic blockers cause the selector to try the next usable method.
- [x] Farming method evaluator.
- [x] Agility level/quest/region-access evaluator.
- [x] Generic evidence/checklist pattern for every later skill.
- [x] Every current skill has reviewed progression bands, exact named methods,
  locations, requirements, transition targets, and a safe F2P boundary where
  applicable. Legacy choice-delegating rows are typed and production-ineligible.

## Goals/dependencies

Typed goal paths now exist for:

- [x] Max.
- [x] Quest Cape.
- [x] Barrows Gloves.
- [x] Prifddinas.
- [x] Bowfa.
- [x] Infernal Cape.
- [x] Diary Cape.
- [x] Elite Combat Achievements.
- [x] Raid Ready.
- [x] 2000 total.
- [x] 85 Slayer.
- [x] Base 70s.
- [x] Gear target.
- [x] Custom goal seam.

These are typed dependency families, not claims that every individual quest/item/boss prerequisite is already encoded.

## Progression objectives / CLOG

- [x] Longer objective model separate from short skill checkpoints.
- [x] Starter objective mapping for Graceful, Prospector, Raiments of the Eye, Smiths' Uniform, Tempoross, and Wintertodt progression.
- [x] Explicit objective-complete state can release milestone protection.
- [x] Unknown completion state remains conservative.
- [x] Reviewed actionable outfit/currency objectives are typed and protected;
  category totals without an exact missing-item route never enter the queue.

## Opportunities

Generic timer engine supports observed-state entries for:

- [x] Birdhouses.
- [x] Herb runs.
- [x] Tree runs.
- [x] Farming contracts.
- [x] Tears of Guthix.
- [x] Kingdom.
- [x] Kingdom approval.
- [x] Battlestaves.
- [x] Dynamite.
- [x] Daily diary rewards.
- [x] Clues.
- [x] Generic future cooldown seam.

An opportunity is not invented when its timer/state has never been observed.

## Clues

- [x] Clue state model.
- [x] Age-based priority signal.
- [x] Preparation surface for equipment, spade, transport, combat supplies, and STASH evidence.
- [x] Current RuneLite clue-step evidence and all 119 STASH identities are
  covered; unopened scrolls and globally unobservable built/filled state fail
  closed without fabricating a step.

## PvM / gear / combat achievements

- [x] PvM readiness snapshot and strategy module.
- [x] Only realistically-ready PvM assessments can surface.
- [x] Typed homes for gear, CAs, minigames, transport, POH, Slayer, Sailing, economy, and storage.
- [x] Shared strategy-signal bus connects observed domain state.
- [x] All current RuneLite boss identities have encounter-specific preparation;
  only locally provable encounters become ready, and CA thresholds without a
  specific ready task stay outside the player queue.

## Economy / resources

- [x] Main GE eligibility is separated from actual affordability.
- [x] Main purchase decision requires verified cash state and verified price/time inputs.
- [x] Buy-vs-self-source comparison.
- [x] Insufficient cash routes to money-making/resource review instead of automatically selling gear.
- [x] Player and built-in protected-item boundary for sale suggestions.
- [x] Iron-like accounts self-source by default.
- [x] GIM Group Storage only when enabled and observed.
- [x] UIM normal-bank routing disabled.
- [x] RuneLite prices, observed cash, and 60 account-aware source families feed
  affordability and sourcing; changing-market or unproven money methods remain
  outside the queue instead of using invented GP/hour.

## Knowledge maintenance / OSRS Wiki

- [x] Explicit game-knowledge domain manifest.
- [x] Coverage state distinguishes Scaffolded / Partial / Verified.
- [x] Provenance metadata supports RuneLite API, OSRS Wiki, manual verification, and player observation.
- [x] Imported Wiki records are staged until explicitly verified for planning.
- [x] Offline Wiki/RuneLite refresh tooling, generated quest enrichment, census
  drift checks, and reviewable source documentation.
- [x] Content drift fails local/CI checks; automatic PR creation is deliberately
  outside the local planner and is not required for gameplay correctness.

## Compass Plus readiness

- [x] Core-vs-hosted feature enum.
- [x] Free local entitlement snapshot always includes the complete local core.
- [x] Versioned future sync envelope/categories.
- [x] Disabled remote gateway with no endpoint, HTTP, telemetry, billing, or auth.
- [x] Remote calls cannot transmit in the current build.
- [x] Architecture preserves offline local planning if hosted services are later unavailable.
- [ ] Future auth/billing/server implementation, intentionally not built yet.

## Safety

- [x] No gameplay clicks, movement, banking, combat, or interaction automation.
- [x] Unknown state stays unknown.
- [x] UIM death storage/deathpile are risk-sensitive.
- [x] Protected item framework.
- [x] Wilderness methods default off.
- [x] Plus network behavior defaults completely off.
