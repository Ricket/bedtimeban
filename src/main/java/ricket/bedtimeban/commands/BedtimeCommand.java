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
import ricket.bedtimeban.ScheduledBan;
import ricket.bedtimeban.TimeParser;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class BedtimeCommand implements ICommand {
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mma z");

    private final TimeParser timeParser;
    private final BanScheduler banScheduler;

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
            sender.sendMessage(new TextComponentString("Must run command from a multiplayer session."));
            return;
        }

        UUID playerUuid = ((EntityPlayerMP) sender).getUniqueID();

        if (args.length == 0) {
            // if it's run without arguments, and a bedtime is set, remind them of their bedtime.
            String banReminderString = banScheduler.makeBanReminderString(playerUuid);
            if (banReminderString != null) {
                sender.sendMessage(new TextComponentString(banReminderString));
                return;
            }
        }

        ScheduledBan scheduledBan = banScheduler.getScheduledBan(playerUuid);
        if (scheduledBan != null) {
            sender.sendMessage(new TextComponentString("You already have a bedtime set."));
            return;
        }

        if (args.length != 1) {
            sender.sendMessage(new TextComponentString("Usage: " + getUsage(sender)));
            return;
        }

        ZoneId timezone = banScheduler.getTimezone(playerUuid);
        if (timezone == null) {
            sender.sendMessage(new TextComponentString(String.format("You have not configured your timezone yet. Use /%s to set your timezone first.", BedtimeBanConfig.commandSetTimezone)));
            return;
        }

        String userEnteredTime = args[0];

        ZonedDateTime dateTime = timeParser.parseUserInput(userEnteredTime, timezone);
        if (dateTime == null) {
            sender.sendMessage(new TextComponentString(String.format("Failed to parse '%s'", userEnteredTime)));
            return;
        }
        Instant startTime = dateTime.toInstant();
        Instant endTime = startTime.plus(8, ChronoUnit.HOURS);
        banScheduler.scheduleBan(playerUuid, startTime, endTime);

        sender.sendMessage(new TextComponentString(String.format("Ok, you will be banned at %s.", dateTime.format(DATETIME_FORMATTER))));
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
