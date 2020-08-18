package ricket.bedtimeban.proxy;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.relauncher.Side;
import ricket.bedtimeban.BanHammer;
import ricket.bedtimeban.BanScheduler;
import ricket.bedtimeban.BedtimeBanMod;
import ricket.bedtimeban.InstantSerializer;
import ricket.bedtimeban.MinecraftServerBanUtils;
import ricket.bedtimeban.PlayerTimeZones;
import ricket.bedtimeban.TimeParser;
import ricket.bedtimeban.commands.BedtimeCommand;
import ricket.bedtimeban.commands.CancelBanCommand;
import ricket.bedtimeban.network.TimezoneMessage;
import ricket.bedtimeban.network.TimezoneMessageHandler;
import ricket.bedtimeban.network.TimezoneRequestHandler;
import ricket.bedtimeban.network.TimezoneRequestMessage;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ServerProxy implements IProxy {

    public static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new InstantSerializer())
            .create();

    public static final ScheduledExecutorService banExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("BedtimeBanHammer").build());

    private final TimeParser timeParser = new TimeParser();
    private final BanScheduler banScheduler = new BanScheduler(timeParser);
    private BanHammer banHammer;
    private final PlayerTimeZones playerTimeZones = new PlayerTimeZones();

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        BedtimeBanMod.networkWrapper.registerMessage(new TimezoneRequestHandler(), TimezoneRequestMessage.class, 1, Side.CLIENT);
        BedtimeBanMod.networkWrapper.registerMessage(new TimezoneMessageHandler(playerTimeZones), TimezoneMessage.class, 2, Side.SERVER);
    }

    @Override
    public void serverStarting(FMLServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        MinecraftServerBanUtils banUtils = new MinecraftServerBanUtils(server);
        banHammer = new BanHammer(banExecutor, server, banUtils, banScheduler, playerTimeZones);

        event.registerServerCommand(new BedtimeCommand(timeParser, banScheduler, playerTimeZones));
        event.registerServerCommand(new CancelBanCommand(banUtils, banScheduler));
    }

    @Override
    public void serverStarted(FMLServerStartedEvent event) {
        banHammer.start();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void playerLoggedIn(PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            BedtimeBanMod.networkWrapper.sendTo(TimezoneRequestMessage.instance, (EntityPlayerMP) event.player);
        } else {
            BedtimeBanMod.logger.warn("Player logged in event but not instanceof EntityPlayerMP: " + event.player);
        }
    }

    @SubscribeEvent
    public void playerLoggedOut(PlayerLoggedOutEvent event) {
        BedtimeBanMod.logger.info("Player logged out, removing timezone data: " + event.player.getUniqueID() + " / " + event.player.getDisplayNameString());
        playerTimeZones.clearPlayer(event.player.getUniqueID());
    }
}
