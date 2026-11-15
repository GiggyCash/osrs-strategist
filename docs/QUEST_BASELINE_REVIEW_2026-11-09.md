# Quest baseline review — 2026-11-09

RuneLite 1.13.6 exposes 216 quest identities. The five identities added after
the previous 211-record snapshot were reviewed against the OSRS Wiki Action API
and added to the existing `QuestKnowledgeCatalog`; no runtime networking was
introduced.

| RuneLite identity | Reviewed Wiki page | Revision | Membership |
| --- | --- | ---: | --- |
| A Ruff Situation | A Ruff Situation | 15383246 | Members |
| Crab Quest | Crab Quest | 15396724 | Members |
| The Fractured Archive | The Fractured Archive (miniquest) | 15386187 | Members |
| Mudfoot's Big Adventure | Mudfoot's Big Adventure | 15396352 | Free-to-play |
| The Writing's on the Wall | The Writing's on the Wall | 15396809 | Members |

The Fractured Archive identity is the miniquest, not the same-named raid page.
Its direct prerequisite is While Guthix Sleeps, and completion requires a full
raid run; the planner therefore retains explicit combat/setup checks rather
than treating lobby access as completion readiness. Mudfoot's four selectable
combat lamps are intentionally not represented as fixed skill XP.

The pinned `A Ruff Situation` details expose no conventional item field. Its
reviewed local contract therefore models needle plus thread or a costume
needle, requires grain, and keeps the accepted-fur choice as an explicit check
rather than guessing which fur identities are valid.

## Resolved validation baseline

The three previously reported quest-data failures were baseline-drift failures,
not accepted product failures. Updating the quest census from 211 to the 216
RuneLite 1.13.6 identities and making unreviewed new hiscore bosses fail closed
resolved them without weakening catalog validation. The focused coverage,
authoritative quest, quest-census, and PvM catalog suites now pass; the full
clean suite is tracked in draft PR #12.
