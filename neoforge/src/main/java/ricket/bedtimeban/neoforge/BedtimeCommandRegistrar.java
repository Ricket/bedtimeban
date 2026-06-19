package ricket.bedtimeban.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import ricket.bedtimeban.common.service.BedtimeRepository;
import ricket.bedtimeban.core.model.PlayerTimezoneRecord;
import ricket.bedtimeban.core.model.ScheduledBanRecord;
import ricket.bedtimeban.core.service.BedtimeDomainService;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

public final class BedtimeCommandRegistrar {
    private final BedtimeRepository repository;
    private final BedtimeDomainService domainService;

    public BedtimeCommandRegistrar(BedtimeRepository repository, BedtimeDomainService domainService) {
        this.repository = repository;
        this.domainService = domainService;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bedtime")
            .executes(ctx -> executeRoot(ctx.getSource()))
            .then(Commands.literal("timezone")
                .executes(ctx -> executeTimezoneGet(ctx.getSource()))
                .then(Commands.argument("tz", StringArgumentType.greedyString())
                    .executes(ctx -> executeTimezoneSet(ctx.getSource(), StringArgumentType.getString(ctx, "tz")))))
            .then(Commands.literal("set")
                .then(Commands.argument("time", StringArgumentType.greedyString())
                    .executes(ctx -> executeSet(ctx.getSource(), StringArgumentType.getString(ctx, "time")))))
            .then(Commands.literal("cancel")
                .requires(source -> source.hasPermission(4))
                .executes(ctx -> executeSelfCancel(ctx.getSource()))
                .then(Commands.argument("player", StringArgumentType.greedyString())
                    .executes(ctx -> executeTargetCancel(ctx.getSource(), StringArgumentType.getString(ctx, "player"))))));
    }

    private int executeRoot(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            Optional<String> reminder = domainService.makeReminderString(repository.getScheduledBan(player.getUUID()), repository.getTimezone(player.getUUID()));
            source.sendSystemMessage(Component.literal(reminder.orElse("You haven't set a bedtime yet.")));
            return 0;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to execute /bedtime", e);
        }
    }

    private int executeTimezoneGet(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            PlayerTimezoneRecord timezone = repository.getTimezone(player.getUUID());
            if (timezone == null) {
                source.sendSystemMessage(Component.literal("You haven't set a timezone yet."));
            } else {
                source.sendSystemMessage(Component.literal("Your timezone is: " + domainService.formatTimezoneDisplay(timezone.zoneId())));
            }
            return 0;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to execute /bedtime timezone", e);
        }
    }

    private int executeTimezoneSet(CommandSourceStack source, String userInputTimezone) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            if (repository.hasScheduledBan(player.getUUID())) {
                source.sendSystemMessage(Component.literal("You already have a ban scheduled! Cannot change timezone."));
                return 1;
            }

            Optional<ZoneId> timezone = domainService.parseZoneId(userInputTimezone);
            if (timezone.isEmpty()) {
                source.sendSystemMessage(Component.literal("No such timezone. Use a standard timezone ID like \"US/Pacific\" or \"America/Chicago\" or \"EDT\"."));
                return 1;
            }

            repository.putTimezone(new PlayerTimezoneRecord(player.getUUID(), timezone.get()));
            source.sendSystemMessage(Component.literal("Updated your timezone to " + domainService.formatTimezoneDisplay(timezone.get()) + "."));
            return 0;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to execute /bedtime timezone <tz>", e);
        }
    }

    private int executeSet(CommandSourceStack source, String userInputTime) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            Optional<LocalTime> bedtime = domainService.parseBedtime(userInputTime);
            if (bedtime.isEmpty()) {
                source.sendSystemMessage(Component.literal("The time argument should be a 12-hr time with am or pm after it. Example: 11:30pm"));
                return 1;
            }

            PlayerTimezoneRecord timezone = repository.getTimezone(player.getUUID());
            if (timezone == null) {
                source.sendSystemMessage(Component.literal("You have not configured your timezone yet. Use `/bedtime timezone` to set your timezone first."));
                return 1;
            }

            ScheduledBanRecord existing = repository.getScheduledBan(player.getUUID());
            if (existing != null) {
                source.sendSystemMessage(Component.literal("You already have a bedtime set."));
                return 1;
            }

            ZonedDateTime scheduled = domainService.calculateScheduledDateTime(timezone.zoneId(), bedtime.get());
            repository.putScheduledBan(domainService.scheduleBan(player.getUUID(), timezone.zoneId(), bedtime.get()));
            source.sendSystemMessage(Component.literal("Ok, you will be banned at " + domainService.formatConfirmation(scheduled) + "."));
            return 0;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to execute /bedtime set <time>", e);
        }
    }

    private int executeSelfCancel(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            ServerBanAccess access = new ServerBanAccess(source.getServer());
            access.unban(player.getUUID());
            if (repository.removeScheduledBan(player.getUUID())) {
                source.sendSystemMessage(Component.literal("Your bedtime ban has been cancelled."));
            } else {
                source.sendSystemMessage(Component.literal("You do not have a bedtime scheduled."));
            }
            return 0;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to execute /bedtime cancel", e);
        }
    }

    private int executeTargetCancel(CommandSourceStack source, String userInputTarget) {
        try {
            ServerBanAccess access = new ServerBanAccess(source.getServer());
            Optional<java.util.UUID> uuid = access.resolvePlayerUuid(userInputTarget);
            if (uuid.isEmpty()) {
                source.sendSystemMessage(Component.literal("Could not get UUID for username " + userInputTarget));
                return 1;
            }

            access.unban(uuid.get());
            if (repository.removeScheduledBan(uuid.get())) {
                source.sendSystemMessage(Component.literal("Ban has been cancelled for ").append(userInputTarget));
            } else {
                source.sendSystemMessage(Component.literal("There was not a scheduled ban for ").append(userInputTarget));
            }
            return 0;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to execute /bedtime cancel <player>", e);
        }
    }
}
