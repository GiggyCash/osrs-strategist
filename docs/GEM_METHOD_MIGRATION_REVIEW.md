# Existing gem method: flow evidence and ranking decision

Research date: 2026-09-30. Audited development commit:
`cce6379940d104bfa4d8f7dde6611b9d8a3daa36`.
No gem method contract is enabled by this document.

## Verified existing-route facts

Current Wiki recipe templates agree with the six existing reviewed input rows.
Each consumes one uncut gem and retains one nonstackable cut gem. A reusable
[chisel](https://oldschool.runescape.wiki/w/Chisel) is required (item1755,
revision15341754). No disposal or output stacking should be assumed.

| Recipe/source | Level | Output ID | Wiki revision |
|---|---:|---:|---:|
| [Sapphire](https://oldschool.runescape.wiki/w/Sapphire) | 20 | 1607 | 15357748 |
| [Emerald](https://oldschool.runescape.wiki/w/Emerald) | 27 | 1605 | 15357750 |
| [Ruby](https://oldschool.runescape.wiki/w/Ruby) | 34 | 1603 | 15357755 |
| [Diamond](https://oldschool.runescape.wiki/w/Diamond) | 43 | 1601 | 15357761 |
| [Dragonstone](https://oldschool.runescape.wiki/w/Dragonstone) | 55 | 1615 | 15357763 |
| [Onyx](https://oldschool.runescape.wiki/w/Onyx) | 67 | 6573 | 15357764 |

The first four recipes are F2P; dragonstone/onyx are members. The existing
crafting_gems training method itself is members-only and this migration should
not silently broaden that method's membership. Soft gems and zenyte remain
outside its reviewed references. The earlier zenyte XP disagreement is not
resolved by this research.

## Architecture issue before enabling the full contract

MethodReadiness.adjustment charges a fixed burden per BUY step, regardless of
purchaseCost. MethodReadinessService breaks equal-adjustment ties by recipe
level. Purchase cost currently proves affordability but does not rank recipe
value. For two recipes with equal preparation shape and verified affordable
quotes, the higher-level recipe can win even if it costs far more.

The current Onyx page explicitly discourages using it for training because of
potential coin losses and acquisition difficulty. Therefore mechanically valid
inputs, outputs and sufficient cash do not establish a good recommendation.
The same policy gap may affect other contracted recipes; it is not a reason to
add an onyx-specific Java exception.

Before enabling crafting_gems, choose a generic recipe-value policy that can
consider purchase cost, retained output value, verified XP/action, account goals
and ownership opportunity cost. Candidate options for reviewer discussion:

1. Reuse the existing strategy-value boundary with recipe-level evidence, keeping
   unresolved value from manufacturing a purchase preference.
2. Introduce an explicit reviewed per-recipe intent/acquisition restriction while
   the general value model is built. This is a product restriction, not a claim
   that cutting the item is mechanically impossible.

Do not substitute a blanket lower-price or higher-level rule for this decision:
both can conflict with XP efficiency, player goals and owned-resource value.
Do not equate Iron self-sourcing with zero opportunity cost. No schema or runtime
behavior has changed while the policy question is pending.

## Proposed integration boundary for reviewer decision

The preferred direction is option 1, with separate feasibility and value stages.
The [issue #9 architecture decision](https://github.com/GiggyCash/osrs-strategist/issues/9#issuecomment-5920677558)
approved this boundary with coarse, bounded economic tie-breaking after
feasibility and strategy/session/verified outcome fit. Missing monetary evidence
must remain unknown and must not independently block an otherwise feasible plan.

The pre-migration call graph lost alternatives too early: TrainingMethodSelector called
MethodReadinessService.evaluate, which chose one recipe using preparation
burden and level before a Recommendation receives StrategicValue. Adding an
economic adjustment only in MethodRecommendationValueService would therefore
score the surviving recipe without reconsidering discarded alternatives. That
service currently contributes travel evidence only. Its name does not imply an
existing recipe economics implementation.

The approved design calls for evaluation of all eligible recipe contracts, retaining the
existing single-result evaluate facade until its callers migrate. Each result
must carry its own preparation, capacity, batch and evidence. Filter feasibility
before comparing value; never combine one recipe's cheap inputs with another's
XP, outputs or setup. Select the recipe with the same strategy/session context
used to rank its method, and carry that chosen evaluation into the final shared
recommendation layer and guidance. Recipe identity must survive semantic
deduplication as part of the selected plan, not as merged quantities.

The value evidence should distinguish:

| Evidence | Meaning and limits |
|---|---|
| Immediate purchase cost | Checked total for the actual missing consumables and reusable tools; the existing preparation cost proves cash affordability only. |
| Consumed input value | Value of the full consumed batch, including owned stock. Ownership does not make a valuable input free. |
| Retained outputs | Item identities and quantities from the reviewed flow; market value is not cash until a separately supported sale occurs. |
| XP outcome | Verified deterministic XP/action, or an explicitly variable/unknown outcome. Do not derive exact cost/XP for an unknown outcome. |
| Acquisition and setup | Separate retrieval/tool/setup costs from recurring consumable cost; do not charge a reusable tool on every batch. |
| Account purpose | Goal/output demand and session context. Iron opportunity cost requires acquisition/use evidence; a GE quote is not an Iron replacement route. |

Every monetary value needs quote provenance and checked arithmetic. Missing or
stale quotes remain unknown, never zero; overflow must invalidate the estimate.
Estimated output proceeds must not fund the current purchase. Current shared
StrategicValue components are bounded scores rather than currency amounts, so
raw coins must not be assigned directly to opportunityCost or resourceFit.
An explicit, reviewable normalization policy is still required. Likewise,
combining evidence with StrategicValue.merge must not charge the same economic
burden again in the final ranking score.

Before enabling gem contracts, regression coverage should demonstrate a cheaper
but slower recipe, an expensive owned input, a goal-relevant retained output,
missing input/output quotes, unaffordable up-front purchases despite valuable
outputs, deterministic versus variable XP, and Main/Iron/UIM transitions.
Expected winners require the reviewer-approved policy; tests must not enshrine
an arbitrary cheapest-input or highest-level rule. Existing Cooking and bow
contracts must preserve their verified feasibility and preparation behavior.

## Recipe candidate boundary — 2026-10-01

The first implementation slice retains every level-eligible recipe evaluation
through `TrainingMethodSelector.rankedCandidates`. Each candidate carries its
own preparation, capacity, batch, checks and guidance; absent contracts still use
the legacy evidence path, while unknown access produces only a diagnostic.
Feasibility remains a hard ranking tier. Method-level strategy/session scoring
is computed once and shared across its recipe candidates.

The single-result readiness facade and existing preparation/level fallback order
remain compatible. This is not an economic or XP ranking policy. The skill
provider still chooses the first renderable ranked plan and carries that exact
readiness into the final queue. The next slice can compare these retained recipe
candidates using reviewed outcome/purpose evidence and bounded economics before
that selection, without reevaluating or mixing their execution plans.

Gem full contracts remain disabled. Consumed-input opportunity cost, reusable
setup value, retained-output value and verified XP evidence are still unmodeled
at this boundary. No cost-per-XP, output-funded purchase or Iron replacement-price
assumption has been introduced. Regression coverage checks simultaneous ordinary
and yew bow candidates, distinct batches and guidance, legacy winner behavior,
level eligibility, and membership/access fail-closed behavior.

Validation: 38 focused tests passed. JDK 11 `./gradlew --offline --no-daemon
clean test jar --continue` ran 872 tests: 869 passed, exactly the three unchanged
quest baselines failed (A Ruff Situation and two expected 213 / actual 211),
with no errors or skips; JAR build passed. Freshness/source validators,
changed-file privacy scan and `git diff --check` passed. Production delta:
+17 net Java lines, no catalog/schema changes. No push or release performed.

## Bounded economics integration — 2026-10-01

The next slice adds optional account-mode-specific reviewed `StrategicValue`
economics to retained recipe candidates. Existing value weights break only
equal same-method ranking scores; incomplete economic evidence preserves the
previous order. See [the comparison contract](METHOD_INTELLIGENCE.md#recipe-economics-comparison-boundary).
Production catalog rows remain unchanged/unrated. This does not complete gem
economics or model cash returns; gem full contracts remain disabled.

At this slice checkpoint, 39 focused tests passed. JDK 11 `./gradlew --offline --no-daemon
clean test jar --continue`: 879 tests, 876 passed, exactly the same three quest
baseline failures, no errors/skips; JAR passed. Freshness (14 families), source
validation (53 sources), privacy scan and diff check passed. This economics slice
adds exactly 43 net production Java lines beyond the prior uncommitted boundary
slice (+60 combined versus `bc082bd`), with no new production Java files or catalog
data changes. The optional economics map is a backward-compatible schema
extension; no recipe has received a production rating. All changes remain local
and uncommitted; nothing pushed or published.

## Foundation follow-on — 2026-10-01

Economics is now stored as reviewed normalized facts (`resourceFit`,
`opportunityCost`, evidence) rather than precomputed policy value. Existing
`StrategicValue` weights convert those facts at the ranking boundary. The selected
plan also retains a typed comparison basis and player-facing explanation for its
closest alternative. Equal or incomplete economics preserve the old stable order.

Reviewed banking behavior now reaches strategy policy. This corrects the legacy
classification of carried-log bow cutting as a conventional UIM bank loop without
weakening conventional-bank restrictions. Six reviewed F2P platebody recipes then
exercise the same pipeline in Smithing with no Smithing-specific Java branch.
Production recipe economics remain unrated and gem contracts remain disabled.
