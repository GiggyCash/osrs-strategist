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
