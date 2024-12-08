package ricket.bedtimeban;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data @NoArgsConstructor @AllArgsConstructor
public class ScheduledBan {
    private Instant start;
    private Instant end;
    private String reason;

    private int warningsSent;
}
