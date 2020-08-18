package ricket.bedtimeban.proxy;

import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.relauncher.Side;
import ricket.bedtimeban.BedtimeBanMod;
import ricket.bedtimeban.network.TimezoneMessage;
import ricket.bedtimeban.network.TimezoneMessageHandler;
import ricket.bedtimeban.network.TimezoneRequestHandler;
import ricket.bedtimeban.network.TimezoneRequestMessage;

public class ClientProxy implements IProxy {
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        BedtimeBanMod.networkWrapper.registerMessage(new TimezoneRequestHandler(), TimezoneRequestMessage.class, 1, Side.CLIENT);
        BedtimeBanMod.networkWrapper.registerMessage(TimezoneMessageHandler.class, TimezoneMessage.class, 2, Side.SERVER);
    }

    @Override
    public void serverStarting(FMLServerStartingEvent event) {
    }

    @Override
    public void serverStarted(FMLServerStartedEvent event) {
    }
}
