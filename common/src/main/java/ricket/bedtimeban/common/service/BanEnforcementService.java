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
    private final Clock clock;
    private Instant lastRun;

    public BanEnforcementService(BedtimeRepository repository, BedtimeDomainService domainService, Clock clock) {
        this.repository = repository;
        this.domainService = domainService;
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
            if (serverAccess.ban(record.playerUuid(), record.start(), record.end())) {
                repository.putScheduledBan(record.withStart(null));
            }
            return;
        }

        Optional<BanWarningThreshold> nextWarning = domainService.nextWarningThreshold(record);
        if (record.start() == null || nextWarning.isEmpty()) {
            return;
        }

        BanWarningThreshold warning = nextWarning.get();
        BanWarningThreshold.InstantDelta delta = warning.toDelta();
        Instant warningInstant = record.start().minus(delta.amount(), delta.unit());
        if (now.isAfter(warningInstant)) {
            if (serverAccess.isPlayerOnline(record.playerUuid())) {
                serverAccess.sendSystemMessage(record.playerUuid(), warning.toUserString() + " until bedtime!");
            }
            repository.putScheduledBan(record.withWarningsSent(record.warningsSent() + 1));
        }
    }
}

