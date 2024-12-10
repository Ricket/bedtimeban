package ricket.bedtimeban;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Instant;

public class InstantSerializer extends TypeAdapter<Instant> {
    @Override
    public void write(JsonWriter out, Instant value) throws IOException {
        out.value(value == null ? null : value.toString());
    }

    @Override
    public Instant read(JsonReader in) throws IOException {
        String str = in.nextString();
        return str == null ? null : Instant.parse(str);
    }
}
