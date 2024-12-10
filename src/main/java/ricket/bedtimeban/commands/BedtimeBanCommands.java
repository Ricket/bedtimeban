package ricket.bedtimeban.commands;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import ricket.bedtimeban.BanScheduler;

public class BedtimeBanCommands {

    private final BanScheduler banScheduler;
    private final SetBedtimeCommand setBedtimeCommand;
    private final CancelBanCommand cancelBanCommand;
    private final SetTimezoneCommand setTimezoneCommand;

    public BedtimeBanCommands(BanScheduler banScheduler)
    {
        Preconditions.checkNotNull(banScheduler, "banScheduler");
        this.banScheduler = banScheduler;

        setBedtimeCommand = new SetBedtimeCommand(banScheduler);
        cancelBanCommand = new CancelBanCommand(banScheduler);
        setTimezoneCommand = new SetTimezoneCommand(banScheduler);
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(
                LiteralArgumentBuilder.<CommandSourceStack>literal("bedtime")
                        .executes(ctx -> {
                            // if it's run without arguments, and a bedtime is set, remind them of their bedtime.
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String banReminderString = banScheduler.makeBanReminderString(player.getUUID());
                            if (banReminderString != null) {
                                player.sendSystemMessage(Component.literal(banReminderString));
                            } else {
                                player.sendSystemMessage(Component.literal("You haven't set a bedtime yet."));
                            }
                            return 0;
                        })
                        .then(setBedtimeCommand.register())
                        .then(cancelBanCommand.register())
                        .then(setTimezoneCommand.register())
        );
    }
}
