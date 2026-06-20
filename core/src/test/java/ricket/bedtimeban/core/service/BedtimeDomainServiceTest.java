package ricket.bedtimeban.core.service;

import org.junit.jupiter.api.Test;
import ricket.bedtimeban.core.model.BanWarningThreshold;
import ricket.bedtimeban.core.model.ScheduledBanRecord;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
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

    @Test
    void scheduleOrUpdateCreatesWhenNoExistingRecord() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-19T20:00:00Z"), ZoneOffset.UTC);
        BedtimeDomainService service = new BedtimeDomainService(clock);

        BedtimeDomainService.ScheduleBedtimeResult result = service.scheduleOrUpdateBan(
            UUID.randomUUID(),
            ZoneOffset.UTC,
            LocalTime.of(23, 30),
            null
        );

        assertEquals(BedtimeDomainService.ScheduleBedtimeStatus.CREATED, result.status());
        assertEquals(Instant.parse("2026-06-19T23:30:00Z"), result.record().start());
    }

    @Test
    void scheduleOrUpdateAllowsEarlierFutureCorrection() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-19T20:00:00Z"), ZoneOffset.UTC);
        BedtimeDomainService service = new BedtimeDomainService(clock);
        UUID userId = UUID.randomUUID();
        ScheduledBanRecord existing = new ScheduledBanRecord(
            userId,
            Instant.parse("2026-06-20T11:30:00Z"),
            Instant.parse("2026-06-20T19:30:00Z"),
            "Bedtime",
            0
        );

        BedtimeDomainService.ScheduleBedtimeResult result = service.scheduleOrUpdateBan(
            userId,
            ZoneOffset.UTC,
            LocalTime.of(23, 30),
            existing
        );

        assertEquals(BedtimeDomainService.ScheduleBedtimeStatus.UPDATED_EARLIER, result.status());
        assertEquals(Instant.parse("2026-06-19T23:30:00Z"), result.record().start());
    }

    @Test
    void scheduleOrUpdateRejectsLaterTime() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-19T20:00:00Z"), ZoneOffset.UTC);
        BedtimeDomainService service = new BedtimeDomainService(clock);
        UUID userId = UUID.randomUUID();
        ScheduledBanRecord existing = new ScheduledBanRecord(
            userId,
            Instant.parse("2026-06-19T21:30:00Z"),
            Instant.parse("2026-06-20T05:30:00Z"),
            "Bedtime",
            0
        );

        BedtimeDomainService.ScheduleBedtimeResult result = service.scheduleOrUpdateBan(
            userId,
            ZoneOffset.UTC,
            LocalTime.of(23, 30),
            existing
        );

        assertEquals(BedtimeDomainService.ScheduleBedtimeStatus.REJECTED, result.status());
    }

    @Test
    void scheduleOrUpdateReturnsUnchangedForSameInstant() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-19T20:00:00Z"), ZoneOffset.UTC);
        BedtimeDomainService service = new BedtimeDomainService(clock);
        UUID userId = UUID.randomUUID();
        ScheduledBanRecord existing = new ScheduledBanRecord(
            userId,
            Instant.parse("2026-06-19T23:30:00Z"),
            Instant.parse("2026-06-20T07:30:00Z"),
            "Bedtime",
            0
        );

        BedtimeDomainService.ScheduleBedtimeResult result = service.scheduleOrUpdateBan(
            userId,
            ZoneOffset.UTC,
            LocalTime.of(23, 30),
            existing
        );

        assertEquals(BedtimeDomainService.ScheduleBedtimeStatus.UNCHANGED, result.status());
    }

    @Test
    void scheduleOrUpdateRejectsPastSameHourCorrection() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-19T22:45:00Z"), ZoneOffset.UTC);
        BedtimeDomainService service = new BedtimeDomainService(clock);
        UUID userId = UUID.randomUUID();
        ScheduledBanRecord existing = new ScheduledBanRecord(
            userId,
            Instant.parse("2026-06-20T11:30:00Z"),
            Instant.parse("2026-06-20T19:30:00Z"),
            "Bedtime",
            0
        );

        BedtimeDomainService.ScheduleBedtimeResult result = service.scheduleOrUpdateBan(
            userId,
            ZoneId.of("UTC"),
            LocalTime.of(22, 30),
            existing
        );

        assertEquals(BedtimeDomainService.ScheduleBedtimeStatus.REJECTED, result.status());
    }

    @Test
    void scheduleOrUpdateRejectsActiveBanRecord() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-19T20:00:00Z"), ZoneOffset.UTC);
        BedtimeDomainService service = new BedtimeDomainService(clock);
        UUID userId = UUID.randomUUID();
        ScheduledBanRecord existing = new ScheduledBanRecord(
            userId,
            null,
            Instant.parse("2026-06-20T07:30:00Z"),
            "Bedtime",
            0
        );

        BedtimeDomainService.ScheduleBedtimeResult result = service.scheduleOrUpdateBan(
            userId,
            ZoneOffset.UTC,
            LocalTime.of(21, 30),
            existing
        );

        assertEquals(BedtimeDomainService.ScheduleBedtimeStatus.REJECTED, result.status());
    }
}
