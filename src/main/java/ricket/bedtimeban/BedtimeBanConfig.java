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
    public PlayerTimezone getTimezone(UUID playerUuid)
    {
        return timezones.get(playerUuid);
    }

    public synchronized void putTimezone(UUID playerUuid, PlayerTimezone timezone)
    {
        Preconditions.checkArgument(playerUuid.equals(timezone.getPlayerUuid()));

        timezones.put(playerUuid, timezone);

        TIMEZONES.set(timezones.values().stream()
                .sorted(Comparator.comparing(PlayerTimezone::getPlayerUuid))
                .map(gson::toJson)
                .collect(Collectors.toList()));
        TIMEZONES.save();
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
        Preconditions.checkArgument(scheduledBan == null || playerUuid.equals(scheduledBan.getPlayerUuid()));

        if (scheduledBan == null) {
            scheduledBans.remove(playerUuid);
        } else {
            scheduledBans.put(playerUuid, scheduledBan);
        }

        SCHEDULED_BANS.set(scheduledBans.values().stream()
                .sorted(Comparator.comparing(ScheduledBan::getPlayerUuid))
                .map(gson::toJson)
                .collect(Collectors.toList()));
        SCHEDULED_BANS.save();
    }

    public synchronized boolean clearScheduledBan(UUID uuid) {
        ScheduledBan scheduledBan = getScheduledBan(uuid);
        if (scheduledBan == null) {
            return false;
        }

        setScheduledBan(uuid, null);
        return true;
    }
}
