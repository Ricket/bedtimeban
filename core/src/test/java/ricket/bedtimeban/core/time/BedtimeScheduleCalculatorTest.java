package ricket.bedtimeban.core.time;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedtimeScheduleCalculatorTest {
    @Test
    void schedulesFutureTimeSameDay() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-19T10:15:00Z"), ZoneOffset.UTC);
        BedtimeScheduleCalculator calculator = new BedtimeScheduleCalculator(clock);

        ZonedDateTime scheduled = calculator.calculate(LocalTime.of(23, 30), ZoneId.of("UTC"));

        assertEquals(ZonedDateTime.parse("2026-06-19T23:30:00Z[UTC]"), scheduled);
    }

    @Test
    void schedulesEarlierHourNextDay() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-19T22:15:00Z"), ZoneOffset.UTC);
        BedtimeScheduleCalculator calculator = new BedtimeScheduleCalculator(clock);

        ZonedDateTime scheduled = calculator.calculate(LocalTime.of(21, 30), ZoneId.of("UTC"));

        assertEquals(ZonedDateTime.parse("2026-06-20T21:30:00Z[UTC]"), scheduled);
    }

    @Test
    void preservesSameHourPastMinuteQuirk() {
        Instant now = Instant.parse("2026-06-19T22:45:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        BedtimeScheduleCalculator calculator = new BedtimeScheduleCalculator(clock);

        ZonedDateTime scheduled = calculator.calculate(LocalTime.of(22, 30), ZoneId.of("UTC"));

        assertEquals(ZonedDateTime.parse("2026-06-19T22:30:00Z[UTC]"), scheduled);
        assertTrue(scheduled.toInstant().isBefore(now));
    }

    @Test
    void handlesTwelveAmAndPm() {
        assertEquals(LocalTime.MIDNIGHT, BedtimeTimeParser.parse("12am").orElseThrow());
        assertEquals(LocalTime.NOON, BedtimeTimeParser.parse("12pm").orElseThrow());
    }
}

