package ricket.bedtimeban.commands;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import ricket.bedtimeban.BanScheduler;
import ricket.bedtimeban.BedtimeBanConfig;
import ricket.bedtimeban.BedtimeBanMod;

import javax.annotation.Nullable;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class SetTimezoneCommand implements ICommand {

    private final BanScheduler banScheduler;

    @Override
    public String getName() {
        return BedtimeBanConfig.commandSetTimezone;
    }

    @Override
    public String getUsage(ICommandSender iCommandSender) {
        return "/" + getName() + " America/Los_Angeles";
    }

    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayerMP)) {
            sender.sendMessage(new TextComponentString("Must run command from a multiplayer session."));
            return;
        }

        UUID playerUuid = ((EntityPlayerMP) sender).getUniqueID();

        if (banScheduler.hasScheduledBan(playerUuid)) {
            sender.sendMessage(new TextComponentString("You already have a ban scheduled! Cannot change timezone."));
            return;
        }

        if (args.length != 1) {
            sender.sendMessage(new TextComponentString("Usage: " + getUsage(sender)));
            return;
        }

        String userEnteredTimezone = args[0];

        ZoneId timeZone;
        try {
            timeZone = ZoneId.of(userEnteredTimezone);
        } catch (Exception e) {
            BedtimeBanMod.logger.debug("Failed to lookup ZoneId {}", userEnteredTimezone, e);
            sender.sendMessage(new TextComponentString("No such timezone. Use a standard timezone ID like \"US/Pacific\" or \"America/Chicago\" or \"EDT\". Start typing and hit tab to see options."));
            return;
        }

        banScheduler.setTimezone(playerUuid, timeZone);

        sender.sendMessage(new TextComponentString(String.format("Updated your timezone to %s (%s).", timeZone.getDisplayName(TextStyle.FULL, Locale.getDefault()), timeZone.getId())));
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        // anyone can use it
        return true;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos blockPos) {
        if (args.length == 1 && !Strings.isNullOrEmpty(args[0])) {
            String prefix = args[0];
            return ZoneId.getAvailableZoneIds().stream()
                    .filter(availableId -> availableId.toLowerCase().startsWith(prefix.toLowerCase()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] strings, int i) {
        return false;
    }

    @Override
    public int compareTo(ICommand o) {
        return getName().compareTo(o.getName());
    }
}
