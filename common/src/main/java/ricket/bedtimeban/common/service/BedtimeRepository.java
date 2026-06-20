package ricket.bedtimeban.common.service;

import ricket.bedtimeban.common.persistence.BedtimeStateStore;
import ricket.bedtimeban.core.model.PlayerLocaleRecord;
import ricket.bedtimeban.core.model.PlayerTimezoneRecord;
import ricket.bedtimeban.core.model.ScheduledBanRecord;
import ricket.bedtimeban.core.persistence.BedtimeState;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BedtimeRepository {
    private final BedtimeStateStore store;
    private final Map<UUID, PlayerTimezoneRecord> timezones = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerLocaleRecord> locales = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledBanRecord> scheduledBans = new ConcurrentHashMap<>();

    public BedtimeRepository(BedtimeStateStore store) {
        this.store = store;
    }

    public synchronized void attachStore(Path stateFile) throws IOException {
        store.setStateFile(stateFile);
        BedtimeState state = store.load();
        timezones.clear();
        timezones.putAll(state.timezones());
        locales.clear();
        locales.putAll(state.locales());
        scheduledBans.clear();
        scheduledBans.putAll(state.scheduledBans());
    }

    public PlayerTimezoneRecord getTimezone(UUID playerUuid) {
        return timezones.get(playerUuid);
    }

    public synchronized void putTimezone(PlayerTimezoneRecord timezoneRecord) throws IOException {
        timezones.put(timezoneRecord.playerUuid(), timezoneRecord);
        save();
    }

    public PlayerLocaleRecord getLocale(UUID playerUuid) {
        return locales.get(playerUuid);
    }

    public synchronized void putLocale(PlayerLocaleRecord localeRecord) throws IOException {
        locales.put(localeRecord.playerUuid(), localeRecord);
        save();
    }

    public ScheduledBanRecord getScheduledBan(UUID playerUuid) {
        return scheduledBans.get(playerUuid);
    }

    public boolean hasScheduledBan(UUID playerUuid) {
        return scheduledBans.containsKey(playerUuid);
    }

    public synchronized void putScheduledBan(ScheduledBanRecord scheduledBanRecord) throws IOException {
        scheduledBans.put(scheduledBanRecord.playerUuid(), scheduledBanRecord);
        save();
    }

    public synchronized boolean removeScheduledBan(UUID playerUuid) throws IOException {
        ScheduledBanRecord removed = scheduledBans.remove(playerUuid);
        if (removed == null) {
            return false;
        }
        save();
        return true;
    }

    public Map<UUID, ScheduledBanRecord> getScheduledBans() {
        return Map.copyOf(scheduledBans);
    }

    public synchronized BedtimeState snapshot() {
        return new BedtimeState(timezones, locales, scheduledBans);
    }

    private void save() throws IOException {
        store.save(snapshot());
    }
}
