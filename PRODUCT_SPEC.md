# Bedtime Ban Product Specification

## Overview

Bedtime Ban is a server-side-only Minecraft server add-on that helps players stop playing at a self-declared bedtime. Each player sets a timezone once, then declares a bedtime for the current play session. The server warns the player shortly before that time and, when the time arrives, temporarily bans the player for a fixed duration.

The product is intentionally session-based rather than recurring. A player must opt in each time by scheduling a bedtime for that play session. The system is meant to enforce a player-declared commitment, not a permanent server-curated sleep schedule.

This product is meant for multiplayer server environments. It does not provide meaningful value in single-player or local-only use.

## Product Goals

- Let each player define bedtime in their own local timezone.
- Enforce the bedtime on the server without requiring any client-side installation.
- Keep the workflow simple: set timezone once, then set bedtime when starting a play session.
- Make bedtime enforcement predictable through reminder messages before the ban begins.

## Server-Only Behavior

- The add-on runs entirely on the server.
- Players do not need a client-side mod or plugin component.
- All command handling, scheduling, warning delivery, ban creation, unban handling, and persistence occur on the server.
- The product should not rely on any client-reported timezone data. Timezone is set manually by the player through a command and then stored by the server.

## Core Data Model

The product persists two per-player record types.

### Player Timezone Record

Fields:

- `playerUuid`: unique player identifier
- `zoneId`: canonical timezone identifier supported by the server runtime

Behavior:

- At most one timezone record exists per player UUID.
- A timezone persists until the player changes it.
- A timezone can only be changed when the player does not currently have a scheduled bedtime/ban record.

### Scheduled Bedtime/Ban Record

Fields:

- `playerUuid`: unique player identifier
- `start`: absolute instant when the ban should begin; later cleared after successful ban application
- `end`: absolute instant when the ban should end
- `reason`: human-readable reason string; default value is `Bedtime`
- `warningsSent`: integer counter tracking how many reminder thresholds have already been consumed

Behavior:

- At most one scheduled record exists per player UUID.
- A scheduled record is created when a player successfully runs the bedtime scheduling command.
- The default ban duration is exactly 8 hours from the scheduled bedtime instant.
- After the bedtime ban is successfully applied, the record remains but its `start` field is cleared so enforcement is not applied twice.
- After the ban end time passes, the player is unbanned and the scheduled record is removed.

## Command Surface

All product functionality is exposed through the root slash command `/bedtime`.

### `/bedtime`

Purpose:

- Display the player’s currently scheduled bedtime reminder, if one exists.

Who can run it:

- Any player.

Success behavior:

- If the player has a scheduled bedtime and a stored timezone, the server sends:
  - `Reminder that your bedtime is: <formatted local time>`
- The formatted local time uses the player’s stored timezone and includes an am/pm time plus timezone abbreviation, such as `11:30PM CDT` or equivalent runtime-generated formatting.

Failure / empty-state behavior:

- If the player has no scheduled bedtime, the server sends:
  - `You haven't set a bedtime yet.`

Notes:

- This command is player-centric. It is not defined as a general admin inspection command.

### `/bedtime timezone`

Purpose:

- Show the player’s currently stored timezone.

Who can run it:

- Any player.

Success behavior:

- If a timezone exists, the server sends:
  - `Your timezone is: <display name> (<zone id>)`

Failure / empty-state behavior:

- If no timezone exists, the server sends:
  - `You haven't set a timezone yet.`

### `/bedtime timezone <tz>`

Purpose:

- Store or replace the player’s timezone.

Who can run it:

- Any player.

Accepted input:

- Any timezone string accepted by the server runtime’s timezone database.
- Examples:
  - `America/Chicago`
  - `US/Pacific`
  - `EDT`

Preconditions:

- The player must not currently have a scheduled bedtime/ban record.

Success behavior:

- The timezone record is created or replaced for that player UUID.
- The server sends:
  - `Updated your timezone to <display name> (<zone id>).`

Failure behavior:

- If the player already has a scheduled bedtime/ban record:
  - `You already have a ban scheduled! Cannot change timezone.`
- If the timezone string is invalid:
  - `No such timezone. Use a standard timezone ID like "US/Pacific" or "America/Chicago" or "EDT".`

### `/bedtime set <time>`

Purpose:

- Schedule a one-time bedtime for the current play session.

Who can run it:

- Any player.

Accepted time formats:

- 12-hour time only.
- Case-insensitive.
- Leading and trailing whitespace ignored.
- No internal spaces.
- Accepted forms:
  - `11pm`
  - `9am`
  - `11:30pm`
  - `09:05am`

Formally accepted patterns:

- `h(am|pm)`
- `hh(am|pm)`
- `h:mm(am|pm)`
- `hh:mm(am|pm)`

Preconditions:

- The player must already have a stored timezone.
- The player must not already have a scheduled bedtime/ban record.

Success behavior:

