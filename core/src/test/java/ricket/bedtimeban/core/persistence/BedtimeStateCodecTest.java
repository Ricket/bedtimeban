package ricket.bedtimeban.core.persistence;

import org.junit.jupiter.api.Test;
import ricket.bedtimeban.core.model.PlayerLocaleRecord;
import ricket.bedtimeban.core.model.PlayerTimezoneRecord;
import ricket.bedtimeban.core.model.ScheduledBanRecord;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedtimeStateCodecTest {
    @Test
    void roundTripsState() {
        UUID userId = UUID.randomUUID();
        BedtimeStateCodec codec = new BedtimeStateCodec();
        BedtimeState state = new BedtimeState(
            Map.of(userId, new PlayerTimezoneRecord(userId, ZoneId.of("America/Chicago"))),
            Map.of(userId, new PlayerLocaleRecord(userId, "en_us")),
            Map.of(userId, new ScheduledBanRecord(userId, Instant.parse("2026-06-19T22:30:00Z"), Instant.parse("2026-06-20T06:30:00Z"), "Bedtime", 1))
        );

        BedtimeState decoded = codec.decode(codec.encode(state));

        assertEquals(state, decoded);
    }

    @Test
    void skipsMalformedEntries() {
        BedtimeStateCodec codec = new BedtimeStateCodec();
        String json = """
            {
              "timezones": [
                {"playerUuid": "11111111-1111-1111-1111-111111111111", "zoneId": "UTC"},
                {"playerUuid": "22222222-2222-2222-2222-222222222222", "zoneId": "Nope/Bad"}
              ],
              "scheduledBans": [
                {"playerUuid": "11111111-1111-1111-1111-111111111111", "start": "2026-06-19T22:30:00Z", "end": "2026-06-20T06:30:00Z", "reason": "Bedtime", "warningsSent": 0},
                {"playerUuid": "broken", "start": "2026-06-19T22:30:00Z", "end": "2026-06-20T06:30:00Z", "reason": "Bedtime", "warningsSent": 0}
              ]
            }
            """;

        BedtimeState decoded = codec.decode(json);

        assertEquals(1, decoded.timezones().size());
        assertEquals(1, decoded.scheduledBans().size());
        assertTrue(decoded.timezones().containsKey(UUID.fromString("11111111-1111-1111-1111-111111111111")));
    }
}
