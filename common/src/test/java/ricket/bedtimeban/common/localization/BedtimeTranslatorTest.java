package ricket.bedtimeban.common.localization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BedtimeTranslatorTest {
    private final BedtimeTranslator translator = new BedtimeTranslator();

    @Test
    void fallsBackToEnglishForUnsupportedLocale() {
        assertEquals("Good night!", translator.render("zz_zz", "bedtimeban.disconnect.good_night"));
    }

    @Test
    void loadsAlternateLocaleWhenAvailable() {
        assertEquals("Bonne nuit !", translator.render("fr_fr", "bedtimeban.disconnect.good_night"));
    }

    @Test
    void formatsPositionalArguments() {
        assertEquals("Ban has been cancelled for Alex", translator.render("en_us", "bedtimeban.command.cancel.target.success", "Alex"));
    }
}
