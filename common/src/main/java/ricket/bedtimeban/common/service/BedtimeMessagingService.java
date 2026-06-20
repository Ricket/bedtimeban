package ricket.bedtimeban.common.service;

import ricket.bedtimeban.common.localization.BedtimeTranslator;
import ricket.bedtimeban.common.localization.LocalizedMessage;
import ricket.bedtimeban.core.model.BanWarningThreshold;
import ricket.bedtimeban.core.model.PlayerLocaleRecord;
import ricket.bedtimeban.core.model.PlayerTimezoneRecord;
import ricket.bedtimeban.core.model.ScheduledBanRecord;
import ricket.bedtimeban.core.service.BedtimeDomainService;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class BedtimeMessagingService {
    private final BedtimeRepository repository;
    private final BedtimeDomainService domainService;
    private final BedtimeTranslator translator;

    public BedtimeMessagingService(BedtimeRepository repository, BedtimeDomainService domainService, BedtimeTranslator translator) {
        this.repository = repository;
        this.domainService = domainService;
        this.translator = translator;
    }

    public void rememberLocale(UUID playerUuid, String localeCode) throws IOException {
        String normalizedLocale = translator.normalizeLocale(localeCode);
        PlayerLocaleRecord existing = repository.getLocale(playerUuid);
        if (existing != null && existing.locale().equals(normalizedLocale)) {
            return;
        }
        repository.putLocale(new PlayerLocaleRecord(playerUuid, normalizedLocale));
    }

    public String resolveLocale(UUID playerUuid, String currentLocale) {
        if (currentLocale != null && !currentLocale.isBlank()) {
            return translator.normalizeLocale(currentLocale);
        }

        PlayerLocaleRecord existing = repository.getLocale(playerUuid);
        if (existing != null && existing.locale() != null && !existing.locale().isBlank()) {
            return translator.normalizeLocale(existing.locale());
        }

        return BedtimeTranslator.DEFAULT_LOCALE;
    }

    public String render(String localeCode, String key, Object... args) {
        return translator.render(localeCode, key, args);
    }

    public String render(String localeCode, LocalizedMessage message) {
        return translator.render(localeCode, message);
    }

    public Optional<String> makeReminderMessage(UUID playerUuid, String currentLocale) {
        ScheduledBanRecord record = repository.getScheduledBan(playerUuid);
        PlayerTimezoneRecord timezone = repository.getTimezone(playerUuid);
        if (record == null || record.start() == null || timezone == null) {
            return Optional.empty();
        }

        String localeCode = resolveLocale(playerUuid, currentLocale);
        String reminderTime = domainService.formatReminder(record.start(), timezone.zoneId(), translator.toJavaLocale(localeCode));
        return Optional.of(render(localeCode, "bedtimeban.command.root.reminder", reminderTime));
    }

    public String warningMessage(UUID playerUuid, String currentLocale, BanWarningThreshold threshold) {
        String localeCode = resolveLocale(playerUuid, currentLocale);
        return render(localeCode, warningKey(threshold));
    }

    public String disconnectMessage(UUID playerUuid, String currentLocale) {
        return render(resolveLocale(playerUuid, currentLocale), "bedtimeban.disconnect.good_night");
    }

    public String banReason(UUID playerUuid, String currentLocale) {
        return render(resolveLocale(playerUuid, currentLocale), "bedtimeban.ban.reason");
    }

    public String formatTimezoneDisplay(ZoneId zoneId, String localeCode) {
        Locale locale = translator.toJavaLocale(localeCode);
        return zoneId.getDisplayName(TextStyle.FULL, locale) + " (" + zoneId.getId() + ")";
    }

    public String formatConfirmation(ZonedDateTime bedtime, String localeCode) {
        return domainService.formatConfirmation(bedtime, translator.toJavaLocale(localeCode));
    }

    public BedtimeTranslator translator() {
        return translator;
    }

    private String warningKey(BanWarningThreshold threshold) {
        return switch (threshold) {
            case FIFTEEN_MINUTES -> "bedtimeban.warning.fifteen_minutes";
            case FIVE_MINUTES -> "bedtimeban.warning.five_minutes";
            case ONE_MINUTE -> "bedtimeban.warning.one_minute";
        };
    }
}
