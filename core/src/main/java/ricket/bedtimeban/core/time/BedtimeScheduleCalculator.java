package ricket.bedtimeban.core.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class BedtimeScheduleCalculator {
    private final Clock clock;

    public BedtimeScheduleCalculator(Clock clock) {
        this.clock = clock;
    }

    public ZonedDateTime calculate(LocalTime bedtime, ZoneId zoneId) {
        ZonedDateTime nowRoundedDown = Instant.now(clock)
            .atZone(zoneId)
            .withMinute(0)
            .withSecond(0)
            .withNano(0);

        ZonedDateTime updated = nowRoundedDown
            .withHour(bedtime.getHour())
            .withMinute(bedtime.getMinute());

        if (updated.isBefore(nowRoundedDown)) {
            updated = updated.plusDays(1);
        }
        return updated;
    }
}

