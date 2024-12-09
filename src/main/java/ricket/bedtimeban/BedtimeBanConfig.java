package ricket.bedtimeban;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import lombok.Getter;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.slf4j.Logger;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.time.ZoneId;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = BedtimeBanMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BedtimeBanConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new InstantSerializer())
            .create();

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<String> COMMAND_BEDTIME = BUILDER
            .comment("Command name for setting up bedtime")
            .define("commandName.bedtime", "bedtime");

    private static final ForgeConfigSpec.ConfigValue<String> COMMAND_CANCEL = BUILDER
            .comment("Command name for canceling a scheduled ban (OP only)")
            .define("commandName.cancel", "cancelbedtime");

    private static final ForgeConfigSpec.ConfigValue<String> COMMAND_SET_TIMEZONE = BUILDER
            .comment("Command name for a player setting their timezone")
            .define("commandName.setTimezone", "setmytimezone");

    private static final ForgeConfigSpec.ConfigValue<Map<String, String>> TIMEZONES = BUILDER
            .comment("Player timezones by player uuid")
            .define("timezones", Map.of());

    private static final ForgeConfigSpec.ConfigValue<Map<String, String>> SCHEDULED_BANS = BUILDER
            .comment("Scheduled player bans by player uuid")
            .define("scheduledBans", Map.of());

    static final ForgeConfigSpec SPEC = BUILDER.build();

    @Getter
    private String commandBedtime;
    @Getter
    private String commandCancel;
    @Getter
    private String commandSetTimezone;
    private ConcurrentMap<UUID, ZoneId> timezones = new ConcurrentHashMap<>();
    private ConcurrentMap<UUID, ScheduledBan> scheduledBans = new ConcurrentHashMap<>();

    @SubscribeEvent
    void onLoad(final ModConfigEvent event)
    {
        LOGGER.info("loading config");
        commandBedtime = COMMAND_BEDTIME.get();
        commandCancel = COMMAND_CANCEL.get();
        commandSetTimezone = COMMAND_SET_TIMEZONE.get();

        timezones = TIMEZONES.get().entrySet().stream()
                .map(e -> {
                    UUID playerUuid = UUID.fromString(e.getKey());
                    return new AbstractMap.SimpleEntry<>(playerUuid, stringToZoneId(playerUuid, e.getValue()));
                })
                .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue));

        scheduledBans = SCHEDULED_BANS.get().entrySet().stream()
                .filter(e -> !Strings.isNullOrEmpty(e.getValue()))
                .map(e -> {
                    UUID key = UUID.fromString(e.getKey());
                    return new AbstractMap.SimpleEntry<>(key, jsonToScheduledBan(key, e.getValue()));
                })
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private ZoneId stringToZoneId(UUID playerUuid, String timeZoneId)
    {
        if (Strings.isNullOrEmpty(timeZoneId)) {
            return null;
        }
        try {
            return ZoneId.of(timeZoneId);
        } catch (Exception e) {
            LOGGER.warn("Invalid config: player {} has unknown ZoneId {}", playerUuid, timeZoneId, e);
            return null;
        }
    }

    private ScheduledBan jsonToScheduledBan(UUID key, String json) {
        try {
            return gson.fromJson(json, ScheduledBan.class);
        } catch (Exception e) {
            LOGGER.warn("Corrupted json for {}, removing ban data", key);
            clearScheduledBan(key);
            return null;
        }
    }

    @CheckForNull
    public ZoneId getTimezone(UUID playerUuid)
    {
        return timezones.get(playerUuid);
    }

    public synchronized void putTimezone(UUID playerUuid, ZoneId timezone)
    {
        timezones.put(playerUuid, timezone);
        Map<String, String> configTimezones = TIMEZONES.get();
        configTimezones.put(playerUuid.toString(), timezone.getId());
        TIMEZONES.set(configTimezones);
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
        Map<String, String> configScheduledBans = SCHEDULED_BANS.get();
        if (scheduledBan == null) {
            scheduledBans.remove(playerUuid);
            configScheduledBans.remove(playerUuid.toString());
        } else {
            scheduledBans.put(playerUuid, scheduledBan);
            configScheduledBans.put(playerUuid.toString(), gson.toJson(scheduledBan));
        }
        SCHEDULED_BANS.set(configScheduledBans);
        SCHEDULED_BANS.save();
    }

    public boolean clearScheduledBan(UUID uuid) {
        ScheduledBan scheduledBan = getScheduledBan(uuid);
        if (scheduledBan == null) {
            return false;
        }

        setScheduledBan(uuid, null);
        return true;
    }
}
