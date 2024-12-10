package ricket.bedtimeban.commands;

import com.mojang.brigadier.builder.ArgumentBuilder;
import lombok.RequiredArgsConstructor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import ricket.bedtimeban.BanScheduler;
import ricket.bedtimeban.MinecraftServerBanUtils;

@RequiredArgsConstructor
public class CancelBanCommand {
    private static final String COMMAND = "cancelbedtime";

    private final BanScheduler banScheduler;

    ArgumentBuilder<CommandSourceStack, ?> register()
    {
        return Commands.literal(COMMAND)
                .requires(cs -> cs.hasPermission(4))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                            MinecraftServerBanUtils.unban(player.getUUID(), ctx.getSource().getServer());
                            boolean removed = banScheduler.clearScheduledBan(player.getUUID());
                            if (removed) {
                                ctx.getSource().sendSystemMessage(Component.literal("Ban has been cancelled."));
                            } else {
                                ctx.getSource().sendSystemMessage(Component.literal("There was not a scheduled ban."));
                            }
                            return 1;
                        }));
    }
}
