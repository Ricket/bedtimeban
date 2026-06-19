package ricket.bedtimeban.core.time;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class BedtimeFormatting {
    private static final DateTimeFormatter REMINDER_FORMATTER = DateTimeFormatter.ofPattern("hh:mma z");
    private static final DateTimeFormatter CONFIRMATION_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mma z");

    private BedtimeFormatting() {
    }

    public static String formatReminder(Instant start, ZoneId zoneId) {
        return start.atZone(zoneId).format(REMINDER_FORMATTER);
    }

    public static String formatConfirmation(ZonedDateTime bedtime) {
        return bedtime.format(CONFIRMATION_FORMATTER);
    }
}

