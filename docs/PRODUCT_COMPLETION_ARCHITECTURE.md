# Product-completion architecture

This document describes the implemented planning seams and their honest
boundaries. A class or enum is not treated as proof that a gameplay domain is
complete. Player-facing recommendations still have to pass membership, build,
requirement, actionability, and presentation gates.

## Recommendation decision flow

1. `MethodStrategyKnowledgeCatalog` generates only sourced shared or
   account-applicable method profiles for the observed mode. `RecommendationEngine`
   turns those legal profiles into skill candidates and targets the
   nearest proven breakpoint rather than an arbitrary round number.
2. Candidate providers add quests, upgrades, Slayer workflow actions, PvM,
   diaries, clues, infrastructure, resources, minigames, and other activities
   to the same pool.
3. A provider may supersede a generic candidate only when it emits a usable
   typed replacement. For example, live Slayer workflow owns `skill:slayer`
   only after it can produce an assignment action.
4. `GoalDependencyProvenanceService` attaches a goal relationship only when the
   goal graph proves the selected-goal-to-action path.
5. Infrastructure, quest-path, method travel, deterministic resource, and
   producer-owned strategic properties are attached as
   `RecommendationStrategicValue`.
6. `FinalExecutionPlanValidator` validates typed execution behavior after the
   method, resource plan, inventory fit, location, travel route, and guidance
   are resolved. `CandidateSafetyPolicy` and `RecommendationActionabilityPolicy` reject
   illegal, blocked, hard-unknown, or non-executable work.
7. `RecommendationIntelligenceService` compares the remaining candidates. It
   consumes typed properties; player-facing prose and method identity do not
   create account, risk, resource, or strategy value.
8. The winning proven path becomes a `StrategicPlan`. Minor score/inventory
   movement preserves it; account, goal, membership, legality, completion, and
   executable-method changes may advance or rebuild it.

Presentability is a gate, not a score bonus. A recommendation does not become
strategically good merely because its evidence or copy is easy to render.

Source specificity is also not a score bonus. An account-specific profile is
valuable only through its real properties—inventory fit, resource route,
setup, risk, or account utility—and can lose to a better-fitting shared method.

## Strategy knowledge and provenance

`StrategySourceRegistry` gives stable IDs to reviewed Wiki/RuneLite source
families, including subject, URL, reviewed date, revision label, and license.
`MethodStrategyProfile` links a method to applicable account modes, evidence
tier, banking behavior, inventory footprint, account-value fit, a concise
causal player reason, and source IDs. The Wiki supplies strategic knowledge;
RuneLite remains authoritative for the character's live state; Compass still
compares and selects the action.

Wiki text is not copied into runtime records. Maintainers retain facts and
short paraphrased strategic properties under clean provenance. OSRS Wiki text
is identified as CC BY-NC-SA 3.0; media has separate per-file licensing and is
not imported by this workflow. See `STRATEGIC_DATA_SOURCES.md`.

## Account strategic priorities

`AccountStrategicPriorityService` derives a typed profile for Main, Ironman,
GIM variants, UIM, HCIM, and HCGIM. It represents inventory pressure, legal
bank/GE access, self-sourcing, shared-resource evidence, POH/storage/transport
value, setup sensitivity, risk, consumable replacement, duplicate grind,
storable equipment, and GP liquidity.

The profile does not name winners. Candidate properties consume relevant
dimensions:

- Main can value a verified tradeable substitute, but affordability and live
  price remain separate evidence.
- Iron modes pay self-source and consumable replacement cost.
- fresh, enabled Group Storage can satisfy an exact item requirement and avoid
  duplicate acquisition; stale/disabled/unknown storage contributes nothing;
- UIM values legal storage, reusable transport, setup preservation, and
  inventory pressure, while conventional-bank-dependent actions remain
  illegal;
- Hardcore risk changes selection through typed risk burden and final safety
  policy, not an appended warning.

RuneLite does not expose reliable teammate levels, roles, POH furniture, or
specialisations. `GimGroupStrategyService` therefore reports teammate
infrastructure as unknown and never invents it from Group Storage.

For training methods the difference is now earlier: the selector obtains
verified account-specific plus verified shared profiles before scoring. The
F2P UIM Smithing level-1 route, for example, mines copper and tin in east
Lumbridge, processes them at the Lumbridge furnace/anvil, and never instantiates
the Main/standard-Iron Varrock West bank loop.

