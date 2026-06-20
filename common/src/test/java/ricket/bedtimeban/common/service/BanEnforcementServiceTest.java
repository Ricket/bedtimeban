package ricket.bedtimeban.common.service;

import ricket.bedtimeban.common.localization.BedtimeTranslator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ricket.bedtimeban.common.persistence.BedtimeStateStore;
import ricket.bedtimeban.core.model.ScheduledBanRecord;
import ricket.bedtimeban.core.persistence.BedtimeStateCodec;
import ricket.bedtimeban.core.service.BedtimeDomainService;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BanEnforcementServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void unbansAndDeletesAfterEnd() throws Exception {
        UUID userId = UUID.randomUUID();
        BedtimeRepository repository = repository();
        repository.putScheduledBan(new ScheduledBanRecord(userId, null, Instant.parse("2026-06-19T22:29:00Z"), "Bedtime", 3));
        FakeAccess access = new FakeAccess();
        BanEnforcementService service = new BanEnforcementService(
            repository,
            new BedtimeDomainService(Clock.systemUTC()),
            new BedtimeMessagingService(repository, new BedtimeDomainService(Clock.systemUTC()), new BedtimeTranslator()),
            Clock.fixed(Instant.parse("2026-06-19T22:30:00Z"), ZoneOffset.UTC)
        );

        service.tick(access, message -> {});

        assertTrue(access.unbanned.contains(userId));
        assertFalse(repository.hasScheduledBan(userId));
    }

    @Test
    void successfulBanClearsStart() throws Exception {
        UUID userId = UUID.randomUUID();
        BedtimeRepository repository = repository();
        repository.putScheduledBan(new ScheduledBanRecord(userId, Instant.parse("2026-06-19T22:29:00Z"), Instant.parse("2026-06-20T06:29:00Z"), "Bedtime", 0));
        FakeAccess access = new FakeAccess();
        BanEnforcementService service = new BanEnforcementService(
            repository,
            new BedtimeDomainService(Clock.systemUTC()),
            new BedtimeMessagingService(repository, new BedtimeDomainService(Clock.systemUTC()), new BedtimeTranslator()),
            Clock.fixed(Instant.parse("2026-06-19T22:30:00Z"), ZoneOffset.UTC)
        );

        service.tick(access, message -> {});

        assertTrue(access.banned.contains(userId));
        assertEquals("Bedtime", access.banReason);
        assertEquals("Good night!", access.disconnectMessage);
        assertNull(repository.getScheduledBan(userId).start());
    }

    @Test
    void alreadyBannedLeavesStartIntact() throws Exception {
        UUID userId = UUID.randomUUID();
        BedtimeRepository repository = repository();
        repository.putScheduledBan(new ScheduledBanRecord(userId, Instant.parse("2026-06-19T22:29:00Z"), Instant.parse("2026-06-20T06:29:00Z"), "Bedtime", 0));
        FakeAccess access = new FakeAccess();
        access.banResult = false;
        BanEnforcementService service = new BanEnforcementService(
            repository,
            new BedtimeDomainService(Clock.systemUTC()),
            new BedtimeMessagingService(repository, new BedtimeDomainService(Clock.systemUTC()), new BedtimeTranslator()),
            Clock.fixed(Instant.parse("2026-06-19T22:30:00Z"), ZoneOffset.UTC)
        );

        service.tick(access, message -> {});

        assertEquals(Instant.parse("2026-06-19T22:29:00Z"), repository.getScheduledBan(userId).start());
    }

    @Test
    void onlineWarningSendsMessageAndIncrementsCounter() throws Exception {
        UUID userId = UUID.randomUUID();
        BedtimeRepository repository = repository();
        repository.putScheduledBan(new ScheduledBanRecord(userId, Instant.parse("2026-06-19T22:40:00Z"), Instant.parse("2026-06-20T06:40:00Z"), "Bedtime", 1));
        FakeAccess access = new FakeAccess();
        access.onlineUsers.add(userId);
        BanEnforcementService service = new BanEnforcementService(
            repository,
            new BedtimeDomainService(Clock.systemUTC()),
            new BedtimeMessagingService(repository, new BedtimeDomainService(Clock.systemUTC()), new BedtimeTranslator()),
            Clock.fixed(Instant.parse("2026-06-19T22:36:00Z"), ZoneOffset.UTC)
        );

        service.tick(access, message -> {});

        assertEquals(List.of("5 minutes until bedtime!"), access.messages);
        assertEquals(2, repository.getScheduledBan(userId).warningsSent());
    }

    @Test
    void offlineWarningStillIncrementsCounter() throws Exception {
        UUID userId = UUID.randomUUID();
        BedtimeRepository repository = repository();
        repository.putScheduledBan(new ScheduledBanRecord(userId, Instant.parse("2026-06-19T22:40:00Z"), Instant.parse("2026-06-20T06:40:00Z"), "Bedtime", 1));
        FakeAccess access = new FakeAccess();
        BanEnforcementService service = new BanEnforcementService(
            repository,
            new BedtimeDomainService(Clock.systemUTC()),
            new BedtimeMessagingService(repository, new BedtimeDomainService(Clock.systemUTC()), new BedtimeTranslator()),
            Clock.fixed(Instant.parse("2026-06-19T22:36:00Z"), ZoneOffset.UTC)
        );

        service.tick(access, message -> {});

        assertTrue(access.messages.isEmpty());
        assertEquals(2, repository.getScheduledBan(userId).warningsSent());
    }

    @Test
    void shortLeadTimeOnlySendsOneMinuteWarning() throws Exception {
        UUID userId = UUID.randomUUID();
        BedtimeRepository repository = repository();
        repository.putScheduledBan(new ScheduledBanRecord(userId, Instant.parse("2026-06-19T22:30:00Z"), Instant.parse("2026-06-20T06:30:00Z"), "Bedtime", 2));
        FakeAccess access = new FakeAccess();
        access.onlineUsers.add(userId);
        BanEnforcementService service = new BanEnforcementService(
            repository,
            new BedtimeDomainService(Clock.systemUTC()),
            new BedtimeMessagingService(repository, new BedtimeDomainService(Clock.systemUTC()), new BedtimeTranslator()),
            Clock.fixed(Instant.parse("2026-06-19T22:29:30Z"), ZoneOffset.UTC)
        );

        service.tick(access, message -> {});

        assertEquals(List.of("1 minute until bedtime!"), access.messages);
        assertEquals(3, repository.getScheduledBan(userId).warningsSent());
    }

    private BedtimeRepository repository() throws Exception {
        BedtimeRepository repository = new BedtimeRepository(new BedtimeStateStore(new BedtimeStateCodec()));
        repository.attachStore(tempDir.resolve(UUID.randomUUID() + ".json"));
        return repository;
    }

    private static final class FakeAccess implements BedtimeServerAccess {
        private final List<UUID> banned = new ArrayList<>();
        private final List<UUID> unbanned = new ArrayList<>();
        private final List<UUID> onlineUsers = new ArrayList<>();
        private final List<String> messages = new ArrayList<>();
        private boolean banResult = true;
        private String banReason;
        private String disconnectMessage;

        @Override
        public boolean ban(UUID playerUuid, Instant start, Instant end, String reason, String disconnectMessage) {
            if (banResult) {
                banned.add(playerUuid);
                banReason = reason;
                this.disconnectMessage = disconnectMessage;
            }
            return banResult;
        }

        @Override
        public void unban(UUID playerUuid) {
            unbanned.add(playerUuid);
        }

        @Override
        public boolean isPlayerOnline(UUID playerUuid) {
            return onlineUsers.contains(playerUuid);
        }

        @Override
        public void sendSystemMessage(UUID playerUuid, String message) {
            messages.add(message);
        }

        @Override
        public String uuidToDisplayName(UUID playerUuid) {
            return playerUuid.toString();
        }

        @Override
        public Optional<UUID> resolvePlayerUuid(String usernameOrUuid) {
            return Optional.empty();
        }

        @Override
        public Optional<String> getPlayerLocale(UUID playerUuid) {
            return Optional.of("en_us");
        }
    }
}
