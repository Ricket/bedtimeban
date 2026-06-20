package ricket.bedtimeban.common.service;

import org.junit.jupiter.api.Test;
import ricket.bedtimeban.common.localization.BedtimeTranslator;
import ricket.bedtimeban.core.service.BedtimeDomainService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BedtimeMessagingServiceTest {
    @Test
    void rendersSetSuccessForToday() {
        TestContext context = messagingServiceAt("2026-06-20T18:00:00Z");
        ZonedDateTime bedtime = ZonedDateTime.of(2026, 6, 20, 23, 0, 0, 0, ZoneId.of("America/Chicago"));

        String message = context.messagingService.renderSetSuccess(bedtime, "en_us");
        String expected = context.translator.render(
            "en_us",
            "bedtimeban.command.set.success.today",
            context.domainService.formatConfirmationTime(bedtime, Locale.US)
        );

        assertEquals(expected, message);
    }

    @Test
    void rendersSetSuccessForTomorrow() {
        TestContext context = messagingServiceAt("2026-06-20T03:00:00Z");
        ZonedDateTime bedtime = ZonedDateTime.of(2026, 6, 21, 1, 0, 0, 0, ZoneId.of("America/Chicago"));

        String message = context.messagingService.renderSetSuccess(bedtime, "en_us");
        String expected = context.translator.render(
            "en_us",
            "bedtimeban.command.set.success.tomorrow",
            context.domainService.formatConfirmationTime(bedtime, Locale.US)
        );

        assertEquals(expected, message);
    }

    @Test
    void rendersSameTimeForTomorrowInAlternateLocale() {
        TestContext context = messagingServiceAt("2026-06-20T03:00:00Z");
        ZonedDateTime bedtime = ZonedDateTime.of(2026, 6, 21, 1, 0, 0, 0, ZoneId.of("America/Chicago"));

        String message = context.messagingService.renderSetSameTime(bedtime, "fr_fr");
        String expected = context.translator.render(
            "fr_fr",
            "bedtimeban.command.set.same_time.tomorrow",
            context.domainService.formatConfirmationTime(bedtime, Locale.FRANCE)
        );

        assertEquals(expected, message);
    }

    private TestContext messagingServiceAt(String now) {
        Clock clock = Clock.fixed(Instant.parse(now), ZoneOffset.UTC);
        BedtimeRepository repository = new BedtimeRepository(null);
        BedtimeDomainService domainService = new BedtimeDomainService(clock);
        BedtimeTranslator translator = new BedtimeTranslator();
        BedtimeMessagingService messagingService = new BedtimeMessagingService(repository, domainService, translator);
        return new TestContext(messagingService, domainService, translator);
    }

    private record TestContext(
        BedtimeMessagingService messagingService,
        BedtimeDomainService domainService,
        BedtimeTranslator translator
    ) {
    }
}
