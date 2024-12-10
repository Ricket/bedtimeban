package ricket.bedtimeban.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import javax.annotation.CheckForNull;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClockTimeArgument implements ArgumentType<LocalTime> {
    public static final String KEY = "clocktime";

    private final Pattern hourAmpmPattern = Pattern.compile("^(1[0-2]|0?[1-9])(am|pm)$");
    private final Pattern hourMinuteAmpmPattern = Pattern.compile("^(1[0-2]|0?[1-9]):([0-5][0-9])(am|pm)$");

    public static ClockTimeArgument clockTime()
    {
        return new ClockTimeArgument();
    }

    @Override
    public LocalTime parse(StringReader reader) throws CommandSyntaxException {
        String str = reader.getString().trim().toLowerCase();

        LocalTime hourAmpm = tryParseHourAmpm(str);
        if (hourAmpm != null) {
            return hourAmpm;
        }

        LocalTime hourMinuteAmpm = tryParseHourMinuteAmpm(str);
        if (hourMinuteAmpm != null) {
            return hourMinuteAmpm;
        }

        // TODO other formats

        return null;
    }

    @Override
    public Collection<String> getExamples() {
        return List.of(
                "11:30pm",
                "1am"
        );
    }

    @CheckForNull
    private LocalTime tryParseHourAmpm(String str) {
        Matcher matcher = hourAmpmPattern.matcher(str);
        if (!matcher.matches()) {
            return null;
        }

        int hourNum = Integer.parseInt(matcher.group(1), 10);
        boolean pm = matcher.group(2).equals("pm");

        return LocalTime.of(toHour24(hourNum, pm), 0);
    }

    @CheckForNull
    private LocalTime tryParseHourMinuteAmpm(String str) {
        Matcher matcher = hourMinuteAmpmPattern.matcher(str);
        if (!matcher.matches()) {
            return null;
        }

        int hourNum = Integer.parseInt(matcher.group(1), 10);
        int minuteNum = Integer.parseInt(matcher.group(2), 10);
        boolean pm = matcher.group(3).equals("pm");

        return LocalTime.of(toHour24(hourNum, pm), minuteNum);
    }

    private int toHour24(int hour, boolean pm) {
        if (hour == 12) {
            return pm ? 12 : 0;
        } else {
            return hour + (pm ? 12 : 0);
        }
    }
}
