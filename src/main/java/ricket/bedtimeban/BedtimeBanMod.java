package ricket.bedtimeban;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import ricket.bedtimeban.proxy.IProxy;
import ricket.bedtimeban.proxy.ServerProxy;

@Mod(BedtimeBanMod.MODID)
public class BedtimeBanMod
{
    public static final String MODID = "bedtimeban";

    public BedtimeBanMod(FMLJavaModLoadingContext context)
    {
        MinecraftForge.EVENT_BUS.register(this);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.SERVER, BedtimeBanConfig.SPEC);

        BedtimeBanConfig configInst = new BedtimeBanConfig();

        // ModConfigEvent is an IModBusEvent
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.register(configInst);

        proxy = new ServerProxy(new BanScheduler(configInst));
    }

    public static IProxy proxy;

    @SubscribeEvent
    public void serverStarting(ServerStartingEvent event)
    {
        proxy.serverStarting(event);
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event)
    {
        proxy.registerCommands(event);
    }

    @SubscribeEvent
    void serverTick(TickEvent.ServerTickEvent event)
    {
        proxy.serverTick(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        proxy.playerLoggedIn(event);
    }
}
