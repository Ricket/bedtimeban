package ricket.bedtimeban.core.time;

import java.time.LocalTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BedtimeTimeParser {
    private static final Pattern HOUR_AMPM = Pattern.compile("^(1[0-2]|0?[1-9])\\s*([ap])\\.?m\\.?$");
    private static final Pattern HOUR_MINUTE_AMPM = Pattern.compile("^(1[0-2]|0?[1-9])([:.])([0-5][0-9])\\s*([ap])\\.?m\\.?$");
    private static final Pattern HOUR_24 = Pattern.compile("^([01]?\\d|2[0-3])$");
    private static final Pattern HOUR_MINUTE_24 = Pattern.compile("^([01]?\\d|2[0-3])([:.])([0-5][0-9])$");

    private BedtimeTimeParser() {
    }

    public static Optional<LocalTime> parse(String userInput) {
        if (userInput == null) {
            return Optional.empty();
        }

        String normalized = userInput.trim().toLowerCase();
        if (normalized.isEmpty()) {
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

        LocalTime hourOnly24 = tryParseHour24(normalized);
        if (hourOnly24 != null) {
            return Optional.of(hourOnly24);
        }

        LocalTime hourMinute24 = tryParseHourMinute24(normalized);
        if (hourMinute24 != null) {
            return Optional.of(hourMinute24);
        }

        return Optional.empty();
    }

    private static LocalTime tryParseHourAmpm(String value) {
        Matcher matcher = HOUR_AMPM.matcher(value);
        if (!matcher.matches()) {
            return null;
        }
        int hour = Integer.parseInt(matcher.group(1));
        boolean pm = matcher.group(2).equals("p");
        return LocalTime.of(toHour24(hour, pm), 0);
    }

    private static LocalTime tryParseHourMinuteAmpm(String value) {
        Matcher matcher = HOUR_MINUTE_AMPM.matcher(value);
        if (!matcher.matches()) {
            return null;
        }
        int hour = Integer.parseInt(matcher.group(1));
        int minute = Integer.parseInt(matcher.group(3));
        boolean pm = matcher.group(4).equals("p");
        return LocalTime.of(toHour24(hour, pm), minute);
    }

    private static LocalTime tryParseHour24(String value) {
        Matcher matcher = HOUR_24.matcher(value);
        if (!matcher.matches()) {
            return null;
        }
        return LocalTime.of(Integer.parseInt(matcher.group(1)), 0);
    }

    private static LocalTime tryParseHourMinute24(String value) {
        Matcher matcher = HOUR_MINUTE_24.matcher(value);
        if (!matcher.matches()) {
            return null;
        }
        int hour = Integer.parseInt(matcher.group(1));
        int minute = Integer.parseInt(matcher.group(3));
        return LocalTime.of(hour, minute);
    }

    private static int toHour24(int hour, boolean pm) {
        if (hour == 12) {
            return pm ? 12 : 0;
        }
        return hour + (pm ? 12 : 0);
    }
}
