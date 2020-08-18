package ricket.bedtimeban;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import net.minecraftforge.common.config.Config.Type;
import net.minecraftforge.common.config.ConfigManager;
import ricket.bedtimeban.proxy.ServerProxy;

import javax.annotation.CheckForNull;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class BanScheduler {
    private final TimeParser timeParser;

    public void scheduleBan(UUID uuid, Instant startTime, Instant endTime) {
        Preconditions.checkArgument(startTime.isBefore(endTime), "Expected start time " + startTime + " before end time " + endTime);

        // save to config
        ScheduledBan scheduledBan = new ScheduledBan(startTime, endTime, "Bedtime", 0);
        updateBan(uuid, scheduledBan);
    }

    public void updateBan(UUID uuid, ScheduledBan ban) {
        BedtimeBanConfig.scheduledBans.put(uuid.toString(), ServerProxy.gson.toJson(ban));
        ConfigManager.sync(BedtimeBanMod.MODID, Type.INSTANCE);
    }

    @CheckForNull
    public ScheduledBan getScheduledBan(UUID uuid) {
        String json = BedtimeBanConfig.scheduledBans.get(uuid.toString());
        if (Strings.isNullOrEmpty(json)) {
            return null;
        }

        try {
            return ServerProxy.gson.fromJson(json, ScheduledBan.class);
        } catch (Exception e) {
            BedtimeBanMod.logger.warn("Invalid json for " + uuid);
            return null;
        }
    }

    public Map<UUID, ScheduledBan> getScheduledBans() {
        Map<String, String> scheduledBans = new HashMap<>(BedtimeBanConfig.scheduledBans);
        return scheduledBans.entrySet().stream()
                .filter(e -> !Strings.isNullOrEmpty(e.getValue()))
                .map(e -> new SimpleEntry<>(UUID.fromString(e.getKey()), fromJson(e.getKey(), e.getValue())))
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    private ScheduledBan fromJson(String key, String json) {
        try {
            return ServerProxy.gson.fromJson(json, ScheduledBan.class);
        } catch (Exception e) {
            BedtimeBanMod.logger.warn("Corrupted json for " + key + ", removing ban data");
            BedtimeBanConfig.scheduledBans.put(key, "");
            return null;
        }
    }

    public boolean clearScheduledBan(UUID uuid) {
        if (Strings.isNullOrEmpty(BedtimeBanConfig.scheduledBans.get(uuid.toString()))) {
            return false;
        }

        BedtimeBanConfig.scheduledBans.put(uuid.toString(), "");
        ConfigManager.sync(BedtimeBanMod.MODID, Type.INSTANCE);
        return true;
    }

}
