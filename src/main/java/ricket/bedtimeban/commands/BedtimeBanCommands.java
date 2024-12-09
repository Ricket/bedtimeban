package ricket.bedtimeban.commands;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import ricket.bedtimeban.BanScheduler;
import ricket.bedtimeban.BedtimeBanConfig;
import ricket.bedtimeban.MinecraftServerBanUtils;

public class BedtimeBanCommands {

    private final BedtimeCommand bedtimeCommand;
    private final CancelBanCommand cancelBanCommand;
    private final SetTimezoneCommand setTimezoneCommand;

    public BedtimeBanCommands(BedtimeBanConfig config, BanScheduler banScheduler, MinecraftServerBanUtils banUtils)
    {
        Preconditions.checkNotNull(config, "config");
        Preconditions.checkNotNull(banScheduler, "banScheduler");
        Preconditions.checkNotNull(banUtils, "banUtils");

        bedtimeCommand = new BedtimeCommand(config, banScheduler);
        cancelBanCommand = new CancelBanCommand(config, banScheduler, banUtils);
        setTimezoneCommand = new SetTimezoneCommand(config, banScheduler);
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(
                LiteralArgumentBuilder.<CommandSourceStack>literal("bedtime")
                        .then(bedtimeCommand.register())
                        .then(cancelBanCommand.register())
                        .then(setTimezoneCommand.register())
        );
    }
}
