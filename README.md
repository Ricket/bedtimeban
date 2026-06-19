# Bedtime Ban

Bedtime Ban is a server-side Minecraft mod that lets each player set a one-time bedtime in their own timezone. Before the deadline, the server sends reminder messages. When the deadline arrives, the player is temporarily banned for 8 hours.

This branch is a fresh multiversion workspace. The current implemented target is:

- Minecraft `1.21.1`
- Loader `NeoForge`
- Java `21`

## Workspace Layout

```text
bedtimeban/
  core/         Pure Java domain logic and persistence codec
  common/       Shared server logic for the active Minecraft version
  neoforge/     NeoForge bootstrap, commands, and server integration
  buildSrc/     Shared Gradle conventions
  versionProperties/
```

The build selects one Minecraft version at a time with `-PmcVer=...`. Right now the only configured target is `1.21.1`.

## Build

From the repository root:

```bash
./gradlew -PmcVer=1.21.1 test
./gradlew -PmcVer=1.21.1 build
```

Run a development server:

```bash
./gradlew -PmcVer=1.21.1 :neoforge:runServer
```

Run a development client:

```bash
./gradlew -PmcVer=1.21.1 :neoforge:runClient
```

## Current Behavior

- `/bedtime`
  Shows the caller's scheduled bedtime reminder, if one exists.
- `/bedtime timezone`
  Shows the caller's saved timezone.
- `/bedtime timezone <tz>`
  Saves the caller's timezone if they do not already have a scheduled bedtime.
- `/bedtime set <time>`
  Schedules a one-time bedtime using 12-hour input such as `11pm` or `11:30pm`.
- `/bedtime cancel`
  Admin-only self-cancel.
- `/bedtime cancel <playername-or-uuid>`
  Admin-only cancel for another player.

State is persisted in a JSON file under the server's `serverconfig` directory as `bedtimeban-state.json`.

## Notes

- This mod is intended to be server-side only.
- The current implementation follows the behavior described in [`PRODUCT_SPEC.md`](./PRODUCT_SPEC.md).
- The scheduling quirk from the older versions is intentionally preserved: a bedtime earlier than the current minute but within the same local hour can resolve into the immediate past and trigger on the next enforcement pass.
