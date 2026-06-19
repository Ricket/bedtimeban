package ricket.bedtimeban.core.time;

import java.time.LocalTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BedtimeTimeParser {
    private static final Pattern HOUR_AMPM = Pattern.compile("^(1[0-2]|0?[1-9])(am|pm)$");
    private static final Pattern HOUR_MINUTE_AMPM = Pattern.compile("^(1[0-2]|0?[1-9]):([0-5][0-9])(am|pm)$");

    private BedtimeTimeParser() {
    }

    public static Optional<LocalTime> parse(String userInput) {
        if (userInput == null) {
            return Optional.empty();
        }

        String normalized = userInput.trim().toLowerCase();
        if (normalized.isEmpty() || normalized.contains(" ")) {
            return Optional.empty();
        }

        LocalTime hourOnly = tryParseHourAmpm(normalized);
        if (hourOnly != null) {
            return Optional.of(hourOnly);
        }

        LocalTime hourMinute = tryParseHourMinuteAmpm(normalized);
        if (hourMinute != null) {
            return Optional.of(hourMinute);
        }

        return Optional.empty();
    }

    private static LocalTime tryParseHourAmpm(String value) {
        Matcher matcher = HOUR_AMPM.matcher(value);
        if (!matcher.matches()) {
            return null;
        }
        int hour = Integer.parseInt(matcher.group(1));
        boolean pm = matcher.group(2).equals("pm");
        return LocalTime.of(toHour24(hour, pm), 0);
    }

    private static LocalTime tryParseHourMinuteAmpm(String value) {
        Matcher matcher = HOUR_MINUTE_AMPM.matcher(value);
        if (!matcher.matches()) {
            return null;
        }
        int hour = Integer.parseInt(matcher.group(1));
        int minute = Integer.parseInt(matcher.group(2));
        boolean pm = matcher.group(3).equals("pm");
        return LocalTime.of(toHour24(hour, pm), minute);
    }

    private static int toHour24(int hour, boolean pm) {
        if (hour == 12) {
            return pm ? 12 : 0;
        }
        return hour + (pm ? 12 : 0);
    }
}

