package ricket.bedtimeban.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import ricket.bedtimeban.common.service.BedtimeMessagingService;
import ricket.bedtimeban.common.service.BedtimeRepository;
import ricket.bedtimeban.core.model.PlayerTimezoneRecord;
import ricket.bedtimeban.core.model.ScheduledBanRecord;
import ricket.bedtimeban.core.service.BedtimeDomainService;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

public final class BedtimeCommandRegistrar {
    private final BedtimeRepository repository;
    private final BedtimeDomainService domainService;
    private final BedtimeMessagingService messagingService;

    public BedtimeCommandRegistrar(BedtimeRepository repository, BedtimeDomainService domainService, BedtimeMessagingService messagingService) {
        this.repository = repository;
        this.domainService = domainService;
        this.messagingService = messagingService;
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
            String locale = player.getLanguage();
            Optional<String> reminder = messagingService.makeReminderMessage(player.getUUID(), locale);
            source.sendSystemMessage(Component.literal(reminder.orElseGet(() -> messagingService.render(locale, "bedtimeban.command.root.none"))));
            return 0;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to execute /bedtime", e);
        }
    }

    private int executeTimezoneGet(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            String locale = player.getLanguage();
            PlayerTimezoneRecord timezone = repository.getTimezone(player.getUUID());
            if (timezone == null) {
                source.sendSystemMessage(Component.literal(messagingService.render(locale, "bedtimeban.command.timezone.get.none")));
            } else {
                source.sendSystemMessage(Component.literal(
                    messagingService.render(
                        locale,
                        "bedtimeban.command.timezone.get.success",
                        messagingService.formatTimezoneDisplay(timezone.zoneId(), locale)
                    )
                ));
            }
            return 0;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to execute /bedtime timezone", e);
        }
    }

    private int executeTimezoneSet(CommandSourceStack source, String userInputTimezone) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            String locale = player.getLanguage();
            if (repository.hasScheduledBan(player.getUUID())) {
                source.sendSystemMessage(Component.literal(messagingService.render(locale, "bedtimeban.command.timezone.set.blocked")));
                return 1;
            }

            Optional<ZoneId> timezone = domainService.parseZoneId(userInputTimezone);
            if (timezone.isEmpty()) {
                source.sendSystemMessage(Component.literal(messagingService.render(locale, "bedtimeban.command.timezone.set.invalid")));
                return 1;
            }

            repository.putTimezone(new PlayerTimezoneRecord(player.getUUID(), timezone.get()));
            source.sendSystemMessage(Component.literal(
                messagingService.render(
                    locale,
                    "bedtimeban.command.timezone.set.success",
                    messagingService.formatTimezoneDisplay(timezone.get(), locale)
                )
            ));
            return 0;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to execute /bedtime timezone <tz>", e);
        }
    }

    private int executeSet(CommandSourceStack source, String userInputTime) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            String locale = player.getLanguage();
            Optional<LocalTime> bedtime = domainService.parseBedtime(userInputTime);
            if (bedtime.isEmpty()) {
                source.sendSystemMessage(Component.literal(messagingService.render(locale, "bedtimeban.command.set.invalid_time")));
                return 1;
            }

            PlayerTimezoneRecord timezone = repository.getTimezone(player.getUUID());
            if (timezone == null) {
                source.sendSystemMessage(Component.literal(messagingService.render(locale, "bedtimeban.command.set.no_timezone")));
                return 1;
            }

            ScheduledBanRecord existing = repository.getScheduledBan(player.getUUID());
            BedtimeDomainService.ScheduleBedtimeResult result =
                domainService.scheduleOrUpdateBan(player.getUUID(), timezone.zoneId(), bedtime.get(), existing);

            if (result.status() == BedtimeDomainService.ScheduleBedtimeStatus.REJECTED) {
                source.sendSystemMessage(Component.literal(messagingService.render(locale, "bedtimeban.command.set.already_set")));
                return 1;
            }

            if (result.status() == BedtimeDomainService.ScheduleBedtimeStatus.UNCHANGED) {
                source.sendSystemMessage(Component.literal(messagingService.renderSetSameTime(result.scheduled(), locale)));
                return 0;
            }

            repository.putScheduledBan(result.record());
            source.sendSystemMessage(Component.literal(messagingService.renderSetSuccess(result.scheduled(), locale)));
            return 0;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to execute /bedtime set <time>", e);
        }
    }

    private int executeSelfCancel(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            String locale = player.getLanguage();
            ServerBanAccess access = new ServerBanAccess(source.getServer());
            access.unban(player.getUUID());
            if (repository.removeScheduledBan(player.getUUID())) {
                source.sendSystemMessage(Component.literal(messagingService.render(locale, "bedtimeban.command.cancel.self.success")));
            } else {
                source.sendSystemMessage(Component.literal(messagingService.render(locale, "bedtimeban.command.cancel.self.none")));
            }
            return 0;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to execute /bedtime cancel", e);
        }
    }

    private int executeTargetCancel(CommandSourceStack source, String userInputTarget) {
        try {
            String locale = sourceLocale(source);
            ServerBanAccess access = new ServerBanAccess(source.getServer());
            Optional<java.util.UUID> uuid = access.resolvePlayerUuid(userInputTarget);
            if (uuid.isEmpty()) {
                source.sendSystemMessage(Component.literal(messagingService.render(locale, "bedtimeban.command.cancel.target.missing_uuid", userInputTarget)));
                return 1;
            }

            access.unban(uuid.get());
            if (repository.removeScheduledBan(uuid.get())) {
                source.sendSystemMessage(Component.literal(messagingService.render(locale, "bedtimeban.command.cancel.target.success", userInputTarget)));
            } else {
                source.sendSystemMessage(Component.literal(messagingService.render(locale, "bedtimeban.command.cancel.target.none", userInputTarget)));
            }
            return 0;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to execute /bedtime cancel <player>", e);
        }
    }

    private String sourceLocale(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer player ? player.getLanguage() : null;
    }
}
