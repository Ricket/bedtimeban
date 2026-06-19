package ricket.bedtimeban.core.model;

import java.time.ZoneId;
import java.util.UUID;

public record PlayerTimezoneRecord(UUID playerUuid, ZoneId zoneId) {
}

