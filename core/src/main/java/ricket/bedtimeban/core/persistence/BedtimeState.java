package ricket.bedtimeban.core.persistence;

import ricket.bedtimeban.core.model.PlayerTimezoneRecord;
import ricket.bedtimeban.core.model.PlayerLocaleRecord;
import ricket.bedtimeban.core.model.ScheduledBanRecord;

import java.util.Map;
import java.util.UUID;

public record BedtimeState(Map<UUID, PlayerTimezoneRecord> timezones, Map<UUID, PlayerLocaleRecord> locales, Map<UUID, ScheduledBanRecord> scheduledBans) {
    public BedtimeState {
        timezones = Map.copyOf(timezones);
        locales = Map.copyOf(locales);
        scheduledBans = Map.copyOf(scheduledBans);
    }

    public static BedtimeState empty() {
        return new BedtimeState(Map.of(), Map.of(), Map.of());
    }
}
