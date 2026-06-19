package ricket.bedtimeban.core.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ricket.bedtimeban.core.model.PlayerTimezoneRecord;
import ricket.bedtimeban.core.model.ScheduledBanRecord;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BedtimeStateCodec {
    private final Gson gson = new GsonBuilder().create();

    public String encode(BedtimeState state) {
        JsonObject root = new JsonObject();
        JsonArray timezones = new JsonArray();
        state.timezones().values().stream()
            .sorted(Comparator.comparing(PlayerTimezoneRecord::playerUuid))
            .map(this::toJson)
            .forEach(timezones::add);
        JsonArray scheduledBans = new JsonArray();
        state.scheduledBans().values().stream()
            .sorted(Comparator.comparing(ScheduledBanRecord::playerUuid))
            .map(this::toJson)
            .forEach(scheduledBans::add);
        root.add("timezones", timezones);
        root.add("scheduledBans", scheduledBans);
        return gson.toJson(root);
    }

    public BedtimeState decode(String json) {
        if (json == null || json.isBlank()) {
            return BedtimeState.empty();
        }

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (root == null) {
            return BedtimeState.empty();
        }

        Map<UUID, PlayerTimezoneRecord> timezones = new HashMap<>();
        forEachTimezone(root.getAsJsonArray("timezones"), record -> timezones.put(record.playerUuid(), record));

        Map<UUID, ScheduledBanRecord> scheduledBans = new HashMap<>();
        forEachScheduledBan(root.getAsJsonArray("scheduledBans"), record -> scheduledBans.put(record.playerUuid(), record));

        return new BedtimeState(timezones, scheduledBans);
    }

    private JsonObject toJson(PlayerTimezoneRecord record) {
        JsonObject object = new JsonObject();
        object.addProperty("playerUuid", record.playerUuid().toString());
        object.addProperty("zoneId", record.zoneId().getId());
        return object;
    }

    private JsonObject toJson(ScheduledBanRecord record) {
        JsonObject object = new JsonObject();
        object.addProperty("playerUuid", record.playerUuid().toString());
        if (record.start() != null) {
            object.addProperty("start", record.start().toString());
        }
        if (record.end() != null) {
            object.addProperty("end", record.end().toString());
        }
        object.addProperty("reason", record.reason());
        object.addProperty("warningsSent", record.warningsSent());
        return object;
    }

    private void forEachTimezone(JsonArray array, ThrowingConsumer<PlayerTimezoneRecord> consumer) {
        if (array == null) {
            return;
        }
        for (JsonElement element : array) {
            try {
                JsonObject object = element.getAsJsonObject();
                consumer.accept(new PlayerTimezoneRecord(
                    UUID.fromString(object.get("playerUuid").getAsString()),
                    ZoneId.of(object.get("zoneId").getAsString())
                ));
            } catch (RuntimeException ex) {
                // Ignore invalid persisted entries.
            }
        }
    }

    private void forEachScheduledBan(JsonArray array, ThrowingConsumer<ScheduledBanRecord> consumer) {
        if (array == null) {
            return;
        }
        for (JsonElement element : array) {
            try {
                JsonObject object = element.getAsJsonObject();
                consumer.accept(new ScheduledBanRecord(
                    UUID.fromString(object.get("playerUuid").getAsString()),
                    object.has("start") && !object.get("start").isJsonNull() ? Instant.parse(object.get("start").getAsString()) : null,
                    object.has("end") && !object.get("end").isJsonNull() ? Instant.parse(object.get("end").getAsString()) : null,
                    object.get("reason").getAsString(),
                    object.get("warningsSent").getAsInt()
                ));
            } catch (RuntimeException ex) {
                // Ignore invalid persisted entries.
            }
        }
    }

    @FunctionalInterface
    private interface ThrowingConsumer<T> {
        void accept(T value);
    }
}
