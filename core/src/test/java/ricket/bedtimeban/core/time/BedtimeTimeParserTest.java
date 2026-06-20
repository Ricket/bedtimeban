package ricket.bedtimeban.core.time;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedtimeTimeParserTest {
    @Test
    void acceptsSupportedFormats() {
        assertEquals(LocalTime.of(23, 0), BedtimeTimeParser.parse("11pm").orElseThrow());
        assertEquals(LocalTime.of(9, 0), BedtimeTimeParser.parse("9am").orElseThrow());
        assertEquals(LocalTime.of(23, 30), BedtimeTimeParser.parse("11:30pm").orElseThrow());
        assertEquals(LocalTime.of(9, 5), BedtimeTimeParser.parse("09:05am").orElseThrow());
        assertEquals(LocalTime.of(23, 30), BedtimeTimeParser.parse("11:30 pm").orElseThrow());
        assertEquals(LocalTime.of(23, 30), BedtimeTimeParser.parse("11:30 PM").orElseThrow());
        assertEquals(LocalTime.of(23, 30), BedtimeTimeParser.parse("11:30 P.M.").orElseThrow());
        assertEquals(LocalTime.of(23, 0), BedtimeTimeParser.parse("11 p.m.").orElseThrow());
        assertEquals(LocalTime.of(23, 0), BedtimeTimeParser.parse("23").orElseThrow());
        assertEquals(LocalTime.of(23, 0), BedtimeTimeParser.parse("23:00").orElseThrow());
        assertEquals(LocalTime.of(23, 30), BedtimeTimeParser.parse("23.30").orElseThrow());
        assertEquals(LocalTime.of(9, 5), BedtimeTimeParser.parse("9.05").orElseThrow());
    }

    @Test
    void rejectsUnsupportedFormats() {
        assertTrue(BedtimeTimeParser.parse("11:7pm").isEmpty());
        assertTrue(BedtimeTimeParser.parse("24:00").isEmpty());
        assertTrue(BedtimeTimeParser.parse("11:30 xm").isEmpty());
        assertTrue(BedtimeTimeParser.parse("noon").isEmpty());
        assertTrue(BedtimeTimeParser.parse("").isEmpty());
        assertFalse(BedtimeTimeParser.parse(" 11pm ").isEmpty());
    }
}
