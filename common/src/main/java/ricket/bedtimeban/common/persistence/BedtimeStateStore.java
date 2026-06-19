package ricket.bedtimeban.common.persistence;

import ricket.bedtimeban.core.persistence.BedtimeState;
import ricket.bedtimeban.core.persistence.BedtimeStateCodec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BedtimeStateStore {
    private final BedtimeStateCodec codec;
    private Path stateFile;

    public BedtimeStateStore(BedtimeStateCodec codec) {
        this.codec = codec;
    }

    public void setStateFile(Path stateFile) {
        this.stateFile = stateFile;
    }

    public BedtimeState load() throws IOException {
        if (stateFile == null || Files.notExists(stateFile)) {
            return BedtimeState.empty();
        }
        return codec.decode(Files.readString(stateFile));
    }

    public void save(BedtimeState state) throws IOException {
        if (stateFile == null) {
            return;
        }
        Files.createDirectories(stateFile.getParent());
        Files.writeString(stateFile, codec.encode(state));
    }
}

