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

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@RequiredArgsConstructor
public class SetBedtimeCommand {
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mma z");
    private static final String COMMAND = "set";

    private final BanScheduler banScheduler;

    private final Clock clock = Clock.systemUTC();

    ArgumentBuilder<CommandSourceStack, ?> register()
    {
        return Commands.literal(COMMAND)
                .then(Commands.argument("time", StringArgumentType.string()) // ClockTimeArgument.clockTime()
                        .executes(ctx -> {
                            LocalTime userEnteredTime = ctx.getArgument("time", LocalTime.class);
                            ServerPlayer player = ctx.getSource().getPlayerOrException();

                            ScheduledBan scheduledBan = banScheduler.getScheduledBan(player.getUUID());
                            if (scheduledBan != null) {
                                player.sendSystemMessage(Component.literal("You already have a bedtime set."));
                                return 1;
                            }

                            PlayerTimezone timezone = banScheduler.getTimezone(player.getUUID());
                            if (timezone == null) {
                                player.sendSystemMessage(Component.literal(String.format("You have not configured your timezone yet. Use /%s to set your timezone first.", SetTimezoneCommand.COMMAND)));
                                return 1;
                            }

                            ZonedDateTime dateTime = makeZonedDateTime(userEnteredTime, timezone.getZoneId());

                            Instant startTime = dateTime.toInstant();
                            Instant endTime = startTime.plus(8, ChronoUnit.HOURS);
                            banScheduler.scheduleBan(player.getUUID(), startTime, endTime);

                            player.sendSystemMessage(Component.literal(String.format("Ok, you will be banned at %s.", dateTime.format(DATETIME_FORMATTER))));

                            return 1;
                        }));
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
