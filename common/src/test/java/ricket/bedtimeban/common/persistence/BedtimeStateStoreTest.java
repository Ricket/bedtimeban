package ricket.bedtimeban.common.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ricket.bedtimeban.core.model.PlayerTimezoneRecord;
import ricket.bedtimeban.core.persistence.BedtimeState;
import ricket.bedtimeban.core.persistence.BedtimeStateCodec;

import java.nio.file.Path;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BedtimeStateStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void savesAndLoadsState() throws Exception {
        BedtimeStateStore store = new BedtimeStateStore(new BedtimeStateCodec());
        store.setStateFile(tempDir.resolve("serverconfig").resolve("bedtimeban-state.json"));

        UUID userId = UUID.randomUUID();
        BedtimeState state = new BedtimeState(Map.of(userId, new PlayerTimezoneRecord(userId, ZoneId.of("UTC"))), Map.of());
        store.save(state);

        assertEquals(state, store.load());
    }
}