- The player’s local bedtime is converted to an absolute scheduled instant using the stored timezone.
- A scheduled bedtime/ban record is created.
- The end time is set to exactly 8 hours after the start time.
- The server sends:
  - `Ok, you will be banned at <yyyy-MM-dd hh:mma z>.`

Failure behavior:

- If the time string is invalid:
  - `The time argument should be a 12-hr time with am or pm after it. Example: 11:30pm`
- If the player has not configured a timezone:
  - `You have not configured your timezone yet. Use \`/bedtime timezone\` to set your timezone first.`
- If the player already has a bedtime scheduled:
  - `You already have a bedtime set.`

### `/bedtime cancel`

Purpose:

- Cancel the caller’s scheduled bedtime/ban and remove any active bedtime ban for that same player.

Who can run it:

- Admin/operator only.

Important behavior:

- This command is not available for ordinary players, even for self-cancellation.

Success behavior:

- The server attempts to remove any active bedtime ban for the caller.
- The server then removes the caller’s scheduled record if one exists.
- If a scheduled record existed, the server sends:
  - `Your bedtime ban has been cancelled.`

Failure / empty-state behavior:

- If no scheduled record exists:
  - `You do not have a bedtime scheduled.`

### `/bedtime cancel <playername-or-uuid>`

Purpose:

- Cancel another player’s scheduled bedtime/ban and remove any active bedtime ban for that player.

Who can run it:

- Admin/operator only.

Identifier resolution behavior:

The argument is resolved in this order:

1. Parse as UUID.
2. If not a UUID, try to match an online player by name.
3. If still unresolved, try to match a cached player profile by name.

Success behavior:

- The server attempts to remove any active bedtime ban for the target player.
- The server removes the target player’s scheduled record if one exists.
- If a scheduled record existed, the server sends:
  - `Ban has been cancelled for <input>`

Failure behavior:

- If the target cannot be resolved to a UUID:
  - `Could not get UUID for username <input>`
- If the target resolves but has no scheduled record:
  - `There was not a scheduled ban for <input>`

## Scheduling Rules

### Timezone Handling

- Bedtime is interpreted in the player’s stored timezone, not in server local time.
- The scheduled reminder shown to the player is also formatted in the player’s stored timezone.

### Conversion from Local Bedtime to Absolute Time

When a player enters a bedtime, the server constructs a target local date-time in the stored timezone using the current date in that timezone.

The exact scheduling rule is:

1. Take the current time in the player’s timezone.
2. Round that current time down to the start of the current minute and second bucket used by the implementation:
   - minute, second, and sub-second precision are reset to zero before comparison.
3. Replace the hour and minute with the player-entered bedtime.
4. If the resulting date-time is before that rounded-down current local time, move it forward by one day.
5. Convert the resulting local date-time to an absolute instant.

Important quirk to preserve:

- Because the comparison baseline is rounded down to the top of the current hour in practice, a bedtime earlier than the current minute but still within the current hour can resolve to a time in the immediate past instead of tomorrow.
- Example: if it is 10:45 locally and the player sets `10:30pm`, the scheduled start may already be in the past and can therefore trigger almost immediately on the next enforcement pass.

This quirk is part of the current product behavior and should be preserved if the goal is exact end-user equivalence.

## Warning Schedule

Before the bedtime ban starts, the product sends up to three warning messages in this fixed order:

1. 15 minutes before bedtime
2. 5 minutes before bedtime
3. 1 minute before bedtime

User-visible warning text:

- `15 minutes until bedtime!`
- `5 minutes until bedtime!`
- `1 minute until bedtime!`

Warning behavior details:

- Warnings are tracked by the `warningsSent` counter on the scheduled record.
- Warnings are consumed in order and only once each.
- If the player is offline when a warning threshold passes, no message is delivered, but the warning still counts as consumed.
- Missed warnings are not replayed when the player logs in later.

## Enforcement Loop

The server runs a periodic enforcement pass approximately once per minute.

Each pass processes every scheduled bedtime/ban record independently.

### Pre-Ban Phase

If `start` exists and the current instant has not yet reached `start`, the enforcement loop checks whether the next warning threshold has passed.

If so:

- If the player is online, the corresponding warning message is sent.
- The `warningsSent` counter is incremented and persisted.

### Ban Start Phase

If `start` exists and the current instant is after `start`:

- If the player is not already banned, the server creates a temporary ban entry.
- The ban entry uses:
  - created-at time equal to the scheduled start instant
  - source string `BedtimeBan`
  - expiry equal to the scheduled end instant
  - ban-list reason string `Bed time!`
- If the player is currently online, the server disconnects them immediately with:
  - `Good night!`
- After successful ban creation, the scheduled record remains but `start` is cleared to prevent reapplying the ban.

If the player is already banned at that moment:

- No new ban is created.
- The scheduled record remains unchanged by the failed re-ban attempt.

## Ban End Phase

