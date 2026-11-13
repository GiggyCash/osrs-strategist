# Source-size intervention

Audit date: 2026-09-30. Baseline: `d9f3e326f1b20bfa4a02180e5b74c549df68a75c`.
Coordination: issue #9, standing engineering policy and architecture question
[#5917062157](https://github.com/GiggyCash/osrs-strategist/issues/9#issuecomment-5917062157).

## Measurement and boundary

Run `python3 scripts/audit-source-size.py --base <previous-checkpoint>`.
The script measures all `src/main/java` sources, including comments and whitespace,
with estimated tokens = ceil(Unicode characters / 4). This is a repeatable size
proxy, not an actual model tokenizer or a Plugin Hub acceptance rule. Do not
reduce the measurement by stripping comments, minifying code, or excluding
production classes. Tests and resources are outside the Java measurement.

Baseline: 70 files, 28,615 lines, 1,053,152 characters, 263,288 estimated tokens.
This exceeds the reviewer's 150,000 intervention threshold before new expansion.
The latest UI checkpoint added 64 production lines / 746 estimated tokens.

| File | Lines | Characters | Observed responsibility |
| --- | ---: | ---: | --- |
| PlanningEngines.java | 3,383 | 137,288 | Resources, access, requirements, ranking, legacy recipes |
| StrategyServices.java | 2,506 | 96,844 | Multiple strategy services; further semantic audit needed |
| CandidateProviders.java | 2,173 | 85,980 | Cross-system candidates and their final requirements |
| GuidanceServices.java | 2,146 | 78,157 | Adaptive, combat, method, universal and variable guidance |
| DomainTypes.java | 2,496 | 71,287 | Shared domain values; size alone is not evidence of duplication |

File splitting may improve ownership but does not reduce total source size.
The first intervention must remove a duplicated responsibility or replace
embedded knowledge with a validated existing data contract.

## Findings at the baseline that constrain consolidation

1. `UniversalActionRecipeResolver` in PlanningEngines has both a bundled
   `action-recipes.json` table and Java name-based ingredient inference for
   Cooking, Crafting, Fletching, Smithing and other skills. Substring matching
   is treated as an exact recipe in several paths. Moving these strings to JSON
   alone would preserve the inference problem. Migrate a reviewed recipe family
   to explicit identity, quantities and provenance, then delete its fallback.
   Do not call old inferred records verified merely because they were imported.
2. `MethodIntelligenceCatalog` already supplies reviewed ingredients, reusable
   tools, output flow and batch limits. `MethodReadinessService` evaluates that
   contract. Extend this path where it fits; do not create another per-skill
   readiness engine alongside the legacy resolver.
3. Ingredient merging currently has different contracts:
   - AccountResourcePlanner merges by normalized name, skips invalid rows and
     saturates sums. It supports legacy name-only ingredients and resource prose.
   - MethodPreparationService merges by positive item ID and rejects invalid
     identities, conflicting names and unrepresentable totals.
   - UniversalActionRecipeResolver.profileInputs merges by ID or name and adds
     quantities as `int`. Overflow can reach MethodInput's nonnegative clamp,
     turning a positive requirement into zero. Its callers in
     AdaptiveActionSelector and AdaptiveMilestoneGuidanceService currently
     assume a non-null result.
   A common merger must preserve explicit identity and failure semantics; simply
   routing strict preparation through the legacy merger would weaken safety.
4. The generic preparation/readiness classes are small and share affordability
   through AccountResourcePlanner.purchase. Do not rewrite this validated
   boundary to reduce the size of unrelated legacy files.
5. Player-facing copy already lives in `player-text.json`; further bulk string
   extraction from the same code is unlikely to solve the behavioral duplication.

## First intervention delivered

`MethodInput.mergeExact` now owns strict aggregation for typed preparation and
legacy execution-profile inputs. Preparation still requires positive IDs;
profiles may use names. Both reject invalid rows, same-ID name conflicts and
unrepresentable totals. Profile quantity multiplication rejects non-finite or
oversized values before integer conversion. Selection skips an unresolved
profile recipe; guidance returns no plan for it. The name-only resource prose
planner remains unchanged because its semantics differ.

Regression scenarios cover duplicate overflow, individual quantity overflow,
identity conflicts, legacy name support and adaptive selection across all seven
known account modes. This closes the demonstrated safety defect and removes a
duplicate strict merger; it does not claim to solve the total size overage.
Production impact: +5 lines / +97 estimated tokens, no new Java files. New total:
70 files / 28,620 lines / 263,385 estimated tokens. The validation and caller
failure handling account for the small net increase.

## Ordered follow-up work

The completed first step was to close the profile-input overflow path with an explicit unresolved result
and account-level selection tests. Further consolidation of the legacy resource
prose planner requires an explicit policy for invalid rows and saturation; do
not silently broaden exact preparation to use its permissive semantics. Preserve
first-seen ordering and merge consumption before subtracting stock.

Next, migrate one legacy processing family to the reviewed recipe contract.
Research only that family's current Wiki mechanics and RuneLite identities;
record source revisions and contextual assumptions in production data. Compare
old/new coverage explicitly: unsupported actions must become unresolved, not
invented recipes. Remove the replaced family inference and prove capacity,
preparation, account-mode safety and adaptive selection through existing tests
plus targeted scenarios. Report net Java and data growth separately.

Then reassess remaining large classes by responsibility. Candidate provider
membership/build checks are not redundant just because they resemble another
provider's checks; only consolidate after tracing their distinct evidence and
failure paths. Do not mass-move classes or widen readiness while waiting for
reviewer feedback.

## Checkpoint gates

For implementation changes: focused tests during iteration, full clean test/JAR
verification at a coherent checkpoint, catalog validation, privacy scan, manual
diff review and source-size comparison. Record the three existing quest failures
separately; they are not permission to introduce new failures. Architecture-only
reports can reuse the unchanged source checkpoint's test evidence if clearly
identified as such. Reverify actual current Plugin Hub constraints before any
release/submission; this report does not authorize publication.

## Second intervention: remove Cooking name inference

Cooking's raw-name fabrication and special-case wine inference were replaced by
30 exact-match reviewed ingredient rows (29 ordinary fish plus wine). One generic
validated dataset extends the existing resolver; no per-food Java branches were
added. Runtime validation and membership gating add a net 20 Java lines / 393
estimated tokens while normalized data adds 13,176 bytes. This is a safety and
knowledge-boundary improvement, not a claim of total source-size reduction.

The next consolidation should reconcile overlapping legacy action inputs and
method-intelligence recipes without losing their distinct readiness/output
contracts. Other skill fallbacks remain unreviewed; do not mass-import them as
trusted records. Coverage and intentional unresolved foods are documented in
METHOD_INTELLIGENCE.md.


## Third intervention: one source for shared ingredients

Extracted reviewed recipe loading/validation from the legacy resolver into
`ReviewedActionRecipeCatalog`, a shared knowledge component. Ten method recipes
now reference its ingredients instead of repeating item IDs, names, quantities
and stackability. Referenced membership is enforced at the method boundary;
method capacity, output, equipment and access contracts stay separate. Inline
Fletching recipes are unchanged.

The extraction removes 31 lines from PlanningEngines but adds a net 61 production
lines / 659 estimated tokens across the project (71 files, 28,701 lines, 264,437
estimated tokens). This is a consolidation of fact ownership, not a net source
reduction. The single new catalog is a reusable domain boundary, not a subsystem
per skill. No additional training method is enabled by this change. Future
reviewed ingredient families should reuse this boundary; do not repeat its
validator or create another recipe catalog.


## Fletching proof of the shared contract

The existing regular/oak arrow-shaft methods now reference reviewed ingredients.
A generic calculator-units-per-batch field preserves their different counting
semantics; the oak record remains method-only when calculator units are unknown.
Removed one hard-coded arrow-shaft inference branch and the now-unused integer
ceiling helper (six Java lines). Generic conversion adds four lines: net -2
production lines, +73 characters / +19 estimated tokens, no new Java files.
Cooking/Fletching capacity, membership, preparation and selection scenarios
remain the behavioral gates. The next embedded-knowledge seam is the remaining
Crafting/Fletching inference in UniversalActionRecipeResolver: suffix-based wood,
metal and composite input guesses must be replaced only with reviewed facts,
not transcribed into trusted data indiscriminately.

## Elemental battlestaff inference removed

Replaced the existing four-element Crafting assembly family with four reviewed
rows in the shared catalog. Deleted the six-line substring/element inference
branch in UniversalActionRecipeResolver; no replacement production Java was
needed. Net production delta: -6 lines / -284 characters / -71 estimated tokens.
This is semantic knowledge removal rather than a file move. Targeted scenarios
retain the four canonical recipes and reject invented higher-XP staff candidates
for Main/Iron/UIM; membership is explicit in the reviewed records. Remaining
Crafting gem, jewellery, leather, glass and bird-house inference still requires
its own evidence-backed migration. No broad method expansion is implied.

## Glassblowing inference removed

Seven existing calculator actions now reuse the shared reviewed schema. Deleted
the four-line glass-product substring rule, with no replacement production Java.
Net delta: -4 lines / -246 characters / -62 estimated tokens. Review distinguished
output-item membership from crafting membership and the calculator Fishbowl
label from the empty bowl produced. Tests preserve canonical input quantities
and reject filled/unknown products and members crafting on F2P accounts. No
schema, validator, per-method Java or broad method coverage was added.

## Ordinary dart inference removed

Eight reviewed rows replace the legacy two-line dart-suffix/first-word ingredient
guess. No replacement Java or schema was added: -2 production lines / -143
characters / -36 estimated tokens. Exact per-item quantities preserve the
existing ordinary family; unsupported atlatl and invented variants fail closed.
The atlatl input/XP discrepancy discovered during research is documented in
METHOD_INTELLIGENCE.md. Other projectile inference and legacy execution-profile
input transformations remain separate follow-up work, not certified by this
migration.

## Alternate profile path consolidated

Four existing execution profiles now consume reviewed recipe inputs instead of
repeating raw-food, wine and dart ingredient rules. The dart-tip inference enum
value and five-line switch branch are deleted. A generic reviewed-input dispatch
plus membership plumbing replaces them: net +4 Java lines / +316 characters /
+79 estimated tokens, with no new production file. This near-flat adapter closes
a separate inference path rather than adding another recipe validator. Regression
coverage compares both input paths and rejects unknown names that still match
profile terms. Remaining legacy profile transformations need separate migration.

## Both ordinary bolt inference paths removed

Nine reviewed records replace the universal metal-bolt inference and its
allowlist/exclusion helper. The existing execution profile uses the same source;
its UNFINISHED_BOLT enum and suffix-appending switch branch are removed too.
Net production change is -16 Java lines / -771 characters / -192 estimated
tokens, with no added schema, validator or production file. Exact IDs also remove
the fabricated spacing in the old adamant unfinished-item name. The RuneLite
dragon-bolt icon/label issue is documented separately rather than hidden behind
a speculative alias. No broad method expansion is implied.

## Final raw-name transformation removed

RAW_ACTION_ITEM and its prefix/suffix logic are deleted. The unresolved karambwan
profile now uses the reviewed-input boundary and cannot manufacture an ingredient
list while its cooking lesson and execution setup remain unobserved. Net change:
-5 production Java lines / -198 characters / -50 estimated tokens; no new
production file, schema, validator or recipe record. This deliberately preserves
the unresolved route instead of expanding support without an access contract.

## Checked recipe scaling shared across legacy and reviewed paths

Removed the saturating multiply helper and the independent reviewed-recipe
scaling loop. Legacy catalog rows, legacy inferred recipes and reviewed recipes
now use one checked quantity boundary plus the existing exact-input merger.
Overflow or incomplete identities discard the whole recipe rather than retaining
partial inputs or claiming an exact Integer.MAX_VALUE requirement. Duplicate
materials merge before guidance receives the recipe. Explicit no-consumable
activities keep their separate representation.

This correctness consolidation adds a net 7 production Java lines / 419
characters / 105 estimated tokens, with no new production file or catalog data.
The prior saturation regression now asserts rejection and a valid boundary;
additional cases cover catalog overflow, duplicate totals and invalid identities.
Existing recipe provenance and access limitations are unchanged.

## Both gem-cutting name transformations removed

Six reviewed deterministic records replace the universal gem-name inference and
the UNCUT_GEM profile transformation. The existing crafting_gems profile uses the
shared catalog; variable soft gems and the conflicting zenyte XP remain unresolved.
The gemName helper still serves legacy jewelry and is not claimed as removed.
Net production change: -7 Java lines / -358 characters / -90 estimated tokens,
without new production files, schemas or validators. No broad method expansion.

## Stage transitions share recipe and access checks

Adaptive stage resolution formerly used only name matching and future levels,
so an action rejected by execution selection could still shorten the current
stage. It now reuses the same profile-input boundary and membership policy,
with live membership supplied by RecommendationEngine. The legacy overload
fails closed to UNKNOWN. Both paths reject non-finite or non-positive XP.
Curated method level-band boundaries remain independent and preserved.

Net production change: +9 Java lines / +526 characters / +132 estimated tokens;
no added production file or data. This consolidates eligibility policy rather
than adding an action-specific exception. It does not certify future resources,
access unlocks or setup; it only prevents unsupported actions from creating a
spurious recipe transition.

## Adaptive resource coverage uses reviewed identities

AdaptiveActionSelector now uses the existing ItemIndex ID lookup when a recipe
supplies a positive item ID. Name-only legacy inputs retain their existing lookup.
This closes the gap between reviewed ingredient identity and selection scoring:
a display label cannot substitute for a different reviewed item, and a changed
label cannot hide stock with the correct ID. Existing mode-safe containers,
observation gating and group-storage opt-in remain in ItemIndex.

Net production change: 0 lines / +48 characters / +12 estimated tokens. No new
production file, catalog data or policy service. Regression scenarios cover all
six iron modes, UIM bank exclusion and enabled observed group storage; existing
name-only profile scenarios continue to cover the legacy path.

## Ownership queries respect explicit observation state

ItemIndex now uses ItemsState.isObserved for ordinary inventory, equipment and
bank evidence, rather than treating a non-null DTO as an observation. One shared
predicate replaces the parallel null checks across ownership completeness,
quantity queries and ranked-item queries. Unknown container payloads cannot
satisfy supplies; independently observed carried stock remains countable even
when the bank is unknown. A zero lower bound is not proof of an empty container.

Net production change: +5 Java lines / +131 characters / +33 estimated tokens.
No new production file or catalog data. Regression coverage distinguishes known
empty and explicitly unknown snapshots across all account modes, rejects
unobserved payloads, and preserves UIM's distinct resource/ownership boundaries.

## Container quantities share checked accumulation

ItemsState's single-ID, multi-ID and name-predicate totals now use one positive
stack accumulator. Duplicate requested IDs match a stack once; invalid negative
stacks cannot subtract observed stock. Totals stop at Integer.MAX_VALUE as an
ownership lower bound instead of wrapping into a shortage. This differs from
recipe requirements: an unrepresentable required quantity still fails closed.
Observation checks remain at the existing caller boundaries.

Net production change: +3 Java lines / +110 characters / +27 estimated tokens,
with no new production file or data. Three independent accumulation loops were
replaced. Regression coverage includes duplicate IDs, overflow through each
query surface, invalid stacks and the actual preparation consumer.

## Preparation shares ownership policy

MethodPreparationService now delegates purchase observation to ItemIndex's
primary ownership boundary and acquisition observation to its usable ownership
boundary. The duplicate inventory/equipment/bank/UIM/group completeness helper
and local observation helper are removed. The explicit UNKNOWN-mode acquisition
rejection remains; known carried or retrieved stock still resolves independently.

Net production change: -13 lines / -504 characters / -126 estimated tokens, with
no new production files or data. Added cross-mode acquisition scenarios verify
that unknown inventory or equipment cannot justify a detour, while known empty
observations can. Existing purchase and group-storage tests cover those paths.

## Explicit recipe scope replaces the broad review flag

The seven migrated execution profiles now declare exact recipe keys; the broad
reviewedInputs flag is removed. A shared catalog row is knowledge, not permission
to use that row in every method matching a substring. Validation and resolution
reuse the existing catalog and profile boundary, without a new service or file.

Net production change: +15 Java lines / +754 characters / +189 estimated tokens.
This is an intentional correctness boundary, not a source reduction claim. No
recipe facts were added. Cross-product coverage checks all seven profiles against
all 66 reviewed rows; a misleading ID/category cannot promote another activity.

## Paired bow inference paths removed

Reviewed standard-bow records replace both universal cutting/stringing heuristics
and the LOG_FOR_BOW profile branch/enum. The existing wood helper remains for
shield/stock and bird-house paths; it is not claimed as removed. Crossbows no
longer inherit a log recipe from an overbroad suffix. Exact profile references
separate cutting from stringing, and the conflicting Willow stringing XP stays
unresolved rather than receiving guessed support.

Net production change: -8 Java lines / -382 characters / -96 estimated tokens;
no new production file, schema or validator. Twenty-three evidence-backed records
replace existing inferred coverage, with no new training methods.

## Shared Smithing profile bar inference removed

Removed BAR_FOR_SMITHED_ITEM, its metal-name construction switch branch, and the
platebody-versus-one-bar default. All three consumers now reference the existing
reviewed recipe catalog. This corrects undercounted multi-bar bronze items and
prevents broad bronze search terms from admitting unrelated activities. No new
training method or schema was added. The universal smithingBarsFor fallback is
still present and is not counted as removed.

Net production change: -11 Java lines / -580 characters / -145 estimated tokens.
Twenty-eight verified recipe rows replace the profile heuristic; shared universal
lookup also reuses those rows. Source XP/action units were cross-checked against
current RuneLite, including the ten-tip batch represented by one Smithing action.

## Universal Smithing inference retired

Removed the remaining Smithing metal-name recipe constructor and smithingBarsFor
quantity classifier. Reviewed anvil records are now the sole anvil recipe source;
unsupported names fail closed. Existing exact furnace records remain unchanged.
Production reduction: 24 lines / 1,110 characters / 277 estimated tokens, with no
new production file. One reviewed iron 2h sword row preserves an existing F2P
route (Wiki revision 15320140, three iron bars, XP agrees with RuneLite).
Regression coverage exercises invented and unreviewed names across membership
states, plus retained reviewed quantities.

## Furnace recipe consolidation

Moved seven deterministic standard-furnace input records from the legacy recipe
list into the shared reviewed catalog and removed the unconditional iron ore
claim. No Java production growth or new schema. This checkpoint consolidates
existing knowledge and fixes uncertain supply arithmetic rather than adding a
training route. Regression tests protect iron uncertainty and retained furnace
input IDs/quantities across membership states.

## Shared material identity lookup

Consolidated ID-first MethodInput lookup in ItemIndex for adaptive selection,
universal ranking and account supply planning (including restricted-storage
reporting). Replaced the supply planner's name-only grouping key with identity
keys. No game-specific Java or catalog expansion. The small shared helper growth
fixes disagreement between selection and supply guidance; it is not represented
as a source reduction. Universal coverage totals now use long arithmetic.

## Checked supply merge consolidation

Removed AccountResourcePlanner's private merge, saturating addition and
MutableNeed carrier. It now delegates to the existing checked MethodInput merge;
both guidance consumers reject an unresolved supply plan. Existing strict
requirement-identity checks remain unchanged. No new schema, model or production file.
Net production change: -53 lines / -1,610 characters / -402 estimated tokens.

## Standard arrow identity correction

Replaced the six standard metal-arrow name guesses with exact reviewed recipe
rows pinned to OSRS Wiki revisions 15397319, 15397320, 15397322, 15397324,
15397326 and 15397327. RuneLite item IDs 39–44 retain their player-facing
`arrowtips` names throughout action guidance and the deterministic dependency
graph. Invented arrow names and non-members now fail closed instead of receiving
a fabricated `arrowhead` input. The generic javelin path remains separate and is
not claimed as reviewed by this checkpoint.

Broad arrows remain a distinct CHECK_NEEDED method because the 300-point Broader
Fletching unlock is not directly observed. The profile now uses RuneLite item
11874's player-facing `Broad arrowheads` name, while generic universal resolution
fails closed instead of implying the unlock is known. The mechanic and identity
were checked against Wiki revisions 15397287 and 15373576.

## Javelin tip inference removed

Replaced all eight RuneLite calculator javelin suffix guesses with exact reviewed
rows for bronze through rune, amethyst and dragon javelins. Each row uses one
javelin shaft and one matching player-facing `javelin tips` item per output;
unknown names and non-members fail closed. Source revisions are 15305303,
15305305, 15182590, 15182826, 15182726, 15372028, 15305300 and 15305299.
The obsolete projectile and first-word helpers were removed rather than retained
for a different family.

## Unobservable leather consumables fail closed

Removed the plain, hard-leather and dragonhide substring fallbacks. They counted
leather but omitted thread, whose five-use state is not observed, and could not
distinguish the thread-free costume-needle unlock. The same path also admitted a
members-only coif on free accounts and let invented names inherit recipes.
Universal leather actions now remain unresolved until a reviewed contract can
represent the needle/thread alternative and its live evidence; no existing
curated training method depended on this fallback.

## Unreviewed wood products fail closed

Removed bird-house, shield and crossbow-stock name inference. Bird-house
crafting counted logs while omitting required clockwork state; shield and stock
suffixes admitted invented names without membership or unlock evidence. These
unreviewed universal actions now remain unresolved, while the existing reviewed
bow recipes and exact headless-arrow recipe are unchanged. No curated training
method depended on the removed fallbacks, and the obsolete wood-name helpers
were removed with them.

## Firemaking inference fails closed

Removed the universal `contains("log")` recipe, which treated pyre logs and
invented future names as ordinary one-log actions. The curated F2P route now
matches only RuneLite's five F2P log-burning action IDs and consumes the action's
observed item identity. Pyre logs, sacred oil and other methods remain unresolved
until their additional inputs and access requirements have reviewed contracts.

## RuneCraft action families separated

The universal one-essence fallback now accepts only the six explicit RuneLite
F2P rune action IDs that have concrete altar guidance. Tiaras, cores, combination
runes, Zeah actions and unknown future runes no longer inherit a false essence
recipe. The relaxed blood and soul profiles now select their exact Arceuus action
IDs and count dark essence fragments. The lava profile was removed because pure
essence alone omitted its elemental-rune and binding-resource requirements.

## Universal Prayer inference retired

Removed the universal bones/ashes/head substring recipe. A calculator item alone
does not identify the selected route: ensouled heads also consume reanimation
runes, and Ectofuntus guidance needs its processing and slime requirements. The
existing F2P-bone and gilded-altar execution profiles retain exact action-item
identity; unsupported Prayer routes now remain CHECK_NEEDED instead of receiving
incomplete universal supply guidance.

## Fishing bait contracts corrected

Universal Fishing no longer claims that every catch has no consumed inputs.
Barbarian routes now select a concrete one-feather-per-catch contract, dark
crabs count dark fishing bait, and the standard anglerfish route counts
sandworms. The exact karambwan execution profile was removed because bait can
be lost on failed attempts; its existing variable guidance remains available.
Unsupported Fishing actions fail closed until a reviewed route identifies their
consumables.

## Farming sapling inference scoped to curated routes

Removed the universal `tree` suffix recipe, which invented sapling names for
unsupported actions and was wrong for special trees. The existing curated tree
profiles retain their selected-action mapping, with the fruit-tree profile now
correctly mapping RuneLite's Pineapple plant action to a pineapple sapling.
Unknown Farming actions remain unresolved instead of inheriting a guessed item.

## Magic curse suffix inference removed

Removed the universal `* curse` suffix recipe. The exact Curse action remains
covered by the existing catalog recipe, and the curated F2P method retains its
explicit execution profile. Invented or future action names no longer inherit
Curse's rune quantities merely from their suffix.

## Construction actions use exact RuneLite identities

Replaced seven legacy substring recipes with six exact RuneLite 1.13.6 action
names. The duplicate, nonexistent Oak Dungeon Door recipe was removed, and the
curated oak-door profile now selects RuneLite's actual
`runelite:construction:oak_door` action. Prefixed or invented furniture names no
longer inherit material quantities from a partial match.

## Legacy action recipes checked against RuneLite identities

Added a pinned-enum coverage invariant for every legacy recipe. It exposed a
dead High Alchemy alias and dose-less Herblore matches. The duplicate Magic row
was removed; Herblore now removes only RuneLite's terminal `(3)`/`(4)` output
dose before exact base-name lookup, and antifire uses its full potion name.
Invented prefixes still fail exact matching.

## Execution profiles require a live action

Added a reachability invariant that runs every execution profile through the
same action matcher used by adaptive selection. It found one dead profile:
Ardougne knights referenced names RuneLite does not expose. The profile now uses
the pinned `runelite:thieving:knight` identity; every execution profile matches
at least one maintained action.

## Birdhouse runs count consumed supplies

The Hunter birdhouse profile no longer claims zero consumed inputs. Nine exact
RuneLite tiers now use reviewed recipes pinned to Bird house trapping revision
15364256. Each emptied/reset house consumes its matching log and a concrete
10-potato-seed bait route; hammer, chisel, and the returned clockwork remain
reusable setup rather than inflated per-house costs.

## Headless arrows use reviewed identities

Moved the remaining headless-arrow Java recipe into the shared reviewed catalog.
The exact RuneLite `Headless arrow` action now consumes item 52 (Arrow shaft)
and item 314 (Feather), pinned to OSRS Wiki revision 15399603. The former plural
alias was not a RuneLite action and now fails closed. P2P gating is preserved.

Deleting the dedicated Fletching branch removes 276 production-Java characters
(about 69 char/4 proxy tokens) while adding 560 reviewed-data characters. The
full clean suite is 950/950, the packaged JAR is 1,255,814 bytes, and the current
source audit is 1,077,288 production-Java characters / 269,322 estimated tokens.
The project remains above the intervention threshold; this is one measured
knowledge-boundary reduction, not a claim that the total size issue is resolved.

## Spinning recipes use reviewed identities and membership

Moved the two spinning-wheel Crafting recipes from the legacy catalog into the
shared reviewed catalog. `Ball of wool` now consumes item 1737 (Wool) and stays
F2P, while `Bow string` consumes item 1779 (Flax) and correctly fails closed for
F2P and unknown membership. The exact RuneLite 1.13.6 action identities are
guarded, and the unsupported plural alias remains unresolved.

This migration changes no production Java, removes 216 legacy-data characters,
and adds 893 reviewed-data characters. The full clean suite is 952/952, the
packaged JAR is 1,255,893 bytes, and the production-Java source audit remains
1,077,288 characters / 269,322 estimated tokens.

## Construction recipes use reviewed plank identities

Moved the five exact Construction recipes from the legacy catalog into the
shared reviewed catalog. Oak larders and doors now use item 8778 (Oak plank),
teak garden benches and mounted mythical capes use item 8780 (Teak plank), and
mahogany tables use item 8782 (Mahogany plank). All five explicitly require P2P;
F2P and unknown membership now fail closed at the recipe boundary. Reusable
hammers, saws, and the returned mythical cape remain setup rather than consumed
inputs.

This migration changes no production Java, removes 642 legacy-data characters,
and adds 2,275 reviewed-data characters. The full clean suite is 953/953, the
packaged JAR is 1,256,012 bytes, and the production-Java source audit remains
1,077,288 characters / 269,322 estimated tokens.

## Magic recipes use reviewed rune identities

Moved the five exact universal Magic recipes from the legacy catalog into the
shared reviewed catalog: Wind Strike, Fire Bolt, Fire Blast, High Level Alchemy,
and Curse. Their base rune quantities now use RuneLite item identities and
pinned Wiki revisions, so universal resource evidence cannot substitute a
different display label. All five remain explicitly F2P. Existing staff-source
setup wording is preserved, and curated execution profiles remain separate;
this migration does not broaden or replace their input contracts.

This migration changes no production Java, removes 751 legacy-data characters,
and adds 2,594 reviewed-data characters. The full clean suite is 954/954, the
packaged JAR is 1,256,152 bytes, and the production-Java source audit remains
1,077,288 characters / 269,322 estimated tokens.

## F2P pottery uses reviewed attempt identities

Moved the six exact F2P pot, pie-dish and bowl shaping/firing actions from the
legacy catalog into the shared reviewed catalog. Shaping consumes item 1761
(Soft clay); firing consumes the matching unfired item identities. Firing setup
now states that quantities are per attempt and low-level pottery can crack, so
successful outputs are not promised. The members-only plant-pot and pot-lid
rows remain legacy and are not certified by this slice.

This migration changes no production Java, removes 705 legacy-data characters,
and adds 2,925 reviewed-data characters. The full clean suite is 956/956, the
packaged JAR is 1,256,350 bytes, and the production-Java source audit remains
1,077,288 characters / 269,322 estimated tokens.

## Plant pots reviewed; quest-locked pot lids fail closed

Moved the two plant-pot shaping/firing actions into the shared reviewed catalog
with explicit P2P access and exact Soft clay / Unfired plant pot identities.
Firing remains attempt-based and does not promise an output. Removed both pot-lid
legacy rows instead of certifying them: crafting pot lids requires partial One
Small Favour progress, which the universal recipe contract cannot currently
observe. They now remain unresolved across every membership state.

This migration changes no production Java, removes 494 legacy-data characters,
and adds 1,009 reviewed-data characters. The full clean suite is 957/957, the
packaged JAR is 1,256,325 bytes, and the production-Java source audit remains
1,077,288 characters / 269,322 estimated tokens.

## Molten glass reviewed; mould-bound tiaras fail closed

Moved conventional furnace `Molten glass` into the shared reviewed catalog as
an explicitly P2P recipe with exact Bucket of sand and Soda ash identities,
pinned to Wiki revision 15288555. The setup keeps Superglass Make outside this
contract because its spell requirements and variable yield differ. Removed the
two tiara legacy rows instead of continuing to treat silver or gold bars alone
as sufficient: both actions require a carried, reusable tiara mould, which the
consumed-input recipe contract cannot currently prove. Both tiaras now remain
unresolved across every membership state.

This boundary changes no production Java, removes 484 legacy-data characters,
and adds 511 reviewed-data characters. The full clean suite is 958/958, the
packaged JAR is 1,256,287 bytes, and the production-Java source audit remains
1,077,288 characters / 269,322 estimated tokens. The legacy catalog is now
confined to the separately bounded Herblore family.
