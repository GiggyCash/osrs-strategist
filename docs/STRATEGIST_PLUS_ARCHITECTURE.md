# Strategist Plus Architecture

Strategist Plus is a future optional hosted-services layer. The local planner is the product core and must remain useful without an account, subscription, server, or network connection.

## Product boundary

Core/local features are never modeled as premium entitlements:

- Local adaptive recommendation engine.
- All supported account modes: Main, Ironman, GIM, UIM, and hardcore variants.
- F2P/P2P content filtering.
- Strategy style, session intent, quest tolerance, and goals.
- Method guidance, evidence checks, local overlays, cooldowns, and preference learning.
- Local account memory, resource readiness, Wilderness policy, and safety rules.
- Locally available game-knowledge coverage.

Possible Plus capabilities are represented by `StrategistFeature` and currently include:

- Cloud profile sync.
- Cross-device history.
- GIM team planning.
- Remote reminders.
- Web dashboard.
- Optional online reasoning.

Nothing in the current build grants or calls these hosted features.

## Current network behavior

`StrategistRemoteGateway` is intentionally disabled:

- No endpoint is configured.
- No HTTP client is created.
- No login or billing system exists.
- No telemetry is sent.
- `canTransmit()` is false.
- Sync calls return a disabled result even if a test grants a Plus entitlement.

This lets the codebase establish stable boundaries without changing the privacy behavior of the current plugin.

## Future sync contract

`PlusSyncEnvelope` starts at schema version 1. It is deliberately category based. Potential sync categories are preferences, goals, progression history, group plans, and reminder state.

The sync contract intentionally excludes:

- Jagex credentials.
- RuneLite credentials.
- Game login credentials.
- Chat messages.
- Unrelated RuneLite/plugin data.

A future implementation should transmit the minimum fields needed for the feature the player explicitly enables.

## Entitlements

`StrategistEntitlementService` is the only intended feature-entitlement boundary. Strategy logic should not contain scattered `if paid` checks.

The local core is always present in `EntitlementSnapshot`, even when the edition is `FREE`. Future hosted implementations can replace the trusted entitlement snapshot without changing recommendation code.

## Offline/failure behavior

If Plus servers are unavailable, authentication expires, or the player disables online features:

1. Local planning continues.
2. Local account memory continues.
3. Local method guidance continues.
4. Hosted-only capabilities become unavailable gracefully.
5. No local recommendation should become intentionally worse because a hosted service failed.

## Security and review requirements before activation

Before any remote implementation is enabled:

- Add an explicit player opt-in.
- Document every transmitted field.
- Use authenticated encrypted transport.
- Define retention/deletion behavior.
- Add schema migration tests.
- Add request rate limits and failure backoff.
- Re-review RuneLite Plugin Hub requirements for third-party communication.
- Keep authentication tokens separate from RuneScape/Jagex credentials.

The current repository intentionally stops before this point.

## GIM Plus direction

A future team-planning service may synchronize explicitly selected Strategist planning state between group members. It must not turn teammate requests into ordinary local recommendations. Local GIM behavior remains Ironman-like unless Group Storage has actually been observed and the player has enabled its use.

## Business-model flexibility

These seams support free-only, supporter, or free-plus-hosted-service models without rewriting the local engine. The architecture does not make a business-model decision; it only prevents a future business decision from contaminating core recommendation logic.
