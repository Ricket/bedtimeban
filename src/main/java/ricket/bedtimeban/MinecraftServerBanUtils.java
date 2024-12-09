package ricket.bedtimeban;

import com.mojang.authlib.GameProfile;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;

import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
public class MinecraftServerBanUtils {

    private final MinecraftServer server;

    /**
     * @return true if the player is now banned; false if the player was already banned (or error)
     */
    public boolean ban(UUID uuid) {
        // This flow was copied from BanPlayerCommands

        GameProfile gameprofile = getGameProfile(uuid);

        PlayerList playerList = server.getPlayerList();

        UserBanList bannedPlayers = playerList.getBans();
        if (bannedPlayers.isBanned(gameprofile)) {
            return false;
        }

        UserBanListEntry entry = new UserBanListEntry(gameprofile, null, "BedtimeBan", null, "Bed time!");
        bannedPlayers.add(entry);

        ServerPlayer entityPlayer = playerList.getPlayer(uuid);
        if (entityPlayer != null) {
            entityPlayer.connection.disconnect(Component.translatable("multiplayer.bedtimeban.disconnect"));
        }

        return true;
    }

    public void unban(UUID uuid) {
        // This flow was copied from PardonCommand

        GameProfile gameprofile = getGameProfile(uuid);

        UserBanList bans = server.getPlayerList().getBans();
        if (bans.isBanned(gameprofile))
        {
            bans.remove(gameprofile);
        }
    }

    private GameProfile getGameProfile(UUID uuid)
    {
        return Objects.requireNonNull(server.getProfileCache()).get(uuid)
                .orElseThrow(() -> new RuntimeException("Could not find GameProfile for player " + uuidToPlayerName(uuid)));
    }

    public String uuidToPlayerName(UUID uuid) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player == null) {
            return uuid.toString();
        }
        return player.getName().getString();
    }
}
