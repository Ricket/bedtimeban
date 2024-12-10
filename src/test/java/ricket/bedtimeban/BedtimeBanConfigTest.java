package ricket.bedtimeban;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BedtimeBanConfigTest {
    @Test
    void testScheduledBanSerialize() {
        ScheduledBan scheduledBan = new ScheduledBan(UUID.randomUUID(), Instant.now(), Instant.now().plus(8, ChronoUnit.HOURS), "reason", 1);

        String json = BedtimeBanConfig.gson.toJson(scheduledBan);

        ScheduledBan actual = BedtimeBanConfig.gson.fromJson(json, ScheduledBan.class);

        assertEquals(scheduledBan, actual);
    }

    @Test
    void testPlayerTimezoneSerialize() {
        PlayerTimezone timezone = new PlayerTimezone(UUID.randomUUID(), ZoneId.of("US/Central"));

        String json = BedtimeBanConfig.gson.toJson(timezone);

        PlayerTimezone actual = BedtimeBanConfig.gson.fromJson(json, PlayerTimezone.class);

        assertEquals(timezone, actual);
    }
}