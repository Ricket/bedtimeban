package ricket.bedtimeban.commands;

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
import ricket.bedtimeban.PlayerTimeZones;
import ricket.bedtimeban.ScheduledBan;
import ricket.bedtimeban.TimeParser;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class BedtimeCommand implements ICommand {

    private final TimeParser timeParser;
    private final BanScheduler banScheduler;
    private final PlayerTimeZones playerTimeZones;

    @Override
    public String getName() {
        return BedtimeBanConfig.commandBedtime;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/" + getName() + " 11:30pm";
    }

    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayerMP)) {
            sender.sendMessage(new TextComponentString("Must run command from a player session"));
            return;
        }

        ScheduledBan scheduledBan = banScheduler.getScheduledBan(((EntityPlayerMP) sender).getUniqueID());
        if (scheduledBan != null) {
            sender.sendMessage(new TextComponentString("You already have a bedtime set"));
            return;
        }

        if (args.length != 1) {
            sender.sendMessage(new TextComponentString("Usage:\n" + getUsage(sender)));
            return;
        }
        String userEnteredTime = args[0];

        ZoneId timezone = playerTimeZones.getTimezone(((EntityPlayerMP) sender).getUniqueID());
        if (timezone == null) {
            BedtimeBanMod.logger.warn("Could not find timezone for player " + ((EntityPlayerMP) sender).getDisplayNameString());
            sender.sendMessage(new TextComponentString("Server has not received timezone info yet, please retry in a minute"));
            return;
        }

        ZonedDateTime dateTime = timeParser.parseUserInput(userEnteredTime, timezone);
        if (dateTime == null) {
            sender.sendMessage(new TextComponentString("Failed to parse '" + userEnteredTime + "'"));
            return;
        }
        Instant startTime = dateTime.toInstant();
        Instant endTime = startTime.plus(8, ChronoUnit.HOURS);
        banScheduler.scheduleBan(((EntityPlayerMP) sender).getUniqueID(), startTime, endTime);
        sender.sendMessage(new TextComponentString("Ok, you will be banned at " + dateTime.toString()));
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        // anyone can use it
        return true;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        return Collections.emptyList();
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
