package ricket.bedtimeban;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZoneId;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor
public class PlayerTimezone {
    private UUID playerUuid;
    private ZoneId zoneId;
}
