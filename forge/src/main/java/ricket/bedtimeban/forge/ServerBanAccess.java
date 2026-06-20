package ricket.bedtimeban.forge;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import ricket.bedtimeban.common.service.BedtimeServerAccess;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ServerBanAccess implements BedtimeServerAccess {
    private final MinecraftServer server;

    public ServerBanAccess(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public boolean ban(UUID playerUuid, Instant start, Instant end) {
        GameProfile gameProfile = getGameProfile(playerUuid);
        PlayerList playerList = server.getPlayerList();
        UserBanList bans = playerList.getBans();
        if (bans.isBanned(gameProfile)) {
            return false;
        }

        UserBanListEntry entry = new UserBanListEntry(gameProfile, Date.from(start), "BedtimeBan", Date.from(end), "Bed time!");
        bans.add(entry);

        ServerPlayer player = playerList.getPlayer(playerUuid);
        if (player != null) {
            player.connection.disconnect(Component.literal("Good night!"));
        }
        return true;
    }

    @Override
    public void unban(UUID playerUuid) {
        GameProfile gameProfile = getGameProfile(playerUuid);
        UserBanList bans = server.getPlayerList().getBans();
        if (bans.isBanned(gameProfile)) {
            bans.remove(gameProfile);
        }
    }

    @Override
    public boolean isPlayerOnline(UUID playerUuid) {
        return server.getPlayerList().getPlayer(playerUuid) != null;
    }

    @Override
    public void sendSystemMessage(UUID playerUuid, String message) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        if (player != null) {
            player.sendSystemMessage(Component.literal(message));
        }
    }

    @Override
    public String uuidToDisplayName(UUID playerUuid) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        return player == null ? playerUuid.toString() : player.getName().getString();
    }

    @Override
    public Optional<UUID> resolvePlayerUuid(String usernameOrUuid) {
        try {
            return Optional.of(UUID.fromString(usernameOrUuid));
        } catch (IllegalArgumentException ignored) {
        }

        ServerPlayer onlinePlayer = server.getPlayerList().getPlayerByName(usernameOrUuid);
        if (onlinePlayer != null) {
            return Optional.of(onlinePlayer.getUUID());
        }

        return Objects.requireNonNull(server.getProfileCache()).get(usernameOrUuid).map(GameProfile::getId);
    }

    private GameProfile getGameProfile(UUID playerUuid) {
        return Objects.requireNonNull(server.getProfileCache()).get(playerUuid)
            .orElseThrow(() -> new IllegalStateException("Could not find GameProfile for player " + playerUuid));
    }
}