## UIM inventory and final execution

`MethodInventoryFootprint` records a method-relative minimum practical free
slot count, persistent and temporary item footprint, qualitative input/output
flow, and whether the method tears down the current setup. A full inventory can
therefore remove a method before ranking while a genuinely zero-slot activity
remains viable. The engine does not translate this into generic "make space",
bank, or drop advice.

`MethodBankingBehavior` distinguishes no-bank/local processing, optional bank
use, conventional bank loops, and UIM-compatible noted-item services. Both the
selected strategy profile and the final `RecommendationGuidance` carry this
typed behavior. The final validator converts any conventional dependency into
the safety flag consumed by `CandidateSafetyPolicy`.

`ActivityStrategyKnowledgeCatalog` extends plan-relative footprint checks to
questing, current Slayer assignments, clues, minigames, gear upgrades, PvM,
and infrastructure before the final queue. This is deliberately not another
recommendation engine: existing providers still use live evidence and create
the candidate, sourced strategy constrains practical fit, and the common
intelligence layer still makes the choice.

UIM storage systems remain distinct. The generic legacy `DEATH_STORAGE`
observation cannot authorize a plan. Hespori, Zulrah, and Volcanic Mine Item
Retrieval Services have exact capability identities; looting bag and
deathpiles retain separate behavior. Any dangerous-storage guidance requires a
verified `UimStorageDecision`, a prominent HIGH RISK disclosure, and explicit
acknowledgement. Missing exact access, compatibility, capacity, or disclosure
fails final execution validation.

## Infrastructure and unlocks

`InfrastructureMilestoneCatalog` contains reviewed milestones for POH access,
the Costume room and oak armour case, a configured Portal Chamber, Superior
Garden and restoration pool, Portal Nexus, basic jewellery box, POH fairy
ring/spirit tree, and the wider fairy-ring/spirit-tree networks. Definitions
support multiple skill and quest requirements and carry typed benefits plus
observable completion evidence.

`LivePohStateReader` scans the complete scene only while RuneLite proves that
the current character is in their own house with building mode enabled. It
records both present and absent tracked furniture. Public and teammate houses
produce no personal evidence, and the observation is retained only after the
stable account identity matches. `InfrastructureCandidateProvider` turns that
evidence into either one exact own-house verification step or an explicit
build-preparation action; F2P never receives these candidates.

`InfrastructureUnlockValueService` distinguishes complete, eligible,
evidence-needed, and not-applicable states. Value attaches only to the actual
skill or quest action that reaches the milestone. A Construction level never
proves furniture exists, and a generic network unlock never proves a specific
destination node.

`SkillBreakpointService` prefers proven goal requirements, reviewed
infrastructure/ability unlocks, and actual selected-goal endpoints before a
one-level safe fallback.

To add an infrastructure unlock:

1. verify mechanics and add the source to `STRATEGIC_DATA_SOURCES.md`;
2. add one typed `InfrastructureMilestoneDefinition` with observable evidence;
3. extend the own-house object classification when the capability is visible
   in RuneLite's scene, without weakening the building-mode ownership gate;
4. add semantic tests for mode value, substitutes, unknown state, and the
   exact prerequisite action;
5. do not expose a direct candidate unless its build/use action is executable.

## Travel and method locations

`MethodLocationCatalog` stores named alternatives for reviewed training
methods. `TravelRouteEvidenceCatalog` describes exact evidence keys and
`TravelRouteEvidenceService` proves them from observed transport, quest, and
usable-item state. `TravelAwareMethodValueService` rejects membership and
Wilderness-incompatible locations, compares travel burden, and can replace the
rendered WHERE with the selected named location.

The model is intentionally not a world pathfinder. A generic fairy-ring or
spirit-tree flag cannot prove an endpoint. Unknown travel earns no shortcut
credit.

To add a method location, extend the location and exact-route catalogs together
and test the ordinary route, exact unlock route, unknown route, membership,
and Wilderness boundary.

## Resource pipelines

`MethodResourceValueService` uses RuneLite skill-calculator action data,
deterministic execution profiles, XP modifiers, and exact target XP to derive
inputs. The live service receives RuneLite's `ItemManager`; default test
catalogs retain unknown membership and fail closed.

