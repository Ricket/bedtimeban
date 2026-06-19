package ricket.bedtimeban.neoforge;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import ricket.bedtimeban.common.BedtimeBanCommon;

import java.nio.file.Path;

public final class NeoForgeServerEvents {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    private final BedtimeBanCommon common;
    private final BedtimeCommandRegistrar commandRegistrar;

    public NeoForgeServerEvents(BedtimeBanCommon common) {
        this.common = common;
        this.commandRegistrar = new BedtimeCommandRegistrar(common.repository(), common.domainService());
    }

    @SubscribeEvent
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        Path stateFile = event.getServer().getServerDirectory().resolve("serverconfig").resolve("bedtimeban-state.json");
        common.initialize(stateFile);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        commandRegistrar.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        common.tick(new ServerBanAccess(event.getServer()), LOGGER::error);
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().getServer() != null) {
            common.onPlayerLogin(new ServerBanAccess(event.getEntity().getServer()), event.getEntity().getUUID());
        }
    }
}
