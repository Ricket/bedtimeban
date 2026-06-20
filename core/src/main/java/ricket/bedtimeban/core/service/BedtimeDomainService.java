package ricket.bedtimeban.core.service;

import ricket.bedtimeban.core.model.BanWarningThreshold;
import ricket.bedtimeban.core.model.ScheduledBanRecord;
import ricket.bedtimeban.core.time.BedtimeFormatting;
import ricket.bedtimeban.core.time.BedtimeScheduleCalculator;
import ricket.bedtimeban.core.time.BedtimeTimeParser;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

public final class BedtimeDomainService {
    private final Clock clock;
    private final BedtimeScheduleCalculator scheduleCalculator;

    public BedtimeDomainService(Clock clock) {
        this.clock = clock;
        this.scheduleCalculator = new BedtimeScheduleCalculator(clock);
    }

    public Optional<ZoneId> parseZoneId(String zoneId) {
        try {
            return Optional.of(ZoneId.of(zoneId));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public Optional<LocalTime> parseBedtime(String bedtime) {
        return BedtimeTimeParser.parse(bedtime);
    }

    public ScheduledBanRecord scheduleBan(UUID playerUuid, ZoneId zoneId, LocalTime bedtime) {
        ZonedDateTime scheduled = scheduleCalculator.calculate(bedtime, zoneId);
        return scheduleBan(playerUuid, scheduled);
    }

    public ScheduleBedtimeResult scheduleOrUpdateBan(UUID playerUuid, ZoneId zoneId, LocalTime bedtime, ScheduledBanRecord existing) {
        ZonedDateTime scheduled = scheduleCalculator.calculate(bedtime, zoneId);
        Instant scheduledInstant = scheduled.toInstant();
        if (existing == null) {
            return new ScheduleBedtimeResult(
                ScheduleBedtimeStatus.CREATED,
                scheduleBan(playerUuid, scheduled),
                scheduled
            );
        }

        if (existing.start() == null) {
            return new ScheduleBedtimeResult(ScheduleBedtimeStatus.REJECTED, existing, null);
        }

        int comparison = scheduledInstant.compareTo(existing.start());
        if (comparison == 0) {
            return new ScheduleBedtimeResult(ScheduleBedtimeStatus.UNCHANGED, existing, scheduled);
        }

        if (comparison > 0 || !scheduledInstant.isAfter(Instant.now(clock))) {
            return new ScheduleBedtimeResult(ScheduleBedtimeStatus.REJECTED, existing, null);
        }

        return new ScheduleBedtimeResult(
            ScheduleBedtimeStatus.UPDATED_EARLIER,
            scheduleBan(playerUuid, scheduled),
            scheduled
        );
    }

    public Optional<PendingWarning> nextWarningThreshold(ScheduledBanRecord record, Instant now) {
        if (record == null || record.start() == null) {
            return Optional.empty();
        }
        BanWarningThreshold[] thresholds = BanWarningThreshold.values();
        if (record.warningsSent() < 0 || record.warningsSent() >= thresholds.length) {
            return Optional.empty();
        }

        Duration remaining = Duration.between(now, record.start());
        int selectedIndex = -1;
        for (int index = record.warningsSent(); index < thresholds.length; index++) {
            if (remaining.compareTo(thresholds[index].toDuration()) <= 0) {
                selectedIndex = index;
            }
        }

        if (selectedIndex < 0) {
            return Optional.empty();
        }

        return Optional.of(new PendingWarning(thresholds[selectedIndex], selectedIndex + 1));
    }

    public String formatReminder(Instant start, ZoneId zoneId, java.util.Locale locale) {
        return BedtimeFormatting.formatReminder(start, zoneId, locale);
    }

    public String formatConfirmation(ZonedDateTime bedtime, java.util.Locale locale) {
        return BedtimeFormatting.formatConfirmation(bedtime, locale);
    }

    public ZonedDateTime calculateScheduledDateTime(ZoneId zoneId, LocalTime bedtime) {
        return scheduleCalculator.calculate(bedtime, zoneId);
    }

    private ScheduledBanRecord scheduleBan(UUID playerUuid, ZonedDateTime scheduled) {
        Instant start = scheduled.toInstant();
        Instant end = start.plus(8, ChronoUnit.HOURS);
        Duration initialLeadTime = Duration.between(Instant.now(clock), start);
        int skippedWarnings = skippedWarningCount(initialLeadTime);
        return new ScheduledBanRecord(playerUuid, start, end, "Bedtime", skippedWarnings);
    }

    private int skippedWarningCount(Duration leadTime) {
        BanWarningThreshold[] thresholds = BanWarningThreshold.values();
        int skipped = 0;
        for (BanWarningThreshold threshold : thresholds) {
            if (leadTime.compareTo(threshold.toDuration()) < 0) {
                skipped++;
            }
        }
        return skipped;
    }

    public record PendingWarning(BanWarningThreshold threshold, int warningsSentAfterProcessing) {
    }

    public record ScheduleBedtimeResult(ScheduleBedtimeStatus status, ScheduledBanRecord record, ZonedDateTime scheduled) {
    }

    public enum ScheduleBedtimeStatus {
        CREATED,
        UPDATED_EARLIER,
        UNCHANGED,
        REJECTED
    }
}
