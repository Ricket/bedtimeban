package ricket.bedtimeban;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor
public class ScheduledBan {
    private UUID playerUuid;
    private Instant start;
    private Instant end;
    private String reason;

    private int warningsSent;
}
