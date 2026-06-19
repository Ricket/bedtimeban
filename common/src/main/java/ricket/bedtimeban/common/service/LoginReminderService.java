package ricket.bedtimeban.common.service;

import ricket.bedtimeban.core.model.PlayerTimezoneRecord;
import ricket.bedtimeban.core.model.ScheduledBanRecord;
import ricket.bedtimeban.core.service.BedtimeDomainService;

import java.util.Optional;
import java.util.UUID;

public final class LoginReminderService {
    private final BedtimeRepository repository;
    private final BedtimeDomainService domainService;

    public LoginReminderService(BedtimeRepository repository, BedtimeDomainService domainService) {
        this.repository = repository;
        this.domainService = domainService;
    }

    public void sendReminderIfNeeded(BedtimeServerAccess access, UUID playerUuid) {
        ScheduledBanRecord scheduledBan = repository.getScheduledBan(playerUuid);
        PlayerTimezoneRecord timezone = repository.getTimezone(playerUuid);
        Optional<String> reminder = domainService.makeReminderString(scheduledBan, timezone);
        reminder.ifPresent(message -> access.sendSystemMessage(playerUuid, message));
    }
}

