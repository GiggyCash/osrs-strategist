# Release checklist

## Automated/repository verification

Check these only after the named command or inspection succeeds on the final
commit.

- [x] Required feature branch confirmed; `main` untouched; `AGENTS.md` remains
  intentionally untracked and unstaged.
- [x] `./gradlew clean test --no-daemon`
- [x] `./gradlew check --no-daemon`
- [x] `./gradlew javadoc --no-daemon` (classify warnings; do not hide new ones)
- [x] `./scripts/refresh-content.sh`
- [x] `./scripts/check-content-census.sh`
- [x] `./scripts/fedora-check.sh`
- [x] `python3 scripts/review-strategy-sources.py --validate`
- [x] `python3 scripts/review-strategy-sources.py --check-live`
- [x] `git diff --check`
- [x] Plugin Hub compliance, startup coalescing, lifecycle/account switching,
  bounded progress state, recommendation tournament, and publication metadata
  tests pass in the final suite.
- [x] `runelite-plugin.properties`, README, BSD-2-Clause LICENSE, root icon,
  classpath icon, support URL, version `0.2.0`, and standard-build metadata agree.
- [x] Production security/privacy scan finds no prohibited runtime behavior,
  gameplay automation, telemetry, player-data transmission, or raw account-ID
  exposure.

`javadoc` succeeds with 100 existing missing-comment/parameter documentation
warnings. The sprint introduced no compiler, deprecation, unchecked-operation,
or API warnings; the remaining Javadoc warnings are non-release-blocking
documentation debt.

## Manual live QA required

- [ ] Complete the short [final live QA checklist](FINAL_LIVE_QA.md) in a real
  RuneLite session. Record any unavailable account types as covered only by the
  automated scenario matrix.

## External Plugin Hub CI/review

- [ ] Push the final plugin commit only after maintainer approval and confirm
  its full SHA is publicly reachable.
- [ ] Generate `plugins/gielinor-compass` from
  [the submission instructions](PLUGIN_HUB_SUBMISSION.md).
- [ ] Open the Plugin Hub PR; pass upstream build/automated checks and address
  reviewer feedback in that same PR.
- [ ] Wait for RuneLite merge/approval. Repository checks never imply Plugin Hub
  approval.
