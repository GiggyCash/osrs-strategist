# Plugin Hub submission

## Submission identity

- Repository: `https://github.com/GiggyCash/osrs-strategist.git`
- Source branch: `feature/content-meat-and-potatoes`
- Display name: `Gielinor Compass`
- Manifest filename: `gielinor-compass` (no extension)

The full commit cannot be embedded in this tracked file without creating a
self-referential commit. After the final push, generate the exact manifest
from the remote-tracking HEAD:

```sh
git fetch origin feature/content-meat-and-potatoes
git rev-parse origin/feature/content-meat-and-potatoes
```

Create `plugins/gielinor-compass` in a fork of RuneLite's `plugin-hub`
repository with exactly:

```properties
repository=https://github.com/GiggyCash/osrs-strategist.git
commit=<FULL_SHA_PRINTED_BY_THE_COMMAND_ABOVE>
authors=GiggyCash
```

## Submission steps

1. Confirm the printed SHA equals local `HEAD` and is publicly reachable.
2. Fork/clone `runelite/plugin-hub`; do not add the manifest to this repository.
3. Add only `plugins/gielinor-compass` with the contents above.
4. Run the Plugin Hub repository's current local validation if available.
5. Open a pull request to RuneLite's `plugin-hub` repository.

Plugin Hub CI is expected to check manifest syntax, commit reachability,
standard build, metadata, dependency/API rules, and automated review policies.
Human review may still examine security, gameplay/input behavior, external
links, maintainability, and whether the plugin fits Plugin Hub policy. Planner
quality and real-account behavior remain the maintainer's responsibility.
