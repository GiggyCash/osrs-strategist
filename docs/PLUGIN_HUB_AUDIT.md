# Plugin Hub compliance audit

Audit date: 2026-08-30

## Current official sources reviewed

- [RuneLite `plugin-hub` README](https://github.com/runelite/plugin-hub):
  standard builds, metadata, icon, and manifest.
- [RuneLite `example-plugin` repository instructions](https://github.com/runelite/example-plugin/blob/master/AGENTS.md): Java/API, dependencies,
  resources, licensing, and prohibited behavior.
- [RuneLite Plugin Hub Review wiki](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review),
  updated 2026-06-19.
- [RuneLite Information about the Plugin Hub wiki](https://github.com/runelite/runelite/wiki/Information-about-the-Plugin-Hub).
- [RuneLite Rejected or Rolled Back Features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features),
  updated 2026-06-29: gameplay/input restrictions, HTTP exposure,
  credential handling, Java-only code, reflection/native/process/runtime-code
  restrictions, and reviewability expectations.

## Packaging and metadata

- `runelite-plugin.properties` contains `displayName`, `author`, GitHub Issues
  `support`, concise `description`/`tags`, the plugin class, `version=0.2.0`,
  and `build=standard`.
- Production is Java 11-compatible and uses RuneLite's standard build. The
  development dependency follows `latest.release` and resolved to RuneLite
  1.12.37 during this audit; Plugin Hub still supplies its own current standard
  build at submission time.
- Root `icon.png` is a transparent 48×48 PNG, within the documented maximum
  48×72 icon bounds. A 32×32 classpath copy supplies the live navigation icon.
- BSD 2-Clause licensing and the required public repository files are present.
- The submission manifest is prepared in `docs/PLUGIN_HUB_SUBMISSION.md`; it
  uses only the currently documented `repository` and `commit` fields. Its
  filename is lowercase/dash-only and has no extension.

## Runtime behavior

- Production source has no Java reflection/`TypeToken`, JNI/JNA, native-memory
  access, process execution, dynamic loading, Java serialization, input
  injection, sockets, or plugin-owned HTTP client. Main-account price
  comparisons can read RuneLite's maintained `ItemManager` price cache; Compass
  does not contact a Compass, Wiki, or other third-party endpoint.
- The plugin does not automate gameplay or add/modify game-action menu entries.
- The plugin does not manipulate the game window, chat input, outgoing chat,
  or other players' data.
- Combat content is planning/preparation only. It does not predict attacks,
  switch prayers, mark future hazards, or simulate encounters.
- Account hashes are equality-only live identity evidence. They are not
  displayed, logged, transmitted, or persisted as player-facing content.
- Startup/shutdown add and remove the navigation button and overlays through an
  idempotent lifecycle guard. Details and Method Guidance visibility are
  independent; disabling both leaves the sidebar operational.
- Bundled content and icons use classpath resource streams; the plugin does not
  assume its JAR is unpacked.
- No third-party runtime dependency, telemetry, hosted entitlement, donation
  SDK, or payment API is present.

## External links

The Plugin Hub `support` property remains the GitHub Issues destination. The
separate optional `Support Compass` destination is one blank centralized HTTPS
value and the control is hidden while blank. A browser can open it only after
an explicit player click; Compass performs no automatic request.

## Repository-backed result

`PluginHubComplianceTest` guards packaging, icon bounds, metadata, and
mechanical prohibited-feature checks. Repository tests also cover bounded
state, account switching, lifecycle, startup refresh coalescing, and the
player-visible actionability boundary. Final command results are recorded in
`docs/RELEASE_CHECKLIST.md` only after they run. Plugin Hub CI and reviewer
approval remain external and cannot be claimed by this repository.
