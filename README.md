Bedtime Ban mod
===
For Minecraft 1.20. [Download it here.](https://github.com/Ricket/bedtimeban/releases/latest) There is an older version for 1.12 with slightly different commands but it's functional if you want it.

Required only on server side. (Doesn't do anything in single player)

Usage
---

First set your timezone using the setmytimezone command, it's pretty flexible with the timezone format:

`/bedtime timezone America/Chicago`
or
`/bedtime timezone US/Pacific`
or
`/bedtime timezone EDT`

You only have to do this once, the timezone will be remembered for your user (of course, you can run the command again to change it).

Then, declare your bedtime for today's play session:

`/bedtime set 11:30pm`
or
`/bedtime set 11pm`

It's a one-time thing; you have to do it each day that you play. I like it this way because I feel in control, I am declaring my good intentions and not just obeying a scheduled punishment.

Once a bedtime is scheduled, it can't be changed or cancelled except from a server command, so don't mess up. It will give you 15-, 5- and 1-minute warnings, and then you'll be banned for 8 hours.

The time is in the player's local time (timezone is transmitted upon login) so you don't need to worry about server location.

Server-only cancel command: `/bedtime cancel [playername or uuid]`

e.g. `/bedtime cancel Ricket`

TODO
---

* string localization
* ban scheduling ("ban me Sun-Thurs at 11pm")
* custom ban durations
* grace period to unban if the ban time was wrong (e.g. "ban me at 11am" "oh shoot I meant 11pm")


Dev notes
---

* Make sure JAVA_HOME and PATH point to the right JDK, otherwise you may get weird red herring errors about unable to find mapping.
* reobfJar is the main gradle target.
