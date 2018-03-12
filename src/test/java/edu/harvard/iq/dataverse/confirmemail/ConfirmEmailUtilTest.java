package edu.harvard.iq.dataverse.confirmemail;

import java.sql.Timestamp;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ConfirmEmailUtilTest {

    @Test
    public void testFriendlyExpirationTime() {
        System.out.println("Friendly expiration timestamp / measurement test");
        System.out.println("1440 Minutes: " + ConfirmEmailUtil.friendlyExpirationTime(1440));
        assertEquals("24 hours", ConfirmEmailUtil.friendlyExpirationTime(1440));
        System.out.println("60 Minutes: " + ConfirmEmailUtil.friendlyExpirationTime(60));
        assertEquals("1 hour", ConfirmEmailUtil.friendlyExpirationTime(60));
        System.out.println("30 Minutes: " + ConfirmEmailUtil.friendlyExpirationTime(30));
        assertEquals("30 minutes", ConfirmEmailUtil.friendlyExpirationTime(30));
        System.out.println("90 Minutes: " + ConfirmEmailUtil.friendlyExpirationTime(90));
        assertEquals("1.5 hours", ConfirmEmailUtil.friendlyExpirationTime(90));
        System.out.println("2880 minutes: " + ConfirmEmailUtil.friendlyExpirationTime(2880));
        assertEquals("48 hours", ConfirmEmailUtil.friendlyExpirationTime(2880));
        System.out.println("150 minutes: " + ConfirmEmailUtil.friendlyExpirationTime(150));
        assertEquals("2.5 hours", ConfirmEmailUtil.friendlyExpirationTime(150));
        System.out.println("165 minutes: " + ConfirmEmailUtil.friendlyExpirationTime(165));
        assertEquals("2.75 hours", ConfirmEmailUtil.friendlyExpirationTime(165));
        System.out.println("1 Minute: " + ConfirmEmailUtil.friendlyExpirationTime(1));
        assertEquals("1 minute", ConfirmEmailUtil.friendlyExpirationTime(1));
        System.out.println();
    }

    @Test
    public void testGrandfatheredTime() {
        System.out.println();
        System.out.println("Grandfathered account timestamp test");
        System.out.println("Grandfathered Time (y2k): " + ConfirmEmailUtil.getGrandfatheredTime());
        assertEquals(Timestamp.valueOf("2000-01-01 00:00:00.0"), ConfirmEmailUtil.getGrandfatheredTime());
        System.out.println();
    }

}
