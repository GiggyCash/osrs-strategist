# Goal Forecast roadmap

Goal Forecast is post-publication work. It is not part of the `0.2.0` release.

## Semantics

Forecasting should derive from the existing dependency graph:

`goal → remaining requirements → selected methods/actions → rate range`

A future requirement model must keep these states distinct:

- **REQUIRED** — an authoritative game requirement.
- **RECOMMENDED** — strategic preparation, never presented as mandatory.
- **COMPLETED** — observed satisfied work.
- **REMAINING** — verified deterministic work still needed.
- **UNKNOWN** — state Compass cannot safely observe.

For example, a suggested Ranged level for Fire Cape is RECOMMENDED unless the
game itself enforces it.

## Time and uncertainty

Estimates must distinguish MODELED, PERSONALIZED, and UNKNOWN rates, plus
DETERMINISTIC PREPARATION and RNG-DEPENDENT WORK. Use ranges where inputs vary
and never turn a random drop into an exact completion time. Strategy and
session choices may change method selection and therefore the range.

## Local history

Future account-scoped persistence may record session, daily, 7-day, and 30-day
XP; per-skill and total XP; personal observed XP/hour; and goal-specific
progress rates. It belongs beside the existing account-scoped profile stores,
with unchanged-state write deduplication. No runtime networking, telemetry,
cloud tracking, or cross-account sharing is permitted.

## Presentation

The future UI may show what's left, deterministic progress, uncertainty, the
current step, and a milestone path derived from real dependency edges. Charts
should clarify progress rather than decorate it. No showcase-only hard-coded
paths, fake precision, calendar promises, or unsupported completion dates.

## Explicitly deferred

Full Goal Progress UI, XP charts, history dashboards, personal XP/hour,
time-to-goal forecasting, completion calendars, milestone timelines,
supporter memberships/perks, cloud sync, and hosted services are deferred
until after first Plugin Hub publication.
