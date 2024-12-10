package ricket.bedtimeban;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.slf4j.Logger;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = BedtimeBanMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BedtimeBanConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new InstantSerializer())
            .registerTypeHierarchyAdapter(ZoneId.class, new ZoneIdSerializer())
            .create();

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> TIMEZONES = BUILDER
            .comment("Player timezones by player uuid")
            .defineListAllowEmpty("timezones", List.of(), o -> true);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SCHEDULED_BANS = BUILDER
            .comment("Scheduled player bans by player uuid")
            .defineListAllowEmpty("scheduledBans", List.of(), o -> true);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    private ConcurrentMap<UUID, PlayerTimezone> timezones = new ConcurrentHashMap<>();
    private ConcurrentMap<UUID, ScheduledBan> scheduledBans = new ConcurrentHashMap<>();

    @SubscribeEvent
    void onLoad(final ModConfigEvent event)
    {
        LOGGER.info("loading config");

        timezones = TIMEZONES.get().stream()
                .map(json -> {
                    try {
                        return gson.fromJson(json, PlayerTimezone.class);
                    } catch (Exception e) {
                        LOGGER.warn("Bad json in timezones config: {}", json);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(playerTimezone -> new AbstractMap.SimpleEntry<>(playerTimezone.getPlayerUuid(), playerTimezone))
                .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue));

        scheduledBans = SCHEDULED_BANS.get().stream()
                .map(json -> {
                    try {
                        return gson.fromJson(json, ScheduledBan.class);
                    } catch (Exception e) {
                        LOGGER.warn("Bad json in scheduledBans config: {}", json);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(scheduledBan -> new AbstractMap.SimpleEntry<>(scheduledBan.getPlayerUuid(), scheduledBan))
                .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @CheckForNull
    private PlayerTimezone jsonToPlayerTimezone(UUID playerUuid, String json)
    {
        try {
            return gson.fromJson(json, PlayerTimezone.class);
        } catch (Exception e) {
            LOGGER.warn("Corrupted json for {}", playerUuid);
            return null;
        }
    }

    @CheckForNull
    private ScheduledBan jsonToScheduledBan(UUID playerUuid, String json) {
        try {
            return gson.fromJson(json, ScheduledBan.class);
        } catch (Exception e) {
            LOGGER.warn("Corrupted json for {}", playerUuid);
            clearScheduledBan(playerUuid);
            return null;
        }
    }

    @CheckForNull
    public PlayerTimezone getTimezone(UUID playerUuid)
    {
        return timezones.get(playerUuid);
    }

    public synchronized void putTimezone(UUID playerUuid, PlayerTimezone timezone)
    {
        timezones.put(playerUuid, timezone);
        // TODO save into the TIMEZONES config
//        Map<String, String> configTimezones = TIMEZONES.get();
//        configTimezones.put(playerUuid.toString(), timezone.getId());
//        TIMEZONES.set(configTimezones);
//        TIMEZONES.save();
    }

    @CheckForNull
    public ScheduledBan getScheduledBan(UUID playerUuid)
    {
        return scheduledBans.get(playerUuid);
    }

    public Map<UUID, ScheduledBan> getScheduledBans()
    {
        return Collections.unmodifiableMap(scheduledBans);
    }

    public synchronized void setScheduledBan(UUID playerUuid, @Nullable ScheduledBan scheduledBan)
    {
        if (scheduledBan == null)
        {
            clearScheduledBan(playerUuid);
            return;
        }

        Preconditions.checkArgument(scheduledBan == null || playerUuid.equals(scheduledBan.getPlayerUuid()));
        // TODO save into the SCHEDULED_BANS config and also scheduledBans map
//        List<String> configScheduledBans = SCHEDULED_BANS.get();
//        if (scheduledBan == null) {
//            scheduledBans.remove(playerUuid);
//
//            configScheduledBans.remove(playerUuid.toString());
//        } else {
//            scheduledBans.put(playerUuid, scheduledBan);
//            configScheduledBans.put(playerUuid.toString(), gson.toJson(scheduledBan));
//        }
//        SCHEDULED_BANS.set(configScheduledBans);
//        SCHEDULED_BANS.save();
    }

    public synchronized boolean clearScheduledBan(UUID uuid) {
        ScheduledBan scheduledBan = getScheduledBan(uuid);
        if (scheduledBan == null) {
            return false;
        }

        // TODO find and remove the entry from SCHEDULED_BANS and also remove from scheduledBans member
//        List<String> configScheduledBans = SCHEDULED_BANS.get();


        return true;
    }
}
