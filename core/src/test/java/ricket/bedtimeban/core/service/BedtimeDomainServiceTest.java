package ricket.bedtimeban.core.service;

import org.junit.jupiter.api.Test;
import ricket.bedtimeban.core.model.BanWarningThreshold;
import ricket.bedtimeban.core.model.ScheduledBanRecord;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedtimeDomainServiceTest {
    @Test
    void shortLeadTimeSkipsFifteenAndFiveMinuteWarnings() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-19T22:28:00Z"), ZoneOffset.UTC);
        BedtimeDomainService service = new BedtimeDomainService(clock);

        ScheduledBanRecord record = service.scheduleBan(UUID.randomUUID(), ZoneOffset.UTC, LocalTime.of(22, 30));

        assertEquals(2, record.warningsSent());
    }

    @Test
    void tenMinuteLeadTimeSkipsOnlyFifteenMinuteWarning() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-19T22:30:00Z"), ZoneOffset.UTC);
        BedtimeDomainService service = new BedtimeDomainService(clock);

        ScheduledBanRecord record = service.scheduleBan(UUID.randomUUID(), ZoneOffset.UTC, LocalTime.of(22, 40));

        assertEquals(1, record.warningsSent());
    }

    @Test
    void nextWarningSelectsNearestApplicableThreshold() {
        BedtimeDomainService service = new BedtimeDomainService(Clock.systemUTC());
        ScheduledBanRecord record = new ScheduledBanRecord(
            UUID.randomUUID(),
            Instant.parse("2026-06-19T22:40:00Z"),
            Instant.parse("2026-06-20T06:40:00Z"),
            "Bedtime",
            1
        );

        BedtimeDomainService.PendingWarning warning = service
            .nextWarningThreshold(record, Instant.parse("2026-06-19T22:36:00Z"))
            .orElseThrow();

        assertEquals(BanWarningThreshold.FIVE_MINUTES, warning.threshold());
        assertEquals(2, warning.warningsSentAfterProcessing());
    }

    @Test
    void nextWarningSkipsMissedLargerThresholdsWhenSmallerOneApplies() {
        BedtimeDomainService service = new BedtimeDomainService(Clock.systemUTC());
        ScheduledBanRecord record = new ScheduledBanRecord(
            UUID.randomUUID(),
            Instant.parse("2026-06-19T22:45:00Z"),
            Instant.parse("2026-06-20T06:45:00Z"),
            "Bedtime",
            0
        );

        BedtimeDomainService.PendingWarning warning = service
            .nextWarningThreshold(record, Instant.parse("2026-06-19T22:41:00Z"))
            .orElseThrow();

        assertEquals(BanWarningThreshold.FIVE_MINUTES, warning.threshold());
        assertEquals(2, warning.warningsSentAfterProcessing());
    }

    @Test
    void noWarningWhenNoThresholdIsDueYet() {
        BedtimeDomainService service = new BedtimeDomainService(Clock.systemUTC());
        ScheduledBanRecord record = new ScheduledBanRecord(
            UUID.randomUUID(),
            Instant.parse("2026-06-19T22:45:00Z"),
            Instant.parse("2026-06-20T06:45:00Z"),
            "Bedtime",
            0
        );

        assertTrue(service.nextWarningThreshold(record, Instant.parse("2026-06-19T22:29:00Z")).isEmpty());
    }
}
