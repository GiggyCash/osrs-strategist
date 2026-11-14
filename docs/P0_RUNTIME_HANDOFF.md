# P0 runtime checkpoint — 2026-10-01

Development branch: `codex/cooking-intelligence`. This checkpoint fixes the
confirmed startup failure and measured logged-in stalls. No release is submitted.

## Root causes and scope

- `OsrsStrategistPlugin.java`: startup/config/sidebar refreshes could read live
  state off the client thread. The shared entry point now uses `ClientThread`
  and rejects callbacks queued for an earlier enable cycle.
- `LiveReaders.java`: reuse the exact clue-injection correction from release
  `357dadf`, missing from development. Resolve the existing clue plugin through
  `PluginManager`; its private injector is not an exported dependency binding.
- `StateModels.java` / `QuestGraphs.java`: repeated normalized status scans and
  prerequisite-result rescans stalled the client thread. Use a snapshot-local
  normalized index and traversal-local visited set, preserving legacy results.
- `DomainTypes.java`: reuse the constant normalization regex.
- Lifecycle/injection and quest-graph tests cover the actual thread queue,
  stale callbacks, aliases, Unicode, snapshot isolation, cycles, shared
  prerequisites and catalog-wide equivalence with the previous traversal.

P0 production Java delta: +45 net lines, including +23 for performance.
No gameplay policy, scoring, refresh frequency or safety requirement changed.

## Enabled versus disabled evidence

Same idle logged-in scene; 30-second frame/CPU samples:

| Compass state | Mean FPS | p95 frame | Maximum frame | Client CPU |
|---|---:|---:|---:|---:|
| Before fix | 34.6 | 22.21 ms | 904.86 ms | 17.06 s |
| Disabled | 50.0 | 22.10 ms | 31.48 ms | 11.17 s |
| Final fix | 48.4 | 22.26 ms | 156.55 ms | 12.35 s |

Compass execution samples fell from 221 to 42. This isolates the near-second
stalls to Compass and demonstrates the improvement. Occasional ~157 ms spikes
remain; this is one account/scene, not a guarantee for every activity. An earlier
986 ms sample included the welcome screen. A disconnected/login-screen sample
was excluded; the final sample followed normal login and was logged in.

## Verification

- Logged-in enable -> disable -> enable, actual sidebar removal/registration,
  config proxy and client-thread queue checks passed.
- 62 focused tests passed. Checkpoint `./gradlew clean test jar --offline`:
  **868 tests, 865 passed, 3 unchanged baseline failures**, zero errors/skips.
  Failures: A Ruff Situation; two expected 213 / actual 211 quest-count checks.
  The suite is not fully green; no baseline assertions were weakened.
- JAR build and standard Plugin Hub packaging/API recorder passed on Java 11 /
  RuneLite 1.13.1. Packaged production inputs match the verified working tree.
- `git diff --check`, source validation and preservation checks passed.
- Verification used the authoritative tree including preserved jewellery work;
  the P0 commit deliberately excludes that separate content slice.

Used the existing authenticated Play Now action. No credential/session file was
inspected, copied or manually changed. Temporary probes recorded only execution
stacks, frame intervals and timing, excluding environment variables, properties
and file/network payloads. No instrumentation ships in production. The test
client was closed after verification; no gameplay actions were performed.

Local evidence: `/tmp/compass-p0-metrics.log`,
`/tmp/compass-p0-scene-before.jfr`, `/tmp/compass-p0-disabled.jfr`,
`/tmp/compass-p0-final-logged-in.jfr`, `/tmp/compass-p0-perf-hub.log`,
`/tmp/compass-p0-checkpoint-full.log`.

## Roadmap continuation

Return to the preserved Step 6 jewellery migration. Its earlier full run had
only the same three quest baselines. All 44 added ingredient rows match their
pinned Wiki evidence for materials, quantities, membership, moulds, item IDs and
stackability; focused jewellery and intelligence-safety tests pass. The slice
adds 1,128 JSON lines and removes 18 lines of legacy Java name inference.
Full-method contracts and the separate gem-value policy remain later work.
