package ricket.bedtimeban.common.service;

import java.util.Optional;
import java.util.UUID;

public final class LoginReminderService {
    private final BedtimeMessagingService messagingService;

    public LoginReminderService(BedtimeMessagingService messagingService) {
        this.messagingService = messagingService;
    }

    public void sendReminderIfNeeded(BedtimeServerAccess access, UUID playerUuid) {
        Optional<String> reminder = messagingService.makeReminderMessage(playerUuid, access.getPlayerLocale(playerUuid).orElse(null));
        reminder.ifPresent(message -> access.sendSystemMessage(playerUuid, message));
    }
}
