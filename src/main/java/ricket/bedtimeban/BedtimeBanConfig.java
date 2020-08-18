package ricket.bedtimeban;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Comment;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Config(modid = BedtimeBanMod.MODID)
public class BedtimeBanConfig {
    @Comment("Command name for setting up bedtime")
    public static String commandBedtime = "bedtime";

    @Comment("Command name for canceling a scheduled ban")
    public static String commandCancel = "cancelbedtime";

    @Comment("Scheduled player bans")
    public static ConcurrentMap<String, String> scheduledBans = new ConcurrentHashMap<>();
}
