package ricket.bedtimeban.commands;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import lombok.RequiredArgsConstructor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import ricket.bedtimeban.BanScheduler;
import ricket.bedtimeban.PlayerTimezone;
import ricket.bedtimeban.ScheduledBan;

import javax.annotation.CheckForNull;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class SetBedtimeCommand {
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mma z");
    private final Pattern hourAmpmPattern = Pattern.compile("^(1[0-2]|0?[1-9])(am|pm)$");
    private final Pattern hourMinuteAmpmPattern = Pattern.compile("^(1[0-2]|0?[1-9]):([0-5][0-9])(am|pm)$");
    private static final String COMMAND = "set";

    private final BanScheduler banScheduler;

    private final Clock clock = Clock.systemUTC();

    ArgumentBuilder<CommandSourceStack, ?> register()
    {
        return Commands.literal(COMMAND)
                .then(Commands.argument("time", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();

                            String userEnteredTimeStr = ctx.getArgument("time", String.class);
                            LocalTime userEnteredTime = parse(userEnteredTimeStr);
                            if (userEnteredTime == null) {
                                ctx.getSource().sendSystemMessage(Component.literal("The time argument should be a 12-hr time with am or pm after it. Example: 11:30pm"));
                                return 1;
                            }

                            PlayerTimezone timezone = banScheduler.getTimezone(player.getUUID());
                            if (timezone == null) {
                                ctx.getSource().sendSystemMessage(Component.literal(String.format("You have not configured your timezone yet. Use `/bedtime %s` to set your timezone first.", SetTimezoneCommand.COMMAND)));
                                return 1;
                            }

                            ScheduledBan scheduledBan = banScheduler.getScheduledBan(player.getUUID());
                            if (scheduledBan != null) {
                                ctx.getSource().sendSystemMessage(Component.literal("You already have a bedtime set."));
                                return 1;
                            }

                            ZonedDateTime dateTime = makeZonedDateTime(userEnteredTime, timezone.getZoneId());

                            Instant startTime = dateTime.toInstant();
                            Instant endTime = startTime.plus(8, ChronoUnit.HOURS);
                            banScheduler.scheduleBan(player.getUUID(), startTime, endTime);

                            ctx.getSource().sendSystemMessage(Component.literal(String.format("Ok, you will be banned at %s.", dateTime.format(DATETIME_FORMATTER))));

                            return 0;
                        }));
    }

    private LocalTime parse(String userInputTime) {
        String str = userInputTime.trim().toLowerCase();

        LocalTime hourAmpm = tryParseHourAmpm(str);
        if (hourAmpm != null) {
            return hourAmpm;
        }

        LocalTime hourMinuteAmpm = tryParseHourMinuteAmpm(str);
        if (hourMinuteAmpm != null) {
            return hourMinuteAmpm;
        }

        // TODO other formats

        return null;
    }

    @CheckForNull
    private LocalTime tryParseHourAmpm(String str) {
        Matcher matcher = hourAmpmPattern.matcher(str);
        if (!matcher.matches()) {
            return null;
        }

        int hourNum = Integer.parseInt(matcher.group(1), 10);
        boolean pm = matcher.group(2).equals("pm");

        return LocalTime.of(toHour24(hourNum, pm), 0);
    }

    @CheckForNull
    private LocalTime tryParseHourMinuteAmpm(String str) {
        Matcher matcher = hourMinuteAmpmPattern.matcher(str);
        if (!matcher.matches()) {
            return null;
        }

        int hourNum = Integer.parseInt(matcher.group(1), 10);
        int minuteNum = Integer.parseInt(matcher.group(2), 10);
        boolean pm = matcher.group(3).equals("pm");

        return LocalTime.of(toHour24(hourNum, pm), minuteNum);
    }

    private int toHour24(int hour, boolean pm) {
        if (hour == 12) {
            return pm ? 12 : 0;
        } else {
            return hour + (pm ? 12 : 0);
        }
    }

    private ZonedDateTime makeZonedDateTime(LocalTime time, ZoneId timezone) {
        ZonedDateTime nowRoundedDown = Instant.now(clock)
                .atZone(timezone)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        ZonedDateTime updatedTime = nowRoundedDown
                .withHour(time.getHour())
                .withMinute(time.getMinute());
        if (updatedTime.isBefore(nowRoundedDown)) {
            updatedTime = updatedTime.plusDays(1);
            Preconditions.checkState(!updatedTime.isBefore(nowRoundedDown));
        }
        return updatedTime;
    }
}
