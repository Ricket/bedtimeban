package ricket.bedtimeban.core.model;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public enum BanWarningThreshold {
    FIFTEEN_MINUTES(15, ChronoUnit.MINUTES),
    FIVE_MINUTES(5, ChronoUnit.MINUTES),
    ONE_MINUTE(1, ChronoUnit.MINUTES);

    private final int amount;
    private final ChronoUnit unit;

    BanWarningThreshold(int amount, ChronoUnit unit) {
        this.amount = amount;
        this.unit = unit;
    }

    public Duration toDuration() {
        return Duration.of(amount, unit);
    }

    public String toUserString() {
        String unitString = unit == ChronoUnit.MINUTES ? "minutes" : unit.toString().toLowerCase();
        if (amount == 1 && unitString.endsWith("s")) {
            unitString = unitString.substring(0, unitString.length() - 1);
        }
        return amount + " " + unitString;
    }
}
