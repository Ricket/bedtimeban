package ricket.bedtimeban.forge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import ricket.bedtimeban.common.BedtimeBanCommon;

@Mod(BedtimeBanForgeMod.MOD_ID)
public final class BedtimeBanForgeMod {
    public static final String MOD_ID = "bedtimeban";

    public BedtimeBanForgeMod() {
        BedtimeBanCommon common = new BedtimeBanCommon();
        ForgeServerEvents events = new ForgeServerEvents(common);
        MinecraftForge.EVENT_BUS.register(events);
    }
}

