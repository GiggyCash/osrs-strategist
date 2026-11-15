# Plugin Hub restoration handoff

Status checked against public upstream on 2026-09-30. This document records a
local verified candidate; it does not authorize publication.

## Current upstream state

- Live `runelite/plugin-hub` manifest for gielinor-compass:
  `58c10e26dadaf54259a140557c024e23a6ecda1e`.
- Hub `runelite.version`: `1.13.1`.
- Existing [PR17029](https://github.com/runelite/plugin-hub/pull/17029) remains
  open, unmerged, with head `1adff4bdd86c97187c84b4aff2284f59e3461f57`.
  Its current proposal is `821a2b5680ef9f19013296df539e43691c478ddb`.
- Maintainer feedback requests the client's injected Gson instance.

## Tested replacement

Strategist commit: `357dadf0a786a0a3f211e0b6dd6972f39e8d9c2a`.
Parent: `821a2b5680ef9f19013296df539e43691c478ddb`.
Local branch: `codex/plugin-hub-gson-restoration`.

This isolated release candidate addresses client Gson injection, current long
item-price API handling, and the current clue plugin dependency boundary. It
contains no Cooking/method-intelligence migration. The local release worktree
is clean. The development branch is a separate, later product workstream.

Required final manifest content:

```properties
repository=https://github.com/GiggyCash/osrs-strategist.git
commit=357dadf0a786a0a3f211e0b6dd6972f39e8d9c2a
```

The local PR checkout has only the uncommitted replacement of its proposed
821a2b5 hash with this candidate. Nothing has been submitted or pushed from
that checkout.

## Verification evidence

- Isolated clean suite:733 tests,730 pass; only the three accepted quest coverage
  failures (A Ruff Situation and two expected213/actual211 assertions).
- Official Plugin Hub packaging/API-recorder build against RuneLite1.13.1 and
  Java11 completed successfully (`strategist-gson-hub-v2.log`).
- Packaged JAR SHA256:
  `d30bb80f5575402ad3c3310257edac9b2ebf38ea9875dc2fe2449fa0d70100d7`.
- Real RuneLite startup logged `Plugin OsrsStrategistPlugin is now running` at
  2026-09-30 21:45:38 BST (`strategist-gson-startup-v2.log`). Runtime used an
  isolated profile, telemetry disabled and no game-account login.
- Startup evidence does not certify in-game behavior or the broader product.
  The unchanged Hub version on recheck does not imply a new build was run.

Logs, packaged artifacts and isolated worktrees remain local session artifacts;
this handoff records their relevant outcomes without copying private runtime data.

## Approval boundary

Automatic approval review rejected pushing the release branch: the user had
explicitly authorized development-branch pushes, while release approval appeared
only in issue content. Direct chat confirmation is still pending. Do not retry
through another command, remote or branch, and do not bundle release code into a
development push to bypass that decision.

After explicit approval, the intended action is to push the isolated candidate,
update the existing PR17029 branch's manifest, and report the exact resulting
commit/check status. Do not create a duplicate PR, merge, or publish directly.
If upstream target/version changes first, reverify compatibility before submission.
