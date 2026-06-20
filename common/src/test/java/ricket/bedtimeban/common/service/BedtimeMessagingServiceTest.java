package ricket.bedtimeban.common.service;

import org.junit.jupiter.api.Test;
import ricket.bedtimeban.common.localization.BedtimeTranslator;
import ricket.bedtimeban.core.service.BedtimeDomainService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BedtimeMessagingServiceTest {
    @Test
    void rendersSetSuccessForToday() {
        BedtimeMessagingService messagingService = messagingServiceAt("2026-06-20T18:00:00Z");

        String message = messagingService.renderSetSuccess(
            ZonedDateTime.of(2026, 6, 20, 23, 0, 0, 0, ZoneId.of("America/Chicago")),
            "en_us"
        );

        assertEquals("Ok, you will be banned at 11:00\u202fPM CT.", message);
    }

    @Test
    void rendersSetSuccessForTomorrow() {
        BedtimeMessagingService messagingService = messagingServiceAt("2026-06-20T03:00:00Z");

        String message = messagingService.renderSetSuccess(
            ZonedDateTime.of(2026, 6, 21, 1, 0, 0, 0, ZoneId.of("America/Chicago")),
            "en_us"
        );

        assertEquals("Ok, you will be banned tomorrow at 1:00\u202fAM CT.", message);
    }

    @Test
    void rendersSameTimeForTomorrowInAlternateLocale() {
        BedtimeMessagingService messagingService = messagingServiceAt("2026-06-20T03:00:00Z");

        String message = messagingService.renderSetSameTime(
            ZonedDateTime.of(2026, 6, 21, 1, 0, 0, 0, ZoneId.of("America/Chicago")),
            "fr_fr"
        );

        assertEquals("Votre heure du coucher est deja definie pour demain a 01:00 CT.", message);
    }

    private BedtimeMessagingService messagingServiceAt(String now) {
        Clock clock = Clock.fixed(Instant.parse(now), ZoneOffset.UTC);
        BedtimeRepository repository = new BedtimeRepository(null);
        BedtimeDomainService domainService = new BedtimeDomainService(clock);
        return new BedtimeMessagingService(repository, domainService, new BedtimeTranslator());
    }
}