If `end` exists and the current instant is after `end`:

- The player is unbanned.
- The scheduled bedtime/ban record is removed.

This cleanup is automatic and does not require the player to be online.

## Login-Time Behavior

When a player logs into the server:

- If they have a scheduled bedtime and a stored timezone, the server sends:
  - `Reminder that your bedtime is: <formatted local time>`
- If they do not have a scheduled bedtime, nothing special happens on login.

This reminder is informational only. It does not resend missed warning thresholds.

## Persistence Requirements

The implementation must persist server-scoped state across restarts.

Required persisted collections:

- `timezones`: a collection of serialized player timezone records
- `scheduledBans`: a collection of serialized scheduled bedtime/ban records

Persistence requirements:

- Records are keyed logically by player UUID.
- A later write for the same UUID replaces the older record of that type.
- Persistence must survive server restarts.
- Invalid persisted entries should be ignored rather than crashing the product.
- Persisted records may be stored in any implementation-specific format, but they must preserve the fields described in this document.

Ordering requirements:

- No user-visible behavior depends on storage order.
- Deterministic ordering is acceptable but not required as a product behavior.

## Permissions Model

### Ordinary Players

Ordinary players can:

- check their scheduled bedtime with `/bedtime`
- check their stored timezone with `/bedtime timezone`
- set or update timezone with `/bedtime timezone <tz>` when no bedtime is currently scheduled
- schedule a bedtime with `/bedtime set <time>`

Ordinary players cannot:

- cancel their own bedtime
- cancel another player’s bedtime
- modify a scheduled bedtime once it exists

### Admins / Operators

Admins/operators can:

- perform all ordinary player actions available to them as players
- cancel their own scheduled bedtime using `/bedtime cancel`
- cancel another player’s scheduled bedtime using `/bedtime cancel <playername-or-uuid>`

## User-Visible Constraints and Quirks

The following behaviors are part of the current product contract and should be preserved for equivalence:

- Timezone is not auto-detected; the player must set it manually.
- Timezone is remembered until changed.
- Bedtime is one-time only and must be set again for each play session.
- Once a bedtime is scheduled, the player cannot edit it.
- Once a bedtime is scheduled, the player cannot change timezone.
- Self-cancel is admin-only.
- Warning thresholds are fixed at 15, 5, and 1 minute.
- Warning delivery depends on the player being online at the moment the threshold is processed.
- The enforcement loop is periodic rather than exact-to-the-second.
- A bedtime set earlier than the current minute but within the same local hour can effectively schedule into the immediate past and trigger on the next pass.

## Out of Scope

The current product does not implement any of the following:

- recurring schedules by weekday or calendar rule
- custom ban durations
- player self-service cancellation without admin privileges
- post-scheduling bedtime edits
- grace-period correction after a mistaken time entry
- localization beyond the current English command and message behavior
- automatic client-side timezone detection
- client-side UI beyond standard slash command interaction

## Acceptance Criteria for a Compatible Reimplementation

A fresh implementation should be considered compatible only if it preserves the same end-user behavior in these areas:

- server-only deployment model
- per-player stored timezone
- one-time bedtime scheduling workflow
- supported slash commands and argument forms
- player/admin permission split
- exact warning thresholds
- 8-hour default ban window
- login reminder behavior
- immediate disconnect at ban start with `Good night!`
- automatic unban after the scheduled end time
- persistence of timezone and scheduled state across restarts
- current quirks, including the same-hour past-minute scheduling behavior

## Validation Scenarios

The following scenarios should all behave as described:

1. A player with no timezone runs `/bedtime` and is told no bedtime is set.
2. A player with no timezone runs `/bedtime timezone` and is told no timezone is set.
3. A player with no timezone runs `/bedtime set 11:30pm` and is told to configure timezone first.
4. A player sets timezone successfully and later retrieves it successfully.
5. A player with timezone set schedules a bedtime successfully and receives the confirmation with an absolute local timestamp.
6. A player who already has a scheduled bedtime cannot schedule a second one.
7. A player who already has a scheduled bedtime cannot change timezone.
8. A scheduled player who logs in before bedtime receives a reminder of their bedtime.
9. An online player receives warning messages at the 15-minute, 5-minute, and 1-minute thresholds.
10. An offline player misses those warnings and does not receive catch-up messages later.
11. When bedtime arrives, the player is temporarily banned and immediately disconnected if online.
12. After 8 hours elapse, the player is automatically unbanned and the scheduled record is removed.
13. An admin can cancel their own bedtime using `/bedtime cancel`.
14. An admin can cancel another player’s bedtime using `/bedtime cancel <playername>`.
15. An admin can cancel another player’s bedtime using `/bedtime cancel <uuid>`.
16. Cancellation of a nonexistent scheduled bedtime returns the appropriate no-schedule response.
17. A bedtime entered earlier than the current minute but within the same local hour can trigger almost immediately instead of rolling to the next day.

