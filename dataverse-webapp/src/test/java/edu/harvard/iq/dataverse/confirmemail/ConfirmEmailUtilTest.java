package edu.harvard.iq.dataverse.confirmemail;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Enclosed.class)
public class ConfirmEmailUtilTest {

    @RunWith(Parameterized.class)
    public static class ConfirmEmailUtilParamTest {

        public String timeAsFriendlyString;
        public int timeInMinutes;

        public ConfirmEmailUtilParamTest(String timeAsFriendlyString, int timeInSeconds) {
            this.timeAsFriendlyString = timeAsFriendlyString;
            this.timeInMinutes = timeInSeconds;
        }

        @Parameters
        public static Collection<Object[]> parameters() {
            return Arrays.asList(
                    new Object[][]{
                            {"48 hours", 2880},
                            {"24 hours", 1440},
                            {"2.75 hours", 165},
                            {"2.5 hours", 150},
                            {"1.5 hours", 90},
                            {"1 hour", 60},
                            {"30 minutes", 30},
                            {"1 minute", 1}
                    }
            );
        }

        @Test
        public void friendlyExpirationTimeTest() {
            assertEquals(timeAsFriendlyString, ConfirmEmailUtil.friendlyExpirationTime(timeInMinutes));
        }
    }

    public static class ConfirmEmailUtilNoParamTest {

        @Test
        public void testGrandfatheredTime() {
            System.out.println();
            System.out.println("Grandfathered account timestamp test");
            System.out.println("Grandfathered Time (y2k): " + ConfirmEmailUtil.getGrandfatheredTime());
            assertEquals(Timestamp.valueOf("2000-01-01 00:00:00.0"), ConfirmEmailUtil.getGrandfatheredTime());
            System.out.println();
        }
    }
}
