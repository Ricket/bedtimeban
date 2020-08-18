package ricket.bedtimeban.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class TimezoneRequestMessage implements IMessage {
    public static final TimezoneRequestMessage instance = new TimezoneRequestMessage();

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }
}
