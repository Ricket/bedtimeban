package ricket.bedtimeban.commands;

import com.google.common.base.Joiner;
import com.mojang.authlib.GameProfile;
import lombok.RequiredArgsConstructor;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.FMLCommonHandler;
import ricket.bedtimeban.BanScheduler;
import ricket.bedtimeban.BedtimeBanConfig;
import ricket.bedtimeban.MinecraftServerBanUtils;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CancelBanCommand implements ICommand {

    private final MinecraftServerBanUtils banUtils;
    private final BanScheduler banScheduler;

    @Override
    public String getName() {
        return BedtimeBanConfig.commandCancel;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/" + getName() + " [player name or uuid]";
    }

    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            sender.sendMessage(new TextComponentString("Usage: " + getUsage(sender)));
            return;
        }

        String usernameOrUuid = Joiner.on(" ").join(args);

        UUID uuid = null;
        try {
            uuid = UUID.fromString(usernameOrUuid);
        } catch (IllegalArgumentException e) {
        }

        if (uuid == null) {
            EntityPlayerMP player = server.getPlayerList().getPlayerByUsername(usernameOrUuid);
            if (player != null) {
                uuid = player.getUniqueID();
            }
        }

        if (uuid == null) {
            GameProfile gameProfile = server.getPlayerProfileCache().getGameProfileForUsername(usernameOrUuid);
            if (gameProfile != null) {
                uuid = gameProfile.getId();
            }
        }

        if (uuid == null) {
            sender.sendMessage(new TextComponentString("Could not get UUID for username " + usernameOrUuid));
            return;
        }

        banUtils.unban(uuid);
        boolean removed = banScheduler.clearScheduledBan(uuid);
        if (removed) {
            sender.sendMessage(new TextComponentString("Ban has been cancelled for " + usernameOrUuid));
        } else {
            sender.sendMessage(new TextComponentString(usernameOrUuid + " does not have a ban scheduled"));
        }

    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        // op or server console only

        if (sender instanceof EntityPlayer) {
            return FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().canSendCommands(((EntityPlayer) sender).getGameProfile());
        }

        return sender instanceof MinecraftServer;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 0 || args[0].trim().isEmpty()) {
            return Arrays.stream(server.getOnlinePlayerNames())
                    .sorted()
                    .collect(Collectors.toList());
        } else {
            String partial = Joiner.on(" ").join(args).toLowerCase();
            return Arrays.stream(server.getOnlinePlayerNames())
                    .filter(n -> n.toLowerCase().startsWith(partial.toLowerCase()))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    @Override
    public int compareTo(ICommand o) {
        return getName().compareTo(o.getName());
    }
}
