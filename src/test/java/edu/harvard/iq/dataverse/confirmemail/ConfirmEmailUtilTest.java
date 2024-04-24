package edu.harvard.iq.dataverse.confirmemail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.Timestamp;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConfirmEmailUtilTest {
    
    static Stream<Arguments> dataPoints() {
        return Stream.of(
            Arguments.of("48 hours", 2880),
            Arguments.of("24 hours", 1440),
            Arguments.of("2.75 hours", 165),
            Arguments.of("2.5 hours", 150),
            Arguments.of("1.5 hours", 90),
            Arguments.of("1 hour", 60),
            Arguments.of("30 minutes", 30),
            Arguments.of("1 minute", 1)
        );
    }
    
    @ParameterizedTest
    @MethodSource("dataPoints")
    void friendlyExpirationTimeTest(String timeAsFriendlyString, int timeInMinutes) {
        assertEquals(timeAsFriendlyString, ConfirmEmailUtil.friendlyExpirationTime(timeInMinutes));
    }
    
    @Test
    void testGrandfatheredTime() {
        //System.out.println("Grandfathered account timestamp test");
        //System.out.println("Grandfathered Time (y2k): " + ConfirmEmailUtil.getGrandfatheredTime());
        assertEquals(Timestamp.valueOf("2000-01-01 00:00:00.0"), ConfirmEmailUtil.getGrandfatheredTime());
        //System.out.println();
    }
}
