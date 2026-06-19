package ricket.bedtimeban.neoforge;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import ricket.bedtimeban.common.BedtimeBanCommon;

@Mod(BedtimeBanNeoForgeMod.MOD_ID)
public final class BedtimeBanNeoForgeMod {
    public static final String MOD_ID = "bedtimeban";

    public BedtimeBanNeoForgeMod() {
        BedtimeBanCommon common = new BedtimeBanCommon();
        NeoForgeServerEvents events = new NeoForgeServerEvents(common);
        NeoForge.EVENT_BUS.register(events);
    }
}

