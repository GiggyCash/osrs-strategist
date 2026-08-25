# Plugin Hub compliance audit

Audit date: 2026-08-25

Official references reviewed:

- RuneLite `plugin-hub` submission and standard-build requirements
- RuneLite `example-plugin` agent rules and restrictions
- RuneLite Plugin Hub Review and Rejected/Rolled-Back Features guidance

Repository-backed result:

- Java 11 target is explicit.
- Standard Plugin Hub build metadata is present.
- Package, config group, repository/project identifier, and profile group are
  stable and non-template values.
- BSD 2-Clause licensing is present.
- Production source has no Java reflection/`TypeToken`, JNI/JNA, native-memory
  access, process execution, dynamic code loading, Java serialization, input
  injection, or runtime networking.
- The plugin neither adds nor modifies game action menu entries and does not
  automate gameplay.
- Combat content is planning/preparation only. It does not predict attacks,
  display prayer switches, count attacks, mark future hazards, or simulate
  encounters.
- Account hashes are equality-only local state. They are never rendered,
  persisted as content, transmitted, or logged.
- Startup/shutdown add and remove the navigation button and all overlays.
- Bundled content is read from classpath streams; the plugin does not assume
  its JAR is unpacked.
- No third-party runtime dependencies or hosted entitlement checks are added.

`PluginHubComplianceTest` guards the mechanical repository checks. Plugin Hub
CI and reviewer approval remain external release steps and cannot be claimed by
this repository alone.
