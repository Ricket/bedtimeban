package ricket.bedtimeban;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import javax.annotation.CheckForNull;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeParser {
    private final Pattern hourAmpmPattern = Pattern.compile("^(1[0-2]|0?[1-9])(am|pm)$");
    private final Pattern hourMinuteAmpmPattern = Pattern.compile("^(1[0-2]|0?[1-9]):([0-5][0-9])(am|pm)$");

    @VisibleForTesting
    Clock clock = Clock.systemUTC();

    @CheckForNull
    public ZonedDateTime parseUserInput(String str, ZoneId timezone) {
        str = str.trim().toLowerCase();

        ZonedDateTime hourAmpm = tryParseHourAmpm(str, timezone);
        if (hourAmpm != null) {
            return hourAmpm;
        }

        ZonedDateTime hourMinuteAmpm = tryParseHourMinuteAmpm(str, timezone);
        if (hourMinuteAmpm != null) {
            return hourMinuteAmpm;
        }

        // TODO other formats

        return null;
    }

    @CheckForNull
    private ZonedDateTime tryParseHourAmpm(String str, ZoneId timezone) {
        Matcher matcher = hourAmpmPattern.matcher(str);
        if (!matcher.matches()) {
            return null;
        }

        int hourNum = Integer.parseInt(matcher.group(1), 10);
        boolean pm = matcher.group(2).equals("pm");

        return makeZonedDateTime(toHour24(hourNum, pm), 0, timezone);
    }

    @CheckForNull
    private ZonedDateTime tryParseHourMinuteAmpm(String str, ZoneId timezone) {
        Matcher matcher = hourMinuteAmpmPattern.matcher(str);
        if (!matcher.matches()) {
            return null;
        }

        int hourNum = Integer.parseInt(matcher.group(1), 10);
        int minuteNum = Integer.parseInt(matcher.group(2), 10);
        boolean pm = matcher.group(3).equals("pm");

        return makeZonedDateTime(toHour24(hourNum, pm), minuteNum, timezone);
    }

    private int toHour24(int hour, boolean pm) {
        if (hour == 12) {
            return pm ? 12 : 0;
        } else {
            return hour + (pm ? 12 : 0);
        }
    }

    private ZonedDateTime makeZonedDateTime(int hour24, int minute, ZoneId timezone) {
        ZonedDateTime nowRoundedDown = Instant.now(clock)
                .atZone(timezone)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        ZonedDateTime updatedTime = nowRoundedDown
                .withHour(hour24)
                .withMinute(minute);
        if (updatedTime.isBefore(nowRoundedDown)) {
            updatedTime = updatedTime.plusDays(1);
            Preconditions.checkState(!updatedTime.isBefore(nowRoundedDown));
        }
        return updatedTime;
    }
}
