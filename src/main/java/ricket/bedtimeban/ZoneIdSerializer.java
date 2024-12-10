package ricket.bedtimeban;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.ZoneId;

public class ZoneIdSerializer extends TypeAdapter<ZoneId> {
    @Override
    public void write(JsonWriter out, ZoneId value) throws IOException {
        out.value(value == null ? null : value.getId());
    }

    @Override
    public ZoneId read(JsonReader in) throws IOException {
        String str = in.nextString();
        return str == null ? null : ZoneId.of(str);
    }
}
