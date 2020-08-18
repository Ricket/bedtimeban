package ricket.bedtimeban;

import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerTimeZones {
    private final Map<UUID, ZoneId> timezones = new ConcurrentHashMap<>();

    public void setTimeZone(UUID player, ZoneId timezone) {
        timezones.put(player, timezone);
    }

    public void clearPlayer(UUID player) {
        timezones.remove(player);
    }

    public ZoneId getTimezone(UUID player) {
        return timezones.get(player);
    }
}
