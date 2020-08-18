package ricket.bedtimeban.network;

import lombok.RequiredArgsConstructor;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import ricket.bedtimeban.BedtimeBanMod;
import ricket.bedtimeban.PlayerTimeZones;

import java.time.format.TextStyle;
import java.util.Locale;
import java.util.UUID;

@RequiredArgsConstructor
public class TimezoneMessageHandler implements IMessageHandler<TimezoneMessage, IMessage> {

    private final PlayerTimeZones playerTimeZones;

    public TimezoneMessageHandler() {
        // client side calls this for some dumb reason
        playerTimeZones = null;
    }

    @Override
    public IMessage onMessage(TimezoneMessage message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        UUID playerUuid = player.getUniqueID();

        BedtimeBanMod.logger.info(player.getDisplayNameString() + " local timezone is: "
                + message.getTimeZone().getDisplayName(TextStyle.FULL, Locale.getDefault()));

        playerTimeZones.setTimeZone(playerUuid, message.getTimeZone());

        return null;
    }
}
