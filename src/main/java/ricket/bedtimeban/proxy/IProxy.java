package ricket.bedtimeban.proxy;

import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

public interface IProxy {
    void preInit(FMLPreInitializationEvent event);
    void serverStarting(FMLServerStartingEvent event);
    void serverStarted(FMLServerStartedEvent event);
}
