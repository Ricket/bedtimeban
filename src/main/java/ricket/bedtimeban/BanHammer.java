package ricket.bedtimeban;

import com.google.common.util.concurrent.RateLimiter;
import com.mojang.logging.LogUtils;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

@RequiredArgsConstructor
public class BanHammer {

    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    private final BanScheduler banScheduler;

    private final Clock clock = Clock.systemUTC();

    private static final int TICKS_BETWEEN_CHECKS = 60 * 20; // 60 seconds in ticks
    private int ticksToNextCheck;
    private final RateLimiter starvationWarningRateLimit = RateLimiter.create(1.0 / 5.0);

    public void tick(MinecraftServer server, boolean haveTime)
    {
        ticksToNextCheck--;
        if (haveTime && ticksToNextCheck <= 0)
        {
            try
            {
                processBans(server);
            }
            catch (Exception e)
            {
                LOGGER.error("Caught exception from BanHammer run", e);
            }
            finally
            {
                ticksToNextCheck = TICKS_BETWEEN_CHECKS;
            }
        }
        else if (ticksToNextCheck < -TICKS_BETWEEN_CHECKS)
        {
            if (starvationWarningRateLimit.tryAcquire()) {
                LOGGER.warn("BanHammer is being starved, has not run in {} seconds", Math.round((double)(TICKS_BETWEEN_CHECKS - ticksToNextCheck) / 20.0));
            }
        }
    }

    private void processBans(MinecraftServer server) {
        Instant now = Instant.now(clock);

        Map<UUID, ScheduledBan> scheduledBans = banScheduler.getScheduledBans();
        for (Entry<UUID, ScheduledBan> entry : scheduledBans.entrySet()) {
            UUID uuid = entry.getKey();
            String playerName = MinecraftServerBanUtils.uuidToPlayerName(uuid, server);
            try {
                ScheduledBan scheduledBan = entry.getValue();

                if (scheduledBan.getEnd() != null && now.isAfter(scheduledBan.getEnd())) {
                    MinecraftServerBanUtils.unban(uuid, server);
                    LOGGER.info("Unbanned " + playerName);
                    banScheduler.clearScheduledBan(uuid);
                } else if (scheduledBan.getStart() != null && now.isAfter(scheduledBan.getStart())) {
                    if (MinecraftServerBanUtils.ban(uuid, server)) {
                        LOGGER.info("Banned " + playerName);
                        scheduledBan.setStart(null);
                        banScheduler.updateBan(uuid, scheduledBan);
                    }
                } else if (scheduledBan.getStart() != null && scheduledBan.getWarningsSent() < BanWarning.values().length) {
                    BanWarning nextWarning = BanWarning.values()[scheduledBan.getWarningsSent()];
                    Instant nextWarningInstant = scheduledBan.getStart().minus(nextWarning.amount, nextWarning.unit);
                    if (now.isAfter(nextWarningInstant)) {
                        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                        if (player != null) {
                            player.sendSystemMessage(Component.literal(nextWarning.toUserString() + " until bedtime!"));
                        }
                        scheduledBan.setWarningsSent(nextWarning.ordinal() + 1);
                        banScheduler.updateBan(uuid, scheduledBan);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error processing scheduled ban data for " + playerName, e);
            }
        }
    }
}
