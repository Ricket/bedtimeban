package ricket.bedtimeban.proxy;

import lombok.RequiredArgsConstructor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import ricket.bedtimeban.BanHammer;
import ricket.bedtimeban.BanScheduler;
import ricket.bedtimeban.BedtimeBanConfig;
import ricket.bedtimeban.MinecraftServerBanUtils;
import ricket.bedtimeban.commands.BedtimeBanCommands;

@RequiredArgsConstructor
public class ServerProxy implements IProxy {

    private final BedtimeBanConfig config;
    private final BanScheduler banScheduler;
    private MinecraftServerBanUtils banUtils;
    private BanHammer banHammer;
    private BedtimeBanCommands commands;

    @Override
    public void serverStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        banUtils = new MinecraftServerBanUtils(server);
        banHammer = new BanHammer(server, banUtils, banScheduler);
    }

    @Override
    public void registerCommands(RegisterCommandsEvent event) {
        commands = new BedtimeBanCommands(config, banScheduler, banUtils);
        commands.register(event.getDispatcher());
    }

    @Override
    public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        banHammer.tick(event.haveTime());
    }

    @Override
    public void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        String reminder = banScheduler.makeBanReminderString(event.getEntity().getUUID());
        if (reminder != null) {
            event.getEntity().sendSystemMessage(Component.literal(reminder));
        }
    }
}
