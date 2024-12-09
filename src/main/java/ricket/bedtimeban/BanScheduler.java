package ricket.bedtimeban;

import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public class BanScheduler {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mma z");
    private final BedtimeBanConfig config;

    public void setTimezone(UUID uuid, ZoneId timezone) {
        config.putTimezone(uuid, timezone);
    }

    @CheckForNull
    public ZoneId getTimezone(UUID playerUuid) {
        return config.getTimezone(playerUuid);
    }

    public void scheduleBan(UUID playerUuid, Instant startTime, Instant endTime) {
        Preconditions.checkArgument(startTime.isBefore(endTime), "Expected start time " + startTime + " before end time " + endTime);

        // save to config
        ScheduledBan scheduledBan = new ScheduledBan(startTime, endTime, "Bedtime", 0);
        updateBan(playerUuid, scheduledBan);
    }

    public void updateBan(UUID playerUuid, ScheduledBan ban) {
        config.setScheduledBan(playerUuid, ban);
    }

    @CheckForNull
    public ScheduledBan getScheduledBan(UUID playerUuid) {
        return config.getScheduledBan(playerUuid);
    }

    public boolean hasScheduledBan(UUID playerUuid) {
        return getScheduledBan(playerUuid) != null;
    }

    public Map<UUID, ScheduledBan> getScheduledBans()
    {
        return config.getScheduledBans();
    }

    @CheckForNull
    public String makeBanReminderString(@Nonnull UUID playerUuid) {
        ScheduledBan scheduledBan = getScheduledBan(playerUuid);
        if (scheduledBan == null) {
            return null;
        }

        Instant start = scheduledBan.getStart();
        if (start == null) {
            return null;
        }

        ZoneId timezone = getTimezone(playerUuid);
        if (timezone == null) {
            return null;
        }

        ZonedDateTime zonedDateTime = start.atZone(timezone);

        // formattedTime will be like "11:30pm PDT"
        String formattedTime = zonedDateTime.format(TIME_FORMATTER);

        return String.format("Reminder that your bedtime is: %s", formattedTime);
    }

    public boolean clearScheduledBan(UUID uuid) {
        return config.clearScheduledBan(uuid);
    }

}
