package ricket.bedtimeban.proxy;

import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import ricket.bedtimeban.BanHammer;
import ricket.bedtimeban.BanScheduler;
import ricket.bedtimeban.commands.BedtimeBanCommands;

public class ServerProxy implements IProxy {

    private final BanScheduler banScheduler;
    private BanHammer banHammer;
    private BedtimeBanCommands commands;

    public ServerProxy(BanScheduler banScheduler)
    {
        this.banScheduler = banScheduler;
        this.commands = new BedtimeBanCommands(banScheduler);
    }

    @Override
    public void registerCommands(RegisterCommandsEvent event) {
        commands.register(event.getDispatcher());
    }

    @Override
    public void serverStarting(ServerStartingEvent event) {
        banHammer = new BanHammer(banScheduler);
    }

    @Override
    public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        banHammer.tick(event.getServer(), event.haveTime());
    }

    @Override
    public void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        String reminder = banScheduler.makeBanReminderString(event.getEntity().getUUID());
        if (reminder != null) {
            event.getEntity().sendSystemMessage(Component.literal(reminder));
        }
    }
}
