package ricket.bedtimeban;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Comment;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Config(modid = BedtimeBanMod.MODID)
public class BedtimeBanConfig {
    @Comment("Command name for setting up bedtime")
    public static String commandBedtime = "bedtime";

    @Comment("Command name for canceling a scheduled ban (OP only)")
    public static String commandCancel = "cancelbedtime";

    @Comment("Command name for a player setting their timezone")
    public static String commandSetTimezone = "setmytimezone";

    @Comment("Player timezones by player uuid")
    public static ConcurrentMap<String, String> timezones = new ConcurrentHashMap<>();

    @Comment("Scheduled player bans by player uuid")
    public static ConcurrentMap<String, String> scheduledBans = new ConcurrentHashMap<>();
}
