package ricket.bedtimeban.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import lombok.RequiredArgsConstructor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import ricket.bedtimeban.BanScheduler;
import ricket.bedtimeban.MinecraftServerBanUtils;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class CancelBanCommand {
    private static final String COMMAND = "cancel";

    private final BanScheduler banScheduler;

    ArgumentBuilder<CommandSourceStack, ?> register()
    {
        return Commands.literal(COMMAND)
                .requires(cs -> cs.hasPermission(4))
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    MinecraftServerBanUtils.unban(player.getUUID(), ctx.getSource().getServer());
                    boolean removed = banScheduler.clearScheduledBan(player.getUUID());
                    if (removed) {
                        ctx.getSource().sendSystemMessage(Component.literal("Your bedtime ban has been cancelled."));
                    } else {
                        ctx.getSource().sendSystemMessage(Component.literal("You do not have a bedtime scheduled."));
                    }
                    return 1;
                })
                .then(Commands.argument("player", StringArgumentType.greedyString())
                        .requires(cs -> cs.hasPermission(4))
                        .executes(ctx -> {
                            String usernameOrUuid = ctx.getArgument("player", String.class);

                            UUID uuid = null;
                            try {
                                uuid = UUID.fromString(usernameOrUuid);
                            } catch (IllegalArgumentException ignored) {
                            }

                            MinecraftServer server = ctx.getSource().getServer();
                            if (uuid == null) {
                                ServerPlayer player = server.getPlayerList().getPlayerByName(usernameOrUuid);
                                if (player != null) {
                                    uuid = player.getUUID();
                                }
                            }

                            if (uuid == null) {
                                Optional<GameProfile> gameProfile = server.getProfileCache().get(usernameOrUuid);
                                if (gameProfile.isPresent()) {
                                    uuid = gameProfile.get().getId();
                                }
                            }

                            if (uuid == null) {
                                ctx.getSource().sendSystemMessage(Component.literal(String.format("Could not get UUID for username %s", usernameOrUuid)));
                                return 1;
                            }

                            MinecraftServerBanUtils.unban(uuid, ctx.getSource().getServer());
                            boolean removed = banScheduler.clearScheduledBan(uuid);
                            if (removed) {
                                ctx.getSource().sendSystemMessage(Component.literal("Ban has been cancelled for ").append(usernameOrUuid));
                            } else {
                                ctx.getSource().sendSystemMessage(Component.literal("There was not a scheduled ban for ").append(usernameOrUuid));
                            }
                            return 0;
                        }));
    }
}
