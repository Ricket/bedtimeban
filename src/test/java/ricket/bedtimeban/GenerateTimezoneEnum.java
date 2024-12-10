package ricket.bedtimeban;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.stream.Collectors;

public class GenerateTimezoneEnum {
    @Test
    void generate()
    {
        System.out.println(ZoneId.getAvailableZoneIds().stream()
                .sorted()
                .collect(Collectors.joining("\n")));
    }
}
