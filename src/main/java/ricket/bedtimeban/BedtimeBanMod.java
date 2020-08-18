package ricket.bedtimeban;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import org.apache.logging.log4j.Logger;
import ricket.bedtimeban.proxy.IProxy;

@Mod(modid = BedtimeBanMod.MODID, name = BedtimeBanMod.NAME, version = BedtimeBanMod.VERSION)
public class BedtimeBanMod
{
    public static final String MODID = "bedtimeban";
    public static final String NAME = "Bedtime Ban";
    public static final String VERSION = "1.0";

    public static Logger logger;

    @Instance(MODID)
    public static BedtimeBanMod instance;

    @SidedProxy(clientSide = "ricket.bedtimeban.proxy.ClientProxy", serverSide = "ricket.bedtimeban.proxy.ServerProxy")
    public static IProxy proxy;

    public static SimpleNetworkWrapper networkWrapper;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        logger.info("Hello world!");

        networkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
        proxy.preInit(event);
        MinecraftForge.EVENT_BUS.register(proxy);
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }

    @EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        proxy.serverStarted(event);
    }
}
