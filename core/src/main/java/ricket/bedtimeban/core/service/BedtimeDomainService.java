package ricket.bedtimeban.core.service;

import ricket.bedtimeban.core.model.BanWarningThreshold;
import ricket.bedtimeban.core.model.PlayerTimezoneRecord;
import ricket.bedtimeban.core.model.ScheduledBanRecord;
import ricket.bedtimeban.core.time.BedtimeFormatting;
import ricket.bedtimeban.core.time.BedtimeScheduleCalculator;
import ricket.bedtimeban.core.time.BedtimeTimeParser;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class BedtimeDomainService {
    private final BedtimeScheduleCalculator scheduleCalculator;

    public BedtimeDomainService(Clock clock) {
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
        Instant start = scheduled.toInstant();
        Instant end = start.plus(8, ChronoUnit.HOURS);
        return new ScheduledBanRecord(playerUuid, start, end, "Bedtime", 0);
    }

    public Optional<String> makeReminderString(ScheduledBanRecord record, PlayerTimezoneRecord timezoneRecord) {
        if (record == null || record.start() == null || timezoneRecord == null) {
            return Optional.empty();
        }
        return Optional.of("Reminder that your bedtime is: " + BedtimeFormatting.formatReminder(record.start(), timezoneRecord.zoneId()));
    }

    public Optional<BanWarningThreshold> nextWarningThreshold(ScheduledBanRecord record) {
        if (record == null || record.start() == null) {
            return Optional.empty();
        }
        BanWarningThreshold[] thresholds = BanWarningThreshold.values();
        if (record.warningsSent() < 0 || record.warningsSent() >= thresholds.length) {
            return Optional.empty();
        }
        return Optional.of(thresholds[record.warningsSent()]);
    }

    public String formatTimezoneDisplay(ZoneId zoneId) {
        return zoneId.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " (" + zoneId.getId() + ")";
    }

    public String formatConfirmation(ZonedDateTime bedtime) {
        return BedtimeFormatting.formatConfirmation(bedtime);
    }

    public ZonedDateTime calculateScheduledDateTime(ZoneId zoneId, LocalTime bedtime) {
        return scheduleCalculator.calculate(bedtime, zoneId);
    }
}

