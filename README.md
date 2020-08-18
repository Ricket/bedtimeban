Bedtime Ban mod
===
For Minecraft 1.12

Required on both client and server side. (Doesn't do anything in single player)

Usage
---

`/bedtime 11:30pm`
or
`/bedtime 11pm`

Once scheduled, it can't be cancelled except from a server command, so don't mess up. It's a one-time thing so you have to do it each day. It will give you 15-, 5- and 1-minute warnings, and then you'll be banned for 8 hours.

The time is in the player's local time (timezone is transmitted upon login) so you don't need to worry about server location.

Server-only cancel command: `/cancelbedtime [playername or uuid]`

e.g. `/cancelbedtime Ricket`

TODO
---

* string localization
* ban scheduling ("ban me Sun-Thurs at 11pm")
* custom ban durations
* grace period to unban if the ban time was wrong (e.g. "ban me at 11am" "oh shoot I meant 11pm")
