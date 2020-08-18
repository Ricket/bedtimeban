package ricket.bedtimeban;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import ricket.bedtimeban.network.TimezoneRequestMessage;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class BanHammer implements Runnable, Callable<Void> {

    private final ScheduledExecutorService executor;
    private final MinecraftServer server;
    private final MinecraftServerBanUtils banUtils;
    private final BanScheduler banScheduler;
    private final PlayerTimeZones playerTimeZones;

    @VisibleForTesting
    Clock clock = Clock.systemUTC();

    public void start() {
        executor.scheduleAtFixedRate(this, 0, 60, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        // This is called within the BanHammer thread, careful not to do Minecraft things here.
        try {
            // TODO we should precompute more of the stuff from the call() method to save main thread time

            ListenableFuture<Void> future = server.callFromMainThread(this);
            future.get();
        } catch (Exception e) {
            BedtimeBanMod.logger.error("Caught exception from BanHammer run", e);
        }
    }

    @Override
    public Void call() throws Exception {
        // This is called from the Minecraft main thread, we can do things with MinecraftServer here
        Instant now = Instant.now(clock);

        Map<UUID, ScheduledBan> scheduledBans = banScheduler.getScheduledBans();
        boolean needConfigSync = false;
        for (Entry<UUID, ScheduledBan> entry : scheduledBans.entrySet()) {
            UUID uuid = entry.getKey();
            String playerName = banUtils.uuidToPlayerName(uuid);
            try {
                ScheduledBan scheduledBan = entry.getValue();

                if (scheduledBan.getEnd() != null && now.isAfter(scheduledBan.getEnd())) {
                    banUtils.unban(uuid);
                    BedtimeBanMod.logger.info("Unbanned " + playerName);
                    banScheduler.clearScheduledBan(uuid);
                } else if (scheduledBan.getStart() != null && now.isAfter(scheduledBan.getStart())) {
                    if (banUtils.ban(uuid)) {
                        BedtimeBanMod.logger.info("Banned " + playerName);
                        scheduledBan.setStart(null);
                        banScheduler.updateBan(uuid, scheduledBan);
                    }
                } else if (scheduledBan.getWarningsSent() < BanWarning.values().length) {
                    BanWarning nextWarning = BanWarning.values()[scheduledBan.getWarningsSent()];
                    Instant nextWarningInstant = scheduledBan.getStart().minus(nextWarning.amount, nextWarning.unit);
                    if (now.isAfter(nextWarningInstant)) {
                        EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(uuid);
                        if (player != null) {
                            player.sendMessage(new TextComponentString(nextWarning.toUserString() + " until bedtime!"));
                        }
                        scheduledBan.setWarningsSent(nextWarning.ordinal() + 1);
                        banScheduler.updateBan(uuid, scheduledBan);
                    }
                }
            } catch (Exception e) {
                BedtimeBanMod.logger.error("Error processing scheduled ban data for " + playerName, e);
            }
        }

        // Also check that we have time zone data for all the logged-in players. Sometimes the network packet gets lost.
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            if (playerTimeZones.getTimezone(player.getUniqueID()) == null) {
                BedtimeBanMod.networkWrapper.sendTo(TimezoneRequestMessage.instance, player);
            }
        }

        return null;
    }
}
