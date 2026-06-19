package ricket.bedtimeban.core.model;

import java.time.Instant;
import java.util.UUID;

public record ScheduledBanRecord(UUID playerUuid, Instant start, Instant end, String reason, int warningsSent) {
    public ScheduledBanRecord withStart(Instant newStart) {
        return new ScheduledBanRecord(playerUuid, newStart, end, reason, warningsSent);
    }

    public ScheduledBanRecord withWarningsSent(int newWarningsSent) {
        return new ScheduledBanRecord(playerUuid, start, end, reason, newWarningsSent);
    }
}

