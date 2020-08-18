package ricket.bedtimeban.network;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.time.ZoneId;

@Data @NoArgsConstructor @AllArgsConstructor
public class TimezoneMessage implements IMessage {
    private ZoneId timeZone;

    @Override
    public void fromBytes(ByteBuf buf) {
        timeZone = ZoneId.of(ByteBufUtils.readUTF8String(buf));
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, timeZone.getId());
    }
}
