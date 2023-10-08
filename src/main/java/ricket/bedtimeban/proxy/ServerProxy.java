package ricket.bedtimeban.proxy;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import ricket.bedtimeban.*;
import ricket.bedtimeban.commands.BedtimeCommand;
import ricket.bedtimeban.commands.CancelBanCommand;
import ricket.bedtimeban.commands.SetTimezoneCommand;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ServerProxy implements IProxy {

    public static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new InstantSerializer())
            .create();

    public static final ScheduledExecutorService banExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("BedtimeBanHammer").build());

    private final TimeParser timeParser = new TimeParser();
    private final BanScheduler banScheduler = new BanScheduler();
    private BanHammer banHammer;

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        // register network handlers, etc
    }

    @Override
    public void serverStarting(FMLServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        MinecraftServerBanUtils banUtils = new MinecraftServerBanUtils(server);
        banHammer = new BanHammer(banExecutor, server, banUtils, banScheduler);

        event.registerServerCommand(new BedtimeCommand(timeParser, banScheduler));
        event.registerServerCommand(new CancelBanCommand(banUtils, banScheduler));
        event.registerServerCommand(new SetTimezoneCommand(banScheduler));
    }

    @Override
    public void serverStarted(FMLServerStartedEvent event) {
        banHammer.start();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void playerLoggedIn(PlayerLoggedInEvent event) {
        String reminder = banScheduler.makeBanReminderString(event.player.getUniqueID());
        if (reminder != null) {
            event.player.sendMessage(new TextComponentString(reminder));
        }
    }

    @SubscribeEvent
    public void playerLoggedOut(PlayerLoggedOutEvent event) {
    }
}
