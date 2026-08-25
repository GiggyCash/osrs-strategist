# Development setup

## Requirements

- Git
- A JDK capable of running Gradle and compiling Java 11-compatible bytecode
- The repository's checked-in Gradle wrapper

## Build and test

From the repository root:

```sh
./gradlew clean test --warning-mode all
./scripts/check-content-census.sh
git diff --check
```

Do not weaken account safety or content evidence rules to make a failing test
pass. The content refresh workflow is documented in `docs/MAINTENANCE.md`.

## Development client

```sh
./gradlew run
```

This starts RuneLite in developer mode with Gielinor Compass on the test
classpath. Confirm the sidebar and overlays initialize without exceptions. Do
not automate gameplay or require credentials as part of repository testing.

## Repository identity

The public product name is Gielinor Compass. The package, config group, profile
group, and repository identifiers retain `osrs-strategist` for compatibility.
