# Release checklist

- [ ] Working branch is the intended feature/release branch; `main` is untouched.
- [ ] `AGENTS.md` is still intentionally untracked and unstaged.
- [ ] `./gradlew clean test --warning-mode all` is green with no warnings or unexplained skips.
- [ ] `./scripts/check-content-census.sh` is green.
- [ ] Content freshness and upstream-change audits are current.
- [ ] `git diff --check` is clean and the build is reproducible.
- [ ] Plugin Hub compliance/security checks pass.
- [ ] RuneLite starts; Compass, sidebar, navigation button, and overlays initialize.
- [ ] Overlay toggles work and no immediate exceptions or recomputation loop appear.
- [ ] Root `icon.png` and bundled navigation icon are present and valid.
- [ ] `runelite-plugin.properties`, README, and changelog are current.
- [ ] GitHub Issues support URL is valid.
- [ ] Optional donation URL is separate, valid, and explicit-click only (or remains blank/hidden).
- [ ] Final commit SHA is recorded for submission.
- [ ] Plugin Hub manifest is generated from the pushed feature HEAD.
