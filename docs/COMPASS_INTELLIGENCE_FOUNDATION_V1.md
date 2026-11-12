# Compass Intelligence Foundation v1 — local evidence record

Local development branch: `codex/cooking-intelligence`. This record describes
the foundation work developed after checkpoint `bc082bd`; it is not a release
claim. The three quest-data failures recorded in the historical validation below
were subsequently resolved by commit `845d4b7`.

## Completed boundaries

- Recipe alternatives survive readiness evaluation with independent access,
  preparation, capacity, batch, economics and guidance.
- Strategy/session ranking sees every eligible recipe candidate. Feasibility is
  a hard tier; missing economic evidence is unknown and cannot look free.
- Reviewed normalized economics are facts in structured data. Existing
  `StrategicValue` weights remain strategy policy and apply only as a same-method
  tie-break after existing preparation/strategy scores.
- The selected training plan retains a structured comparison with the closest
  alternative, a typed deciding basis and a concise player-facing tradeoff
  explanation.
- Reviewed method banking behavior supersedes legacy family assumptions. This
  proves local-processing UIM routes without weakening conventional-bank gates.
- Production recipes remain economically unrated and gem contracts remain
  disabled. No live-price, Wiki-ingestion or backend expansion has begun.
- Six reviewed F2P platebody recipes prove the same boundary outside Cooking and
  Fletching. They add structured data and tests without Smithing-specific ranking
  logic.

## Reusable decision path

`MethodReadinessService` expands a reviewed method contract into independent
recipe candidates. `TrainingMethodSelector` keeps those candidates distinct while
it applies hard feasibility, preparation, existing strategy/session scores and an
optional complete-cohort economics tie-break. `TrainingPlan` carries the selected
readiness and nearest-alternative comparison into `RecommendationEngine` and
`Presentation`; guidance therefore renders from the exact winning candidate.

Game facts stay in the reviewed catalogs and readiness models. Existing policy
weights decide how normalized facts affect a player. Unknown economics disables
economic comparison for the feasible cohort instead of treating the missing value
as zero. Stable catalog order makes complete ties deterministic.

## Remaining content work

- Add production economics only after every compared feasible alternative has
  reviewed normalized evidence for the relevant account mode.
- Expand reviewed method contracts and recipe facts to more domains. Smelting,
  Blast Furnace, boosts and catalysts need their own setup evidence.
- Keep gem contracts disabled until their opportunity-cost/value evidence is
  reviewed. No GE or live-price layer is required for the current boundary.
- A future knowledge import can populate these data contracts after its own
  provenance and validation design; the local reasoning path does not require a
  backend.

The remaining work is principally evidence and content coverage. The tested local
architecture already carries candidate-specific facts from reviewed data through
strategy/session selection, explanation and exact guidance across UIM Fletching
and F2P Smithing.

The development-time acquisition and provenance boundary that now supplies a
representative Smithing family is documented in
[Compass Knowledge Foundation v1](COMPASS_KNOWLEDGE_FOUNDATION_V1.md).

## Final local validation

- Focused candidate, economics, comparison, reviewed-banking, Smithing and
  end-to-end pipeline suites pass.
- JDK 11 `./gradlew --offline --no-daemon clean test jar --continue` built the
  JAR and ran 891 tests: 888 passed and only the three established quest-data
  failures remained (A Ruff Situation and two expected 213 / actual 211 checks).
- The content-census validator ran 5 tests: 4 passed and the same known quest
  manifest assertion failed. Freshness validation passed for 14 families;
  strategy-source validation passed for 53 sources. The changed-production-file
  privacy scan and `git diff --check` passed.

Against checkpoint `bc082bd`, production Java is 71 files / 28,818 lines, a net
increase of exactly 153 lines, 8,990 characters and 2,247 estimated tokens, with
no new production Java file. The repository size audit therefore still reports
its existing `architecture intervention` guardrail. This growth implements the
generic candidate boundary, economics fact/policy split, ranking comparison and
reviewed banking policy; the representative Smithing expansion is data-only.

`method-intelligence.json` adds 159 lines: six platebody recipes and one method
record covering two existing Smithing method IDs. It contains no production
economics ratings. Reviewed-action ingredient data is unchanged. All work remains
local and uncommitted; nothing has been pushed or published.
