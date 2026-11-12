# Compass Progression Foundation v1

This local foundation proves one narrow progression decision without adding a
second strategy engine. It uses the existing reviewed F2P platebody knowledge,
candidate readiness, skill-breakpoint policy, recommendation ranking, guidance,
and presentation paths.

The three quest-data failures recorded in the historical validation below were
subsequently resolved by commit `845d4b7`.

## Proof scenario

The representative player is an F2P Ironman with 67 Smithing and an observed
bank containing a hammer, steel bars, and mithril bars. The test fixes unrelated
skills at 99 so the assertion covers the real global `DO NEXT` ordering. Steel
platebodies are actionable. Mithril platebodies have all modeled supplies
available but require 68 Smithing, so the level requirement alone keeps them out
of the current action.

Compass recommends training Smithing to 68 with the verified steel-platebody
candidate. The recommendation carries the existing training-action unlock value
and evidence. Its guidance names the steel action and preparation. Its details
name mithril platebodies as the next reviewed unlock, show the required and
current levels, and explain that the option remains excluded until the level is
met. At 68, the same ranking path selects mithril platebodies. Adding irrelevant
runite bars at level 67 does not change the winner, and repeated evaluation is
deterministic.

The conventional-bank contract remains unavailable to UIM. This proof therefore
does not weaken account-mode safety to make the progression narrative work.

## Reused path

`GameData` and `AccountSnapshot` supply the level, membership, account mode, and
observed resources. The Wiki-reviewed platebody facts enter through the Knowledge
Foundation's existing `reviewed-action-recipes.json` and
`method-intelligence.json` projection.

`MethodReadinessService` evaluates every currently legal recipe as before. It now
also retains the nearest higher-level reviewed recipe instead of discarding that
identity after calculating the existing review level. The blocked recipe does not
receive an actionability score or compete with executable candidates.

`TrainingMethodSelector` still ranks the available candidates. When the existing
generic next-level breakpoint exactly matches the retained reviewed recipe,
`RecommendationEngine` upgrades that same target to the existing typed
training-action unlock evidence and value. It does not jump to a distant recipe
or alter the selected executable method. `Presentation` renders the retained
blocked fact as a generic `NEXT UNLOCK` section. No Smithing ID, level, or
narrative is embedded in Java.

This keeps the layers distinct:

- Player state says what the account has and its current level.
- Reviewed knowledge says which recipe exists and its requirement.
- Readiness says which options are executable or level-blocked.
- Existing policy ranks the executable action and values the reachable unlock.
- Presentation explains both the immediate action and the blocked next option.

## Extension boundary

Other ordered recipe families using full method intelligence automatically gain
the same next-level explanation. Adding another family is a knowledge/catalog
task plus scenario tests; it does not require another progression engine.

This version models only a single next recipe blocked by skill level. Quest,
diary, multi-skill, facility, or item-unlock progression should reuse the same
pattern only after their typed prerequisite facts survive into readiness. It does
not attempt multi-step path optimization, compare distant goals, or claim that a
higher-level recipe always has greater economic value.

## Footprint

No catalog or knowledge record was added for this goal; the six Knowledge
Foundation platebody records were sufficient. The only production change retains
one already-known next recipe fact, recognizes an exact existing breakpoint, and
renders it through the existing plan.

Relative to the completed Knowledge Foundation baseline, production Java remains
at 71 files and grew by 35 lines, 1,985 characters, and an estimated 497 tokens.
The JAR is 1,238,047 bytes, an increase of 1,135 bytes. Catalog/data growth for
this goal is zero files, records, lines, and bytes.

## Validation

- Progression, Smithing, Knowledge Foundation integration, and candidate
  comparison focused tests pass.
- All eight knowledge-pipeline tests pass, and the six-record projection matches
  both runtime catalogs.
- The full Java suite is 894/897 passed. The only failures are the same three
  known quest coverage/enrichment failures from the 889/892 baseline.
- The JAR builds successfully.
- Freshness validates 15 families; source review validates 53 sources; the
  privacy scan and `git diff --check` pass.
- The content census remains 4/5 because of the known quest-manifest failure.
  The aggregate refresh validator likewise reaches only the same three known
  quest failures after its freshness, source, knowledge, and detector checks
  pass.
