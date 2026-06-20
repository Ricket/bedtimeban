package ricket.bedtimeban.forge;

import com.mojang.logging.LogUtils;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import ricket.bedtimeban.common.BedtimeBanCommon;

import java.nio.file.Path;

public final class ForgeServerEvents {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    private final BedtimeBanCommon common;
    private final BedtimeCommandRegistrar commandRegistrar;

    public ForgeServerEvents(BedtimeBanCommon common) {
        this.common = common;
        this.commandRegistrar = new BedtimeCommandRegistrar(common.repository(), common.domainService());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        Path stateFile = event.getServer().getServerDirectory().toPath().resolve("serverconfig").resolve("bedtimeban-state.json");
        common.initialize(stateFile);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        commandRegistrar.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        common.tick(new ServerBanAccess(event.getServer()), LOGGER::error);
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().getServer() != null) {
            common.onPlayerLogin(new ServerBanAccess(event.getEntity().getServer()), event.getEntity().getUUID());
        }
    }
}
