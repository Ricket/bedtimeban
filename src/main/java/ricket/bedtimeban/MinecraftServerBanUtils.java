package ricket.bedtimeban;

import com.mojang.authlib.GameProfile;
import lombok.RequiredArgsConstructor;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.server.management.UserListBans;
import net.minecraft.server.management.UserListBansEntry;
import net.minecraft.util.text.TextComponentString;

import java.util.UUID;

@RequiredArgsConstructor
public class MinecraftServerBanUtils {

    private final MinecraftServer server;

    /**
     * @return true if the player is now banned; false if the player was already banned (or error)
     */
    public boolean ban(UUID uuid) {
        // This flow was largely copied from CommandBanPlayer

        GameProfile gameprofile = server.getPlayerProfileCache().getProfileByUUID(uuid);
        if (gameprofile == null) {
            throw new RuntimeException("Could not find GameProfile for player " + uuidToPlayerName(uuid));
        }

        PlayerList playerList = server.getPlayerList();
        UserListBans bannedPlayers = playerList.getBannedPlayers();
        if (bannedPlayers.isBanned(gameprofile)) {
            return false;
        }

        UserListBansEntry entry = new UserListBansEntry(gameprofile, null, "BedtimeBan", null, "Bed time!");
        bannedPlayers.addEntry(entry);

        EntityPlayerMP entityPlayer = playerList.getPlayerByUUID(uuid);
        if (entityPlayer != null) {
            entityPlayer.connection.disconnect(new TextComponentString("Good night!"));
        }

        return true;
    }

    public void unban(UUID uuid) {
        // This flow was copied from CommandPardonPlayer

        GameProfile gameprofile = server.getPlayerProfileCache().getProfileByUUID(uuid);
        if (gameprofile == null) {
            throw new RuntimeException("Could not find GameProfile for player " + uuidToPlayerName(uuid));
        }

        server.getPlayerList().getBannedPlayers().removeEntry(gameprofile);
    }

    public String uuidToPlayerName(UUID uuid) {
        EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(uuid);
        if (player == null) {
            return uuid.toString();
        }
        return player.getName();
    }
}
