package ricket.bedtimeban.common;

import ricket.bedtimeban.common.localization.BedtimeTranslator;
import ricket.bedtimeban.common.persistence.BedtimeStateStore;
import ricket.bedtimeban.common.service.BanEnforcementService;
import ricket.bedtimeban.common.service.BedtimeMessagingService;
import ricket.bedtimeban.common.service.BedtimeRepository;
import ricket.bedtimeban.common.service.BedtimeServerAccess;
import ricket.bedtimeban.common.service.LoginReminderService;
import ricket.bedtimeban.core.persistence.BedtimeStateCodec;
import ricket.bedtimeban.core.service.BedtimeDomainService;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.UUID;
import java.util.function.Consumer;

public final class BedtimeBanCommon {
    private final BedtimeDomainService domainService = new BedtimeDomainService(Clock.systemUTC());
    private final BedtimeRepository repository = new BedtimeRepository(new BedtimeStateStore(new BedtimeStateCodec()));
    private final BedtimeMessagingService messagingService = new BedtimeMessagingService(repository, domainService, new BedtimeTranslator());
    private final BanEnforcementService enforcementService = new BanEnforcementService(repository, domainService, messagingService, Clock.systemUTC());
    private final LoginReminderService loginReminderService = new LoginReminderService(messagingService);

    public BedtimeRepository repository() {
        return repository;
    }

    public BedtimeDomainService domainService() {
        return domainService;
    }

    public BedtimeMessagingService messagingService() {
        return messagingService;
    }

    public void initialize(Path stateFile) {
        try {
            repository.attachStore(stateFile);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize Bedtime Ban state store", e);
        }
    }

    public void tick(BedtimeServerAccess access, Consumer<String> logger) {
        enforcementService.tick(access, logger);
    }

    public void onPlayerLogin(BedtimeServerAccess access, UUID playerUuid) {
        access.getPlayerLocale(playerUuid).ifPresent(locale -> {
            try {
                messagingService.rememberLocale(playerUuid, locale);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to store player locale", e);
            }
        });
        loginReminderService.sendReminderIfNeeded(access, playerUuid);
    }
}
