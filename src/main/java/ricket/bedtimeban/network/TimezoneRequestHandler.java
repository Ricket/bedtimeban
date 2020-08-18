package ricket.bedtimeban.network;

import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import ricket.bedtimeban.BedtimeBanMod;

import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.Locale;

public class TimezoneRequestHandler implements IMessageHandler<TimezoneRequestMessage, TimezoneMessage> {
    @Override
    public TimezoneMessage onMessage(TimezoneRequestMessage message, MessageContext ctx) {
        ZoneId timezone = ZoneId.systemDefault();
        BedtimeBanMod.logger.info("Local timezone is: " + timezone.getDisplayName(TextStyle.FULL, Locale.getDefault()));
        return new TimezoneMessage(timezone);
    }
}
