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

import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.Locale;

@RequiredArgsConstructor
public class SetTimezoneCommand {

    public static final String COMMAND = "setmytimezone";

    private final BanScheduler banScheduler;

    ArgumentBuilder<CommandSourceStack, ?> register()
    {
        return Commands.literal(COMMAND)
                .then(Commands.argument("tz", StringArgumentType.string()) // TimezoneArgument.timezone()
                        .executes(ctx -> {
                            // set the timezone
                            ZoneId userEnteredTimezone = ctx.getArgument("tz", ZoneId.class);
                            ServerPlayer player = ctx.getSource().getPlayerOrException();

                            if (banScheduler.hasScheduledBan(player.getUUID())) {
                                ctx.getSource().sendSystemMessage(Component.literal("You already have a ban scheduled! Cannot change timezone."));
                                return 0;
                            }

                            banScheduler.setTimezone(player.getUUID(), new PlayerTimezone(player.getUUID(), userEnteredTimezone));

                            ctx.getSource().sendSystemMessage(Component.literal(String.format("Updated your timezone to %s (%s).", userEnteredTimezone.getDisplayName(TextStyle.FULL, Locale.getDefault()), userEnteredTimezone.getId())));

                            return 1;
                        }));
    }
}
