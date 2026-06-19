package ricket.bedtimeban.common.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ricket.bedtimeban.common.persistence.BedtimeStateStore;
import ricket.bedtimeban.core.model.PlayerTimezoneRecord;
import ricket.bedtimeban.core.model.ScheduledBanRecord;
import ricket.bedtimeban.core.persistence.BedtimeStateCodec;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedtimeRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void storesAndRemovesRecords() throws Exception {
        BedtimeRepository repository = new BedtimeRepository(new BedtimeStateStore(new BedtimeStateCodec()));
        repository.attachStore(tempDir.resolve("bedtimeban-state.json"));

        UUID userId = UUID.randomUUID();
        repository.putTimezone(new PlayerTimezoneRecord(userId, ZoneId.of("UTC")));
        repository.putScheduledBan(new ScheduledBanRecord(userId, Instant.parse("2026-06-19T22:30:00Z"), Instant.parse("2026-06-20T06:30:00Z"), "Bedtime", 0));

        assertEquals("UTC", repository.getTimezone(userId).zoneId().getId());
        assertTrue(repository.hasScheduledBan(userId));
        assertTrue(repository.removeScheduledBan(userId));
        assertFalse(repository.hasScheduledBan(userId));
    }
}