`SustainableResourceValueService` separates current observed readiness from
replacement burden. Reviewed input families include relevant runes/essence,
bars, planks, logs, raw food, production ingredients, herbs, seeds, and other
deterministic method inputs. Unknown inputs receive no guessed tradeability or
scarcity. Unlike resources are evaluated separately and never summed as one
quantity.

Main, Iron, GIM, and UIM replacement economics differ. Fresh enabled Group
Storage can add exact shared-resource evidence; it cannot prove future teammate
production. Variable consumption, burn rates, RNG yield, and future Slayer
workload are not presented as exact.

## Contextual gear

The existing gear progression and acquisition catalogs remain encounter-aware:
there is no universal BIS ladder. `ContextualGearValueService` can compare an
exact target using supplied marginal benefit, replacement horizon, acquisition
burden, goal relevance, and UIM storage disposition. Missing evidence returns
`NEEDS_EVIDENCE` instead of a winner.

Executable exact upgrades continue to come from audited upgrade providers.
Generic tier comparisons remain secondary CHECK_NEEDED work until ownership,
encounter benefit, live price/supplies, and acquisition route are proven. The
typed value evaluator is deliberately not used to turn a broad gear tier into
an authoritative purchase.

To add a gear upgrade, add an exact acquisition route and provenance, preserve
Main/Iron/UIM/Hardcore/build legality, provide an executable first dependency,
and test replacement horizon and context. Do not label an item BIS without an
encounter model.

## Slayer strategist

`LiveSlayerStateReader` models UNKNOWN, NO_TASK, CHOICE_PENDING, and ASSIGNED from stable
RuneLite task/count/points/streak state. It also decodes the assigning-master
varbit, master-specific block-slot varbits, and reviewed reward/extension
varbits. Mortimer's live task/modifier choices, two block slots, introduction
state, and distinct costs are decoded from pinned RuneLite DB/varbit data; his
unexposed separate streak remains unknown rather than borrowing the ordinary
streak. A zero unlock varbit is therefore known locked, not missing evidence.
`SlayerStrategist` can produce DO,
SKIP, BLOCK, PREP_FIRST, or ALTERNATIVE, then `SlayerCandidateProvider` exposes
that one workflow action to the shared queue.

The reviewed local catalog gives every current mechanics family conservative
ordinal strategic economics plus master requirements, verified task weights,
risk, setup, required items, resource value, and selected alternatives. Direct
boss tasks cannot inherit a generic Slayer loadout and require verified
encounter readiness. Point decisions respect
cancellation cost, per-master block cost, observed free slots, and streak
bonuses. Unknown master or block state still fails closed.

Between assignments, `SlayerRewardAdvisor` can lead with a verified locked,
affordable permanent reward when its account/goal value exceeds immediately
requesting another task. It currently reasons about Bigger and Badder, Slayer
helmet crafting, broad ammunition, Slayer-ring crafting, Task Storage, boss
tasks, and TzHaar tasks while retaining a 30-point cancellation reserve.
Reviewed extensions for dust devils, nechryaels, abyssal demons, bloodvelds,
gargoyles, and cave kraken can also lead when account mode, selected goal,
Slayer level, and session length make the longer future assignments worthwhile;
they are never purchased merely because the points exist.

To add a Slayer task:

1. verify current master pools/weights and mechanics against the Wiki;
2. add a `SlayerTaskStrategicProfile` with task identity aliases, setup, risk,
   required items, and any legal alternative;
3. add DO/SKIP/PREP and point/streak tests for affected modes;
4. add exact TASK/WHERE/STYLE/BRING/DO guidance;
5. never infer an observed task, master, block slot, or point balance.

Unknown future task economics remain PREP_FIRST instead of receiving a guessed
keep, skip, or block score.

## Quest ordering, goal paths, and plans

`QuestPathPlanningService` orders incomplete quests already proven on the
selected goal path, counts shared downstream dependencies, and attaches only
relevant guaranteed quest-XP value. Required goal quests remain eligible when
the player sets Optional quests to Low; the preference continues to penalise
unrelated elective quest detours.

`GoalDependencyProvenance` is the authority for prerequisite/direct language.
A score, skill level, or generic goal affinity is never sufficient. The same
rule is applied to Barrows gloves, Fire cape, Quest cape, Prifddinas, Bowfa,
Infernal cape, Max cape, and other typed goals.

`StrategicPlanService` builds NOW/NEXT/TARGET steps from the winning proven
path. `PlanContinuityService` preserves unfinished work across minor refreshes
and advances/rebuilds on meaningful state changes. It does not create a second
dependency graph or fake goal percentage.

