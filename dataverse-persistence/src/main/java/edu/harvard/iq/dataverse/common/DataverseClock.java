package edu.harvard.iq.dataverse.common;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class DataverseClock {

    private static Clock clock = Clock.systemDefaultZone();
    public static final ZoneId zoneId = ZoneId.systemDefault();
    private static LocalDateTime lastFixed;

    public static LocalDateTime now() {
        return LocalDateTime.now(getClock());
    }

    public static void fixedAt(LocalDateTime date) {
        lastFixed = now();
        clock = Clock.fixed(date.atZone(zoneId).toInstant(), zoneId);
    }

    public static void reset() {
        if (lastFixed != null) {
            clock = Clock.fixed(lastFixed.atZone(zoneId).toInstant(), zoneId);
            lastFixed = null;
        }
    }

    public static void defaultClock() {
        clock = Clock.systemDefaultZone();
    }

    private static Clock getClock() {
        return clock;
    }
}
