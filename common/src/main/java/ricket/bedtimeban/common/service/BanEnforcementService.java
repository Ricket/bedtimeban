package ricket.bedtimeban.common.service;

import ricket.bedtimeban.core.model.BanWarningThreshold;
import ricket.bedtimeban.core.model.ScheduledBanRecord;
import ricket.bedtimeban.core.service.BedtimeDomainService;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public final class BanEnforcementService {
    private static final Duration CHECK_INTERVAL = Duration.ofMinutes(1);

    private final BedtimeRepository repository;
    private final BedtimeDomainService domainService;
    private final BedtimeMessagingService messagingService;
    private final Clock clock;
    private Instant lastRun;

    public BanEnforcementService(BedtimeRepository repository, BedtimeDomainService domainService, BedtimeMessagingService messagingService, Clock clock) {
        this.repository = repository;
        this.domainService = domainService;
        this.messagingService = messagingService;
        this.clock = clock;
    }

    public void tick(BedtimeServerAccess serverAccess, Consumer<String> logger) {
        Instant now = Instant.now(clock);
        if (lastRun != null && now.isBefore(lastRun.plus(CHECK_INTERVAL))) {
            return;
        }
        lastRun = now;

        for (Map.Entry<UUID, ScheduledBanRecord> entry : repository.getScheduledBans().entrySet()) {
            UUID playerUuid = entry.getKey();
            String playerName = serverAccess.uuidToDisplayName(playerUuid);
            try {
                processEntry(serverAccess, now, entry.getValue());
            } catch (Exception e) {
                logger.accept("Error processing scheduled ban data for " + playerName + ": " + e.getMessage());
            }
        }
    }

    private void processEntry(BedtimeServerAccess serverAccess, Instant now, ScheduledBanRecord record) throws IOException {
        if (record.end() != null && now.isAfter(record.end())) {
            serverAccess.unban(record.playerUuid());
            repository.removeScheduledBan(record.playerUuid());
            return;
        }

        if (record.start() != null && now.isAfter(record.start())) {
            String currentLocale = serverAccess.getPlayerLocale(record.playerUuid()).orElse(null);
            if (serverAccess.ban(
                record.playerUuid(),
                record.start(),
                record.end(),
                messagingService.banReason(record.playerUuid(), currentLocale),
                messagingService.disconnectMessage(record.playerUuid(), currentLocale)
            )) {
                repository.putScheduledBan(record.withStart(null));
            }
            return;
        }

        Optional<BedtimeDomainService.PendingWarning> nextWarning = domainService.nextWarningThreshold(record, now);
        if (record.start() == null || nextWarning.isEmpty()) {
            return;
        }

        BedtimeDomainService.PendingWarning pendingWarning = nextWarning.get();
        BanWarningThreshold warning = pendingWarning.threshold();
        Instant warningInstant = record.start().minus(warning.toDuration());
        if (now.isAfter(warningInstant)) {
            if (serverAccess.isPlayerOnline(record.playerUuid())) {
                String currentLocale = serverAccess.getPlayerLocale(record.playerUuid()).orElse(null);
                serverAccess.sendSystemMessage(record.playerUuid(), messagingService.warningMessage(record.playerUuid(), currentLocale, warning));
            }
            repository.putScheduledBan(record.withWarningsSent(pendingWarning.warningsSentAfterProcessing()));
        }
    }
}
