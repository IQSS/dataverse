package edu.harvard.iq.dataverse.confirmemail;

import java.sql.Timestamp;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ConfirmEmailUtilTest {

    @Test
    public void testFriendlyExpirationTime() {
        ConfirmEmailUtil confirmEmailUtil = new ConfirmEmailUtil();
        System.out.println("Friendly expiration timestamp / measurement test");
        System.out.println("1440 Minutes: " + confirmEmailUtil.friendlyExpirationTime(1440));
        assertEquals("24 hours", ConfirmEmailUtil.friendlyExpirationTime(1440));
        System.out.println("60 Minutes: " + confirmEmailUtil.friendlyExpirationTime(60));
        assertEquals("1 hour", ConfirmEmailUtil.friendlyExpirationTime(60));
        System.out.println("30 Minutes: " + confirmEmailUtil.friendlyExpirationTime(30));
        assertEquals("30 minutes", ConfirmEmailUtil.friendlyExpirationTime(30));
        System.out.println("90 Minutes: " + confirmEmailUtil.friendlyExpirationTime(90));
        assertEquals("1.5 hours", confirmEmailUtil.friendlyExpirationTime(90));
        System.out.println("2880 minutes: " + confirmEmailUtil.friendlyExpirationTime(2880));
        assertEquals("48 hours", confirmEmailUtil.friendlyExpirationTime(2880));
        System.out.println("150 minutes: " + confirmEmailUtil.friendlyExpirationTime(150));
        assertEquals("2.5 hours", confirmEmailUtil.friendlyExpirationTime(150));
        System.out.println("165 minutes: " + confirmEmailUtil.friendlyExpirationTime(165));
        assertEquals("2.75 hours", confirmEmailUtil.friendlyExpirationTime(165));
        System.out.println("1 Minute: " + confirmEmailUtil.friendlyExpirationTime(1));
        assertEquals("1 minute", confirmEmailUtil.friendlyExpirationTime(1));
        System.out.println();
    }

    @Test
    public void testGrandfatheredTime() {
        ConfirmEmailUtil confirmEmailUtil = new ConfirmEmailUtil();
        System.out.println();
        System.out.println("Grandfathered account timestamp test");
        System.out.println("Grandfathered Time (y2k): " + confirmEmailUtil.getGrandfatheredTime());
        assertEquals(Timestamp.valueOf("2000-01-01 00:00:00.0"), confirmEmailUtil.getGrandfatheredTime());
        System.out.println();
    }

}
