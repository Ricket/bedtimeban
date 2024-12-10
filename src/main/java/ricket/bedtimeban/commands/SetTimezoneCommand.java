package ricket.bedtimeban.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import lombok.RequiredArgsConstructor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import ricket.bedtimeban.BanScheduler;
import ricket.bedtimeban.PlayerTimezone;

import javax.annotation.CheckForNull;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.Locale;

@RequiredArgsConstructor
public class SetTimezoneCommand {

    public static final String COMMAND = "timezone";

    private final BanScheduler banScheduler;

    ArgumentBuilder<CommandSourceStack, ?> register()
    {
        return Commands.literal(COMMAND)
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();

                    PlayerTimezone timezone = banScheduler.getTimezone(player.getUUID());
                    if (timezone == null) {
                        ctx.getSource().sendSystemMessage(Component.literal("You haven't set a timezone yet."));
                    } else {
                        ctx.getSource().sendSystemMessage(Component.literal(String.format("Your timezone is: %s (%s)", timezone.getZoneId().getDisplayName(TextStyle.FULL, Locale.getDefault()), timezone.getZoneId().getId())));
                    }

                    return 0;
                })
                .then(Commands.argument("tz", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();

                            if (banScheduler.hasScheduledBan(player.getUUID())) {
                                ctx.getSource().sendSystemMessage(Component.literal("You already have a ban scheduled! Cannot change timezone."));
                                return 1;
                            }

                            String userEnteredTimezoneStr = ctx.getArgument("tz", String.class);
                            ZoneId userEnteredTimezone = parse(userEnteredTimezoneStr);
                            if (userEnteredTimezone == null) {
                                ctx.getSource().sendSystemMessage(Component.literal("No such timezone. Use a standard timezone ID like \"US/Pacific\" or \"America/Chicago\" or \"EDT\"."));
                                return 1;
                            }

                            banScheduler.setTimezone(player.getUUID(), new PlayerTimezone(player.getUUID(), userEnteredTimezone));

                            ctx.getSource().sendSystemMessage(Component.literal(String.format("Updated your timezone to %s (%s).", userEnteredTimezone.getDisplayName(TextStyle.FULL, Locale.getDefault()), userEnteredTimezone.getId())));

                            return 0;
                        }));
    }

    @CheckForNull
    private ZoneId parse(String userEnteredTimezone) {
        try {
            return ZoneId.of(userEnteredTimezone);
        } catch (Exception e) {
            return null;
        }
    }
}
