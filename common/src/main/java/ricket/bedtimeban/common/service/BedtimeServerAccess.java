package ricket.bedtimeban.common.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface BedtimeServerAccess {
    boolean ban(UUID playerUuid, Instant start, Instant end, String reason, String disconnectMessage);

    void unban(UUID playerUuid);

    boolean isPlayerOnline(UUID playerUuid);

    void sendSystemMessage(UUID playerUuid, String message);

    String uuidToDisplayName(UUID playerUuid);

    Optional<UUID> resolvePlayerUuid(String usernameOrUuid);

    Optional<String> getPlayerLocale(UUID playerUuid);
}
