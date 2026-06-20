# Bedtime Ban

Bedtime Ban is a server-side Minecraft mod that lets each player set a one-time bedtime in their own timezone. Before the deadline, the server sends reminder messages. When the deadline arrives, the player is temporarily banned for 8 hours.

This branch is a fresh multiversion workspace. The currently supported targets are:

- Minecraft `1.21.1`
- Loader `NeoForge`
- Java `21`
- Minecraft `1.20.1`
- Loader `Forge`
- Java `17`

## Workspace Layout

```text
bedtimeban/
  core/         Pure Java domain logic and persistence codec
  common/       Shared server logic for the active Minecraft version
  forge/        Forge bootstrap, commands, and server integration for 1.20.1
  neoforge/     NeoForge bootstrap, commands, and server integration
  buildSrc/     Shared Gradle conventions
  versionProperties/
```

The build selects one configured target at a time with `-PmcVer=...`. The current configured targets are `1.21.1` and `1.20.1`.

## Build

From the repository root:

```bash
./gradlew -PmcVer=1.21.1 test
./gradlew -PmcVer=1.21.1 build
./gradlew -PmcVer=1.20.1 test
./gradlew -PmcVer=1.20.1 build
```

Run a development server:

```bash
./gradlew -PmcVer=1.21.1 :neoforge:runServer
./gradlew -PmcVer=1.20.1 :forge:runServer
```

Run a development client:

```bash
./gradlew -PmcVer=1.21.1 :neoforge:runClient
./gradlew -PmcVer=1.20.1 :forge:runClient
```

## CI And Releases

GitHub Actions validates both supported targets on every pull request to `master` and every push to `master`.

- `1.20.1` Forge builds on Java `17`
- `1.21.1` NeoForge builds on Java `21`

Each CI run uploads temporary workflow artifacts containing the built loader jar for each target. These are useful for validation, but they are not the permanent public download location.

Tagged releases publish permanent GitHub Release assets. To cut a release:

```bash
git tag v2.0.0
git push origin v2.0.0
```

That tag builds both targets and publishes:

- `bedtimeban-v2.0.0-mc1.20.1-forge.jar`
- `bedtimeban-v2.0.0-mc1.21.1-neoforge.jar`

Release builds inject the version with `-PreleaseVersion=...`, so development stays on `2.0.0-SNAPSHOT` while release jars and embedded mod metadata use the tagged version.

## Install Into Local Test Servers

The repository includes a helper for pulling built jars from GitHub and installing them into the local Docker test server directories:

- `../server-1.20.1-forge/data/mods`
- `../server-1.21.1-neoforge/data/mods`

Prerequisite:

```bash
gh auth login
```

Install the latest successful CI artifacts for a pull request into both local test servers:

```bash
./scripts/install-bedtimeban.sh pr 3
```

Install only one target from a pull request:

```bash
./scripts/install-bedtimeban.sh pr 3 --target 1.21.1
```

Install a tagged GitHub Release into both local test servers:

```bash
./scripts/install-bedtimeban.sh release v2.0.0-rc1
```

Install only one target from a release:

```bash
./scripts/install-bedtimeban.sh release v2.0.0-rc1 --target 1.20.1
```

Notes:

- Pull request installs use `gh` to download the CI workflow artifacts. GitHub expands those artifacts into directories, and the script installs the jar found inside each one.
- Release installs use `gh` to download the release jar assets directly.
- Before installing, the script removes existing `bedtimeban*.jar` files from the target `data/mods` directory so only the newly installed jar remains.

## Current Behavior

- `/bedtime`
  Shows the caller's scheduled bedtime reminder, if one exists.
- `/bedtime timezone`
  Shows the caller's saved timezone.
- `/bedtime timezone <tz>`
  Saves the caller's timezone if they do not already have a scheduled bedtime.
- `/bedtime set <time>`
  Schedules a one-time bedtime using `11pm`, `11:30 pm`, `23:30`, or `23.30`. If a bedtime is already set, the player may only update it to an earlier future time.
- `/bedtime cancel`
  Admin-only self-cancel.
- `/bedtime cancel <playername-or-uuid>`
  Admin-only cancel for another player.

State is persisted in a JSON file under the server's `serverconfig` directory as `bedtimeban-state.json`.

## Notes

- This mod is intended to be server-side only.
- The current implementation follows the behavior described in [`PRODUCT_SPEC.md`](./PRODUCT_SPEC.md).
- The `1.20.1` Forge target uses the same JSON persistence model and product behavior as the new workspace, not the old config-list persistence from the legacy `mc1.20` branch.
- The scheduling quirk from the older versions is intentionally preserved: a bedtime earlier than the current minute but within the same local hour can resolve into the immediate past and trigger on the next enforcement pass.
# Localization

Server-side localization files live in `common/src/main/resources/assets/bedtimeban/lang/`.
Add a new locale by copying `en_us.json` to `<locale>.json` and translating the values.
The server renders strings from these files using each player's reported Minecraft locale, with `en_us` as the fallback.