To add a quest, update the quest knowledge/dependency catalog, requirements,
rewards/unlocks, source registry, and path tests together. Guaranteed XP must
be exact and relevant to a mandatory or strongly preferred upcoming path.

## Setup reuse, detours, and worth-doing-now

`SetupReuseService` measures explicit equipment, inventory, region, spellbook,
and setup-time properties. `SmartDetourService` converts verified travel saved,
setup saved, duration, account/goal value, risk, and interruption cost into the
common DO_NOW/PREP_FIRST/WAIT/SKIP assessment.

These evaluators do not inspect recommendation IDs. Producers may attach their
results only when the corresponding live evidence is available. The current
strategy bundle has inventory/equipment evidence, but not a general trusted
region/spellbook/current-activity profile for every candidate; Compass does not
pretend broad automatic batching is live where that evidence is absent.

## Progress analytics and persistence

`ProgressAnalyticsService` consumes `StatChanged` events rather than polling.
It records per-skill/session XP, levels, active duration, bounded five-minute
buckets, recent measured rate, target XP remaining, ETA, and meaningful
milestones. A measured rate requires multiple samples over sufficient elapsed
time; idle gaps are excluded and insufficient evidence shows calculating
rather than a fabricated rate. Logout/loading explicitly closes the current
active/rate segment even when the gap is short. Cross-skill events must be
globally time-ordered, and an absolute-XP rebase clears all volatile session
evidence instead of retaining another character's chart or milestones.

`ProgressHistory` retains at most 30 sessions, 100 milestones, and 288 buckets.
`ProgressHistoryCodec` tolerates missing/corrupt state and
`AccountProgressHistoryStore` namespaces data by RuneLite character profile.
History normalization sorts sessions and buckets, merges same-time buckets,
and deduplicates stable milestone/session identities. A replaceable active
session preview is saved at most once per minute, limiting crash or late
profile-switch loss without writing RuneLite configuration on every XP drop.
Schema 2 retains typed account milestones inside each session recap and still
loads schema-1 XP history safely.
Recommendation feedback/completion history remains separately bounded at 200
events. There is no network telemetry.

`AccountProgressMilestoneDetector` recognises successive observed quest,
transport, storage, POH, diary-tier, and Slayer-reward changes. It rebaselines
on account switch, uses stable event identities, and never turns the first
snapshot into a list of achievements.

The sidebar uses a calm DO NEXT / Progress switch. Progress shows session XP,
levels, active time, current target/rate/ETA, NOW/NEXT/TARGET, a lightweight
bounded chart, recent milestones, and the last session recap. It does not show
fake weighted goal percentages. The recap includes account progress even when
the session's durable unlock mattered more than its XP total.

## UI and performance

The sidebar remains primary. Recommendation Details and Method Guidance are
mutually exclusive in-game overlays, not simultaneous copies. Rendering uses
wrapped Swing components and the existing text scaling/theme conventions.

XP events update the small progress model and repaint the Progress surface;
they do not run the full strategist on every drop. Catalogs are local,
runtime Wiki requests are forbidden, histories are bounded, and expensive
planning stays out of per-tick polling. Inventory/gear/bank and varbit bursts
retain one pending rerank and are coalesced to a two-second boundary; the newest
state is never discarded. Farming varbit reads and personal-POH furniture scans
use the same bound. POH scans remain gated behind login, varbit, and scene-object
events plus own-house build mode, so a full scene walk is never performed
continuously merely because the player is standing in their house. Immutable
Farming catalogs/evaluators are reused across evaluations instead of allocated
inside every candidate pass.

Login and RuneScape-profile transitions only mark the live snapshot dirty. The
first game tick performs one coalesced full observation/evaluation instead of
evaluating immediately and repeating the same work for the inventory,
equipment, varbit, quest, Slayer, and scene-event burst that follows login.

## Verification workflow

For every strategic data change:

1. update `STRATEGIC_DATA_SOURCES.md` with the verified mechanic and date;
2. change typed data rather than adding an identity-based winner bonus;
3. add semantic/invariant tests;
4. run `./gradlew clean test` and `./scripts/check-content-census.sh`;
5. run `git diff --check` and inspect player-facing copy.

The exact current content limits belong in the final engineering report and
source registry. Documentation must not turn an internal evaluator into a
player-facing capability claim.
