package ricket.bedtimeban.proxy;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;

public interface IProxy {
    void serverStarting(ServerStartingEvent event);
    void registerCommands(RegisterCommandsEvent event);
    void serverTick(TickEvent.ServerTickEvent event);
    void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event);
}
