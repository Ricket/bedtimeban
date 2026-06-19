package ricket.bedtimeban.core.persistence;

import ricket.bedtimeban.core.model.PlayerTimezoneRecord;
import ricket.bedtimeban.core.model.ScheduledBanRecord;

import java.util.Map;
import java.util.UUID;

public record BedtimeState(Map<UUID, PlayerTimezoneRecord> timezones, Map<UUID, ScheduledBanRecord> scheduledBans) {
    public BedtimeState {
        timezones = Map.copyOf(timezones);
        scheduledBans = Map.copyOf(scheduledBans);
    }

    public static BedtimeState empty() {
        return new BedtimeState(Map.of(), Map.of());
    }
}

