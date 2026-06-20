package ricket.bedtimeban.core.time;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.util.Locale;

public final class BedtimeFormatting {
    private BedtimeFormatting() {
    }

    public static String formatReminder(Instant start, ZoneId zoneId, Locale locale) {
        ZonedDateTime zonedDateTime = start.atZone(zoneId);
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).localizedBy(locale);
        return zonedDateTime.format(formatter) + " " + zoneId.getDisplayName(TextStyle.SHORT, locale);
    }

    public static String formatConfirmation(ZonedDateTime bedtime, Locale locale) {
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).localizedBy(locale);
        return bedtime.format(formatter) + " " + bedtime.getZone().getDisplayName(TextStyle.SHORT, locale);
    }
}
