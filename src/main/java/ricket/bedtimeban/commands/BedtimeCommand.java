package ricket.bedtimeban.commands;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.builder.ArgumentBuilder;
import lombok.RequiredArgsConstructor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import ricket.bedtimeban.BanScheduler;
import ricket.bedtimeban.BedtimeBanConfig;
import ricket.bedtimeban.ScheduledBan;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@RequiredArgsConstructor
public class BedtimeCommand {
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mma z");

    private final BedtimeBanConfig config;
    private final BanScheduler banScheduler;

    private final Clock clock = Clock.systemUTC();

    ArgumentBuilder<CommandSourceStack, ?> register()
    {
        return Commands.literal(config.getCommandBedtime())
                .then(Commands.argument("time", new ClockTimeArgument())
                        .executes(ctx -> {
                            LocalTime userEnteredTime = ctx.getArgument("time", LocalTime.class);
                            ServerPlayer player = ctx.getSource().getPlayerOrException();

                            ScheduledBan scheduledBan = banScheduler.getScheduledBan(player.getUUID());
                            if (scheduledBan != null) {
                                player.sendSystemMessage(Component.literal("You already have a bedtime set."));
                                return 1;
                            }

                            ZoneId timezone = banScheduler.getTimezone(player.getUUID());
                            if (timezone == null) {
                                player.sendSystemMessage(Component.literal(String.format("You have not configured your timezone yet. Use /%s to set your timezone first.", config.getCommandSetTimezone())));
                                return 1;
                            }

                            ZonedDateTime dateTime = makeZonedDateTime(userEnteredTime, timezone);

                            Instant startTime = dateTime.toInstant();
                            Instant endTime = startTime.plus(8, ChronoUnit.HOURS);
                            banScheduler.scheduleBan(player.getUUID(), startTime, endTime);

                            player.sendSystemMessage(Component.literal(String.format("Ok, you will be banned at %s.", dateTime.format(DATETIME_FORMATTER))));

                            return 1;
                        }))
                .executes(ctx -> {
                    // if it's run without arguments, and a bedtime is set, remind them of their bedtime.
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    String banReminderString = banScheduler.makeBanReminderString(player.getUUID());
                    if (banReminderString != null) {
                        player.sendSystemMessage(Component.literal(banReminderString));
                    }
                    return 0;
                });
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
