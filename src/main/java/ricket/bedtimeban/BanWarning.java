package ricket.bedtimeban;

import lombok.RequiredArgsConstructor;

import java.time.temporal.ChronoUnit;

@RequiredArgsConstructor
public enum BanWarning {
    FifteenMinutes(15, ChronoUnit.MINUTES),
    FiveMinutes(5, ChronoUnit.MINUTES),
    OneMinute(1, ChronoUnit.MINUTES);

    public final int amount;
    public final ChronoUnit unit;

    public String toUserString() {
        String unitStr = unit.toString().toLowerCase();
        if (amount == 1) {
            // chop the "s" off the end
            unitStr = unitStr.substring(0, unitStr.length() - 1);
        }
        return amount + " " + unitStr;
    }
}
